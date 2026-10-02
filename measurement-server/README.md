# Controlled local measurement endpoint

TASK-004 implements an experimental Node **24.x** TypeScript service using only built-in runtime APIs. Canonical meaning: [protocol](../protocol/README.md), [task](../docs/tasks/TASK-004-controlled-endpoint-probe-replay.md), and [evidence](../docs/evidence/TASK-004/REPORT.md).

```powershell
npm ci
npm run typecheck
npm test
npm start
```

The entry point binds **127.0.0.1:8787** only (`PORT` may select another local port). It prints endpoint/service/procedure versions once at startup. It does not log requests, remote addresses, radio information, location or user identifiers. This is not a public deployment; Android physical experiments require integration-owner endpoint/TLS wiring.

Send a fresh per-request `X-Test-Id` containing 1–64 ASCII letters/digits/hyphens. Responses echo it and include `X-Endpoint-Id`, `X-Service-Version`, `X-Protocol-Version`, `X-Endpoint-Region` and `X-Endpoint-Health`.

| Route | Body | Meaning |
|---|---|---|
| `GET /v1/health` | Small JSON | Versions, caps, coarse health, accounting scope |
| `GET /v1/echo` | One byte | HTTP application responsiveness, not ICMP ping |
| `GET /v1/download?bytes=N&durationMs=M` | Random bytes | Bounded uncached/uncompressed gross transfer |
| `POST /v1/upload?durationMs=M` | Fixed `Content-Length` | Bounded sink; `X-Actual-Upload-Bytes` confirms server body count |

Transfer cap: **1,048,576 bytes**, request duration: **5,000 ms**, active requests: **2**, connections: **8**, headers: **8,192 bytes**, max requests/socket: **64**. Incomplete headers have an explicit absolute deadline, including drip-fed headers on reused connections. Smaller test caps are supported in the factory; callers cannot raise ceilings. Uploads require fixed length: chunked/compressed request bodies are rejected. No arbitrary fetch/proxy. Downloads honor backpressure and stop timers after close.

Capacity health detects active-request saturation, explicitly supplied unhealthy state and >100ms event-loop lag. It cannot establish physical server/link capacity; physical capacity qualification remains required. An invalid/503 response is endpoint failure evidence. It is never packet-loss evidence. Abrupt deadline/health closure retains partial/error evidence rather than a successful peak-speed claim.

Server download counters mean application bytes queued to Node, not confirmed remote receipt. Upload counters mean HTTP body bytes received. Kernel/TLS/header overhead is outside body counts. Test-only instrumentation contains aggregate byte/count statistics without addresses or request IDs.

`src/replay.ts` validates/replays the task's eight explicitly synthetic fixtures. Nanoseconds are decimal strings so values beyond JavaScript's safe integer range survive round trips. Replay is for future pure analysis consumers and does not create production measurement data.

Replay serialization `probe-wave1-alpha-1` preserves canonical Wave 1 scoped snapshots, availability/reasons and complete before/after boundary evidence. Its parser rejects cross-session refs, hidden boundaries and unjustified comparison safety. This task-local mapping leaves HTTP procedure `http-probe-alpha-1` and shared contracts unchanged.

Development dependencies are pinned TypeScript and Node declarations (with `undici-types` transitive declarations); runtime dependencies: **none**. Public exposure requires separately approved TLS, capacity qualification, bounded grants/rate controls and operations. No hosting/provider/credentials are chosen here.
