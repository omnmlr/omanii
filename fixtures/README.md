# omanii Fixture Plan

Fixtures are reusable, deterministic, synthetic or sanitized records used to test semantics, replay and regressions. They never contain real private user information.

## Rules

- Every fixture declares `schema_version`, `protocol_version`, `test_profile` and endpoint context.
- Expected outputs are specified independently of implementation where possible.
- Score golden cases include manually calculated expected values/tolerances, not a copy of production code.
- Missing/stale/unavailable states are represented explicitly.
- Replay fixtures may contain pose/network/probe event streams but must use synthetic/session-scoped identities.
- Large raw physical logs belong in bounded evidence storage or external research storage, not committed as routine fixtures unless deliberately reduced/sanitized.

## Initial catalog

### Score fixtures

- `score/reference-connection`
- `score/very-poor`
- `score/strong`
- `score/exceptional`
- `score/huge-throughput-bad-latency`
- `score/moderate-throughput-excellent-latency`
- `score/missing-required-metric`
- `score/lower-bound-throughput`
- `gaming/high-general-mediocre-gaming`
- `gaming/moderate-general-excellent-gaming` once an OG experimental model exists

### Observation semantics

- `network/missing-metric`
- `network/stale-radio`
- `network/unknown-age-radio`
- `network/cached-looking-repeated-value`
- `network/transport-change`
- `network/ap-band-transition`
- `network/vpn-context-change`
- `network/captive-or-unvalidated`

### Pose/session

- `pose/tracking-loss`
- `pose/discontinuous-resume`
- `pose/return-to-start-drift`
- `session/cancellation`
- `session/timeout`
- `session/partial-test`
- `session/byte-budget-exhausted`
- `session/network-change-during-probe`
- `session/moving-long-transfer`

### Verification/result

- `verification/tie`
- `verification/inconclusive`
- `verification/invalid`
- `verification/false-candidate-loses-on-retest`
- `verification/real-candidate-confirms`
- `verification/temporal-drift-dominates`
- `verification/reference-stable-spatial-effect`

### Probe/backend

- `probe/server-overload-invalidates`
- `probe/cancel-no-late-traffic`
- `probe/http-failure-not-packet-loss`
- `probe/endpoint-change-invalidates-comparison`

## Expected growth

Task 002 adds capability/network fixtures. Task 003 adds pose/session replay. Task 004 adds probe/cancellation/byte fixtures. Later score tasks add golden cases only after model versions are explicitly approved.
