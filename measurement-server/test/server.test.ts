import { test } from 'node:test';
import assert from 'node:assert/strict';
import { request, type IncomingMessage } from 'node:http';
import { once } from 'node:events';
import { setTimeout as delay } from 'node:timers/promises';
import { deflateSync } from 'node:zlib';
import { connect } from 'node:net';
import { measurementService, LIMITS, VERSION, type ServiceOptions } from '../src/server.ts';

async function start(options: ServiceOptions = {}) {
  const service = measurementService(options);
  service.server.listen(0, '127.0.0.1');
  await once(service.server, 'listening');
  const address = service.server.address();
  assert.ok(address && typeof address !== 'string');
  return { ...service, port: address.port,
    close: async () => { service.server.closeAllConnections(); service.server.close(); await once(service.server, 'close'); } };
}
async function get(port: number, path: string, method = 'GET', body?: Buffer) {
  return await new Promise<{ response: IncomingMessage; body: Buffer }>((resolve, reject) => {
    const req = request({ host: '127.0.0.1', port, path, method,
      headers: { 'X-Test-Id': 'synthetic-test', ...(body ? { 'Content-Length': body.length } : {}) } }, response => {
      const chunks: Buffer[] = [];
      response.on('data', (chunk: Buffer) => chunks.push(chunk));
      response.once('end', () => resolve({ response, body: Buffer.concat(chunks) }));
      response.once('error', reject);
    });
    req.once('error', reject); req.end(body);
  });
}
async function expectClosed(stats: { active: number }) {
  const before = performance.now();
  while (stats.active !== 0 && performance.now() - before < 500) await delay(5);
  assert.equal(stats.active, 0, 'server must release active work within 500ms of observed peer close');
}
test('versioned local health and one-byte echo; no compression or caching', async () => {
  const s = await start();
  try {
    const health = await get(s.port, '/v1/health');
    assert.equal(JSON.parse(health.body.toString()).serviceVersion, VERSION.serviceVersion);
    const echo = await get(s.port, '/v1/echo');
    assert.equal(echo.body.length, 1);
    assert.equal(echo.response.headers['x-endpoint-health'], 'healthy');
    assert.equal(echo.response.headers['cache-control'], 'no-store, no-transform');
    assert.equal(echo.response.headers['content-encoding'], undefined);
  } finally { await s.close(); }
});
test('oversized download/upload rejected; exact cap stream and incompressible bytes', async () => {
  const s = await start({ maxBytes: 32768 });
  try {
    assert.equal((await get(s.port, '/v1/download?bytes=32769')).response.statusCode, 413);
    assert.equal((await get(s.port, '/v1/upload', 'POST', Buffer.alloc(32769))).response.statusCode, 413);
    assert.equal((await get(s.port, '/v1/download?bytes=-1')).response.statusCode, 413);
    const download = await get(s.port, '/v1/download?bytes=32768');
    assert.equal(download.body.length, 32768);
    assert.equal(s.stats.downloadQueuedBytes, 32768);
    assert.ok(deflateSync(download.body).length >= 32768);
    const upload = await get(s.port, '/v1/upload', 'POST', Buffer.alloc(32768));
    assert.equal(upload.response.statusCode, 200);
    assert.equal(upload.response.headers['x-actual-upload-bytes'], '32768');
    assert.equal(s.stats.uploadReadBytes, 32768);
  } finally { await s.close(); }
});
test('overload marks endpoint invalid and rejects new measurement', async () => {
  const s = await start({ maxConcurrent: 1, chunkDelayMs: 100 });
  try {
    const req = request({ host: '127.0.0.1', port: s.port, path: '/v1/download?bytes=65536',
      headers: { 'X-Test-Id': 'active-test' } });
    req.on('error', () => {}); req.end();
    const [response] = await once(req, 'response') as [IncomingMessage];
    response.on('error', () => {}); response.resume();
    const second = await get(s.port, '/v1/echo');
    assert.equal(second.response.statusCode, 503);
    assert.equal(second.response.headers['x-endpoint-health'], 'invalid');
    req.destroy();
  } finally { await s.close(); }
});
test('explicit unhealthy marker rejects without interpreting HTTP failure as loss', async () => {
  const s = await start({ unhealthy: () => true });
  try {
    const r = await get(s.port, '/v1/echo');
    assert.equal(r.response.statusCode, 503);
    assert.equal(r.response.headers['x-endpoint-health'], 'invalid');
    assert.equal(r.response.headers['packet-loss'], undefined);
  } finally { await s.close(); }
});
test('absolute server time cap closes stream independently of client duration', async () => {
  const s = await start({ maxDurationMs: 60, chunkDelayMs: 100 });
  try {
    const before = performance.now();
    await assert.rejects(get(s.port, '/v1/download?bytes=1048576&durationMs=999999'));
    assert.ok(performance.now() - before < 1000);
    await expectClosed(s.stats);
    assert.ok(s.stats.downloadQueuedBytes < LIMITS.maxBytes);
  } finally { await s.close(); }
});
test('cancel disconnect: close within 500ms and queued application payload flat for 300ms', async () => {
  const s = await start({ chunkDelayMs: 30 });
  try {
    const req = request({ host: '127.0.0.1', port: s.port, path: '/v1/download?bytes=1048576',
      headers: { 'X-Test-Id': 'cancel-test' } });
    req.on('error', () => {}); req.end();
    const [response] = await once(req, 'response') as [IncomingMessage];
    response.on('error', () => {});
    await once(response, 'data');
    const cancelledAt = performance.now();
    req.destroy();
    while (s.stats.active !== 0 && performance.now() - cancelledAt < 500) await delay(5);
    assert.equal(s.stats.active, 0);
    const bytesAtClose = s.stats.downloadQueuedBytes;
    await delay(300);
    assert.equal(s.stats.downloadQueuedBytes, bytesAtClose);
    assert.equal(s.stats.requests, 1);
  } finally { await s.close(); }
});
test('slow upload body also ends by absolute server deadline', async () => {
  const s = await start({ maxDurationMs: 80 });
  try {
    const req = request({ host: '127.0.0.1', port: s.port, method: 'POST', path: '/v1/upload',
      headers: { 'X-Test-Id': 'slow-upload', 'Content-Length': 1048576 } });
    const closed = new Promise<void>(resolve => req.on('close', resolve));
    req.on('error', () => {}); req.write(Buffer.alloc(10));
    await Promise.race([closed, delay(1000).then(() => { throw new Error('upload did not close'); })]);
    assert.equal(s.stats.uploadReadBytes, 10);
    await expectClosed(s.stats);
  } finally { await s.close(); }
});
test('no arbitrary proxy, chunked body or invalid test identity accepted', async () => {
  const s = await start();
  try {
    assert.equal((await get(s.port, '/proxy?url=https://example.com')).response.statusCode, 404);
    const status = await new Promise<number | undefined>(resolve => {
      const req = request({ host: '127.0.0.1', port: s.port, path: '/v1/upload', method: 'POST',
        headers: { 'X-Test-Id': 'test', 'Transfer-Encoding': 'chunked' } }, res => { res.resume(); resolve(res.statusCode); });
      req.end('a');
    });
    assert.equal(status, 400);
  } finally { await s.close(); }
});
test('incomplete drip-fed headers cannot evade absolute server deadline', async () => {
  const s = await start({ maxDurationMs: 80 });
  const socket = connect(s.port, '127.0.0.1');
  socket.on('error', () => {});
  let drip: NodeJS.Timeout | undefined;
  try {
    await once(socket, 'connect');
    const before = performance.now();
    const closed = new Promise<void>(resolve => socket.once('close', () => resolve()));
    socket.write('GET /v1/echo HTTP/1.1\r\nX-Drip: ');
    drip = setInterval(() => socket.write('a'), 20);
    await Promise.race([closed, delay(500).then(() => { throw new Error('headers evaded absolute cap'); })]);
    assert.ok(performance.now() - before < 500);
    assert.equal(s.stats.requests, 0);
  } finally { if (drip) clearInterval(drip); socket.destroy(); await s.close(); }
});
