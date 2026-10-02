import { createServer, type Server } from 'node:http';
import { randomBytes } from 'node:crypto';
import { monitorEventLoopDelay } from 'node:perf_hooks';
import type { Socket } from 'node:net';

export const VERSION = { endpointId: 'omanii-local-alpha', serviceVersion: '0.1.0',
  protocolVersion: 'http-probe-alpha-1', region: 'local-private' } as const;
export const LIMITS = { maxBytes: 1_048_576, maxDurationMs: 5_000, maxConcurrent: 2,
  maxConnections: 8, chunkBytes: 16_384, maxHeaderBytes: 8_192 } as const;
export interface ServiceOptions {
  maxBytes?: number; maxDurationMs?: number; maxConcurrent?: number;
  chunkDelayMs?: number; unhealthy?: () => boolean;
}
export interface ServiceStats { active: number; requests: number; downloadQueuedBytes: number;
  uploadReadBytes: number; closedTransfers: number; }

// Local experiment limits; canonical measurement meaning remains in protocol/README.md.
export function measurementService(options: ServiceOptions = {}): { server: Server; stats: ServiceStats } {
  const maxBytes = options.maxBytes ?? LIMITS.maxBytes;
  const maxDurationMs = options.maxDurationMs ?? LIMITS.maxDurationMs;
  const maxConcurrent = options.maxConcurrent ?? LIMITS.maxConcurrent;
  const chunkDelayMs = options.chunkDelayMs ?? 0;
  for (const [value, ceiling] of [[maxBytes, LIMITS.maxBytes], [maxDurationMs, LIMITS.maxDurationMs],
    [maxConcurrent, LIMITS.maxConcurrent], [chunkDelayMs, LIMITS.maxDurationMs]]) {
    if (!Number.isSafeInteger(value) || value! < 0 || value! > ceiling!) throw new Error('Invalid service cap');
  }
  if (maxBytes < 1 || maxDurationMs < 1 || maxConcurrent < 1) throw new Error('Caps must be positive');
  const stats: ServiceStats = { active: 0, requests: 0, downloadQueuedBytes: 0,
    uploadReadBytes: 0, closedTransfers: 0 };
  const lag = monitorEventLoopDelay({ resolution: 20 });
  lag.enable();
  const lagReset = setInterval(() => lag.reset(), 1_000).unref();
  const unhealthy = () => options.unhealthy?.() === true || lag.max > 100_000_000;
  const headerDeadlines = new Map<Socket, NodeJS.Timeout>();
  const armHeaderDeadline = (socket: Socket) => {
    const previous = headerDeadlines.get(socket);
    if (previous) clearTimeout(previous);
    if (!socket.destroyed) headerDeadlines.set(socket, setTimeout(() => socket.destroy(), maxDurationMs));
  };
  const server = createServer({ maxHeaderSize: LIMITS.maxHeaderBytes }, (req, res) => {
    const headerDeadline = headerDeadlines.get(req.socket);
    if (headerDeadline) clearTimeout(headerDeadline);
    headerDeadlines.delete(req.socket);
    res.once('finish', () => armHeaderDeadline(req.socket));
    stats.requests++;
    const id = req.headers['x-test-id'];
    const healthy = !unhealthy() && stats.active < maxConcurrent;
    const metadata = { 'X-Endpoint-Id': VERSION.endpointId, 'X-Service-Version': VERSION.serviceVersion,
      'X-Protocol-Version': VERSION.protocolVersion, 'X-Endpoint-Region': VERSION.region,
      'X-Endpoint-Health': healthy ? 'healthy' : 'invalid',
      'Cache-Control': 'no-store, no-transform', 'Content-Type': 'application/octet-stream',
      'X-Content-Type-Options': 'nosniff' };
    for (const [name, value] of Object.entries(metadata)) res.setHeader(name, value);
    res.on('error', () => req.destroy());
    req.on('error', () => res.destroy());
    const reject = (status: number) => {
      res.setHeader('Connection', 'close');
      res.writeHead(status, { 'Content-Length': 0 });
      res.end();
    };
    if (typeof id !== 'string' || !/^[a-zA-Z0-9-]{1,64}$/.test(id)) return reject(400);
    res.setHeader('X-Test-Id', id);
    if (!healthy) return reject(503);
    let url: URL;
    try { url = new URL(req.url ?? '', 'http://localhost'); } catch { return reject(400); }
    if (!['/v1/health', '/v1/echo', '/v1/download', '/v1/upload'].includes(url.pathname)) return reject(404);
    if (req.headers['transfer-encoding'] || req.headers['content-encoding']) return reject(400);
    const upload = url.pathname === '/v1/upload';
    if (req.method !== (upload ? 'POST' : 'GET')) return reject(405);
    const length = req.headers['content-length'] ?? '0';
    if (!/^\d+$/.test(length)) return reject(400);
    const bodyBytes = Number(length);
    if (!Number.isSafeInteger(bodyBytes) || bodyBytes > maxBytes) return reject(413);
    if (!upload && bodyBytes !== 0) return reject(400);
    const requested = upload ? bodyBytes : Number(url.searchParams.get('bytes') ?? '0');
    if ((upload || url.pathname === '/v1/download') &&
      (!Number.isSafeInteger(requested) || requested < 1 || requested > maxBytes)) return reject(413);
    const durationText = url.searchParams.get('durationMs');
    const requestedMs = durationText === null ? maxDurationMs : Number(durationText);
    if (!Number.isSafeInteger(requestedMs) || requestedMs < 1) return reject(400);
    const durationMs = Math.min(requestedMs, maxDurationMs);
    stats.active++;
    let finished = false;
    let scheduled: NodeJS.Timeout | undefined;
    const deadline = setTimeout(() => { res.destroy(); req.destroy(); }, durationMs);
    const finish = () => {
      if (finished) return;
      finished = true;
      stats.active--;
      stats.closedTransfers++;
      clearTimeout(deadline);
      if (scheduled) clearTimeout(scheduled);
    };
    res.once('close', finish);
    res.once('finish', finish);
    if (url.pathname === '/v1/health') {
      const body = Buffer.from(JSON.stringify({ ...VERSION, maxBytes, maxDurationMs, maxConcurrent,
        health: 'healthy', scope: 'single-controlled-endpoint', accounting: 'application-body-only' }));
      res.writeHead(200, { 'Content-Length': body.length, 'Content-Type': 'application/json' });
      res.end(body);
    } else if (url.pathname === '/v1/echo') {
      res.writeHead(200, { 'Content-Length': 1 });
      res.end(Buffer.from([0x6f]));
    } else if (upload) {
      let actual = 0;
      req.on('data', (chunk: Buffer) => {
        actual += chunk.length;
        stats.uploadReadBytes += chunk.length;
        if (actual > requested || actual > maxBytes || unhealthy()) {
          req.destroy(); res.destroy();
        }
      });
      req.on('end', () => {
        if (res.destroyed) return;
        res.setHeader('X-Actual-Upload-Bytes', actual);
        if (unhealthy() || actual !== requested) {
          res.setHeader('X-Endpoint-Health', 'invalid');
          res.writeHead(503, { 'Content-Length': 0 });
        } else res.writeHead(200, { 'Content-Length': 0 });
        res.end();
      });
    } else {
      res.writeHead(200, { 'Content-Length': requested });
      let sent = 0;
      const write = () => {
        if (finished || res.destroyed) return;
        if (unhealthy()) { res.destroy(); req.destroy(); return; }
        const body = randomBytes(Math.min(LIMITS.chunkBytes, requested - sent));
        sent += body.length;
        stats.downloadQueuedBytes += body.length;
        const writable = res.write(body);
        if (sent === requested) { res.end(); return; }
        const next = () => { if (!finished) scheduled = setTimeout(write, chunkDelayMs); };
        if (writable) next(); else res.once('drain', next);
      };
      write();
    }
  });
  server.maxConnections = LIMITS.maxConnections;
  // Node's headersTimeout polling alone is not an absolute deadline for a dripping peer.
  server.on('connection', socket => {
    armHeaderDeadline(socket);
    socket.once('close', () => {
      const deadline = headerDeadlines.get(socket);
      if (deadline) clearTimeout(deadline);
      headerDeadlines.delete(socket);
    });
  });
  server.requestTimeout = maxDurationMs;
  server.headersTimeout = maxDurationMs;
  server.keepAliveTimeout = 1_000;
  server.maxRequestsPerSocket = 64;
  server.setTimeout(maxDurationMs, socket => socket.destroy());
  server.on('checkContinue', (_req, res) => { res.writeHead(417, { 'Content-Length': 0, Connection: 'close' }); res.end(); });
  server.on('clientError', (_error, socket) => socket.destroy());
  server.once('close', () => { clearInterval(lagReset); lag.disable(); });
  return { server, stats };
}
