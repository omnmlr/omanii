# TASK 004 - Controlled Measurement Endpoint + Probe Client + Replay Foundation

**Status:** READY after Bootstrap Task 001 approval; server/replay work may begin immediately, Android client integration uses the shared scaffold
**Type:** Implementation / measurement infrastructure

## Objective

Create the smallest bounded controlled measurement path needed for real experiments: a minimal versioned endpoint, an Android `ProbeClient`, hard byte/time accounting, safe cancellation, and deterministic replay fixtures.

Do not build a global speed-test platform.

## Product reason

omanii must measure actual connection behavior rather than infer it from radio strength. Those measurements are only useful if the tested endpoint/path is controlled, bounded and known. This task creates that experimental path without prematurely scaling infrastructure or claiming complete benchmark accuracy.

## Dependencies

- Bootstrap Task 001 approved and merged.
- `PRODUCT.md`, `ARCHITECTURE.md`, `protocol/README.md` accepted.
- ADR-002, ADR-004, ADR-006 accepted.
- Shared Android scaffold required only for the Android probe-client portion.

No dependency on Task 002/003 implementations for server-side completion. Integration must later consume their context/session outputs without changing their semantics.

## Owned paths

- `measurement-server/**`
- `android/app/src/main/java/**/probe/**`
- corresponding tests
- `fixtures/probe/**`
- `docs/evidence/TASK-004/**`

Protocol meaning is read-only. Any required semantic change must escalate.

## Required context

Read only:

- `PRODUCT.md` - truth rules, low-data principle, V1 exclusions.
- `ARCHITECTURE.md` - active measurement, backend, profile/budget, cancellation boundaries.
- `AGENTS.md`.
- `protocol/README.md` - `ProbeObservation`, `TestProfile`, `ByteBudget`, endpoint/version semantics.
- ADR-002, ADR-004, ADR-006.
- This task packet.

If choosing a runtime/library/cloud target whose current limits matter, verify current official documentation before deployment. The task may run locally/staging without prematurely committing production hosting.

## Interfaces/contracts used

- `ProbeClient`
- `ProbeObservation`
- `ByteBudget`
- `TestProfile`
- endpoint context/version
- monotonic time semantics
- cancellation/no-late-traffic rule

## Interfaces forbidden to change

This task may not redefine:

- score formulas;
- radio/pose semantics;
- scan candidate/verification semantics;
- privacy defaults;
- measurement profile claim semantics without approval;
- packet loss as an HTTP-derived metric.

## Implementation requirements

### 1. Minimal controlled endpoint

Create a minimal service with explicit service/protocol version and health metadata that can support:

- a tiny responsiveness/echo-style HTTP request path;
- a bounded incompressible download stream/payload;
- a bounded upload sink;
- server-side maximum bytes/time independent of client requests;
- request/test ID suitable for debugging without becoming a stable user identifier.

A small TypeScript service is the default implementation direction for Task 004 unless a clearly simpler maintained option is justified in the handoff. Do not introduce Kubernetes, microservices or a database.

### 2. Endpoint identity and health

Every probe result records:

- endpoint ID;
- endpoint/service version;
- coarse region/location label if relevant;
- protocol type/version;
- server response/health metadata required to detect obvious endpoint invalidity.

Server overload/saturation must be able to mark a test invalid or unusable rather than scoring the bottleneck as the user's connection.

### 3. Low-data responsiveness probe

Implement a small warm/reused-connection responsiveness profile or building block with:

- monotonic start/end timing on client;
- multiple bounded samples as configured by profile;
- success/timeout/error state;
- byte accounting;
- cancellation.

Document whether the timing is HTTP application RTT and do not label it ICMP ping.

### 4. Bounded transfer

Implement controlled download/upload sufficient for early gross-throughput experiments:

- payload is not trivially compressible/cached into meaningless speed;
- requested maximum is bounded;
- actual bytes and duration recorded;
- cancellation stops active application transfer promptly;
- profile can return a lower-bound/partial state when budget/duration is insufficient for peak throughput.

Task 004 does not need to certify gigabit peak speed.

### 5. ByteBudget

Client-side accounting must:

- know the selected total budget;
- refuse/limit probes that would exceed it;
- count actual transfer bytes as defensibly as the implementation permits;
- expose exhausted/cancelled state;
- aggregate across a session/profile rather than each probe pretending to have a fresh unlimited budget.

Server also enforces its own caps.

### 6. Cancellation/no late traffic

Cancellation is a release gate.

Tests must show:

- client stops scheduling new probes;
- active transfer is cancelled/closed;
- no new application payload continues after the defined cancellation grace/teardown observation window;
- any unavoidable socket/transport teardown bytes are not misrepresented as ongoing test traffic.

Define the precise automated test method in evidence rather than claiming literal zero packets at every network layer.

### 7. Timeout/error semantics

Differentiate:

- timeout;
- connection/request failure;
- cancellation;
- server-invalid/overloaded response;
- partial/budget-exhausted transfer.

Do not convert these into exact packet loss.

### 8. Movement/context association support

`ProbeObservation` includes start/end monotonic timestamps and a context/segment reference supplied by the session layer when integrated. It must be possible to attach movement span later.

The probe client does not decide that a multi-second transfer belongs to one final pose.

### 9. Replay foundation

Create deterministic fixtures and a small parser/harness that can replay recorded probe observations into pure analysis consumers later.

Initial fixtures include:

- success;
- timeout;
- cancellation;
- partial transfer;
- byte budget exhausted;
- endpoint overload/invalid;
- context transition during probe;
- HTTP failure explicitly not treated as packet loss.

### 10. Security/abuse baseline

For local/private alpha, keep the service minimal. Before any public deployment:

- TLS;
- explicit maximum request/body/stream sizes;
- rate/abuse controls or bounded grants;
- no arbitrary URL fetch/proxy behavior;
- no secrets committed to repo/client;
- logs minimized and no sensitive radio/location payload required.

Signed bounded test grants are an architectural requirement for public scale, not necessarily mandatory for the very first localhost/private endpoint if that would delay the physical experiment. The handoff must state the deployment exposure.

## Acceptance criteria

1. Measurement service starts locally/staging with a visible endpoint/service version.
2. Android probe client can perform bounded responsiveness and bounded transfer against it.
3. Probe observations record endpoint/profile/start/end/bytes/error state.
4. Client and server both enforce a finite byte/time ceiling.
5. Cancellation test shows no new application measurement work is scheduled and active transfer ends within the documented bound.
6. Timeout, cancellation, endpoint invalidity and partial/budget exhaustion are distinct states.
7. HTTP failure is never exposed as exact packet loss.
8. A multi-second probe is represented as an interval with start/end context hooks.
9. Deterministic replay fixtures/tests pass.
10. No database, microservice platform, global endpoint mesh, VPN/routing service or public arbitrary proxy is introduced.
11. Physical evidence includes at least Wi-Fi and one alternate/poor-network condition if available, observed bytes, timeout/cancel behavior and one bounded transfer.

## Verification commands

Measurement service target contract:

```bash
cd measurement-server
npm ci
npm run typecheck
npm test
```

Android client after shared scaffold:

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Repository hygiene:

```bash
git diff --check
```

If the implementation selects a different minimal server runtime with approval, equivalent exact commands replace the npm commands and the reason is documented before review.

## Automated tests

At minimum:

- server rejects/limits oversized requested transfer;
- stream stops at server cap;
- client refuses a probe that would exceed remaining `ByteBudget`;
- actual bytes accumulate across probes;
- timeout state;
- cancellation state;
- cancellation/no-new-scheduled-work;
- active transfer closes within documented bound;
- endpoint overload/invalid marker invalidates result;
- partial transfer/lower-bound state;
- warm responsiveness samples use explicit semantics;
- context transition during interval marks comparison unsafe/invalid hook;
- replay fixture round-trip;
- HTTP request failure does not populate packet-loss field.

## Physical-device evidence

On a real Android device:

- run low-data responsiveness on Wi-Fi;
- run one bounded download and upload;
- record app/server byte accounting and compare within explainable overhead/measurement boundaries;
- cancel during a transfer and inspect client/server logs for stop behavior;
- induce or simulate a timeout/poor network condition without falsifying the result;
- repeat on cellular or a second distinct path if available, with visible data-budget awareness;
- note battery/thermal observation qualitatively for this short task, without claiming the full product resource gate.

## Security/privacy considerations

- No arbitrary proxy/fetch target.
- No credentials in repo/logs/prompts.
- No stable user identifier needed.
- Server logs only operational request metadata necessary for experiment; minimize raw IP retention in any externally exposed environment.
- No analytics/ad SDK traffic during probe evidence.
- Public exposure requires TLS and abuse/budget controls before promotion.

## Out of scope

- global speed-test infrastructure;
- final throughput methodology/calibration;
- final score implementation;
- UDP packet-loss service unless separately approved after HTTP foundation;
- QUIC/Cronet work;
- VPN/routing optimization;
- billing/ads/accounts;
- public production operations platform;
- scanner field/candidate UI.

## Stop / escalation conditions

Stop and escalate if:

- selected hosting/runtime cannot enforce bounded transfer safely;
- cancellation semantics cannot prevent continuing application transfer within a defensible bound;
- a dependency requires broad/unrelated permissions or sensitive data;
- endpoint capacity/overload cannot be distinguished sufficiently for experiments;
- the only way to claim packet loss is HTTP failure inference;
- a protocol/schema change is required;
- public deployment would require unapproved credentials, billing or infrastructure commitment.

## Handoff

Report:

- files changed;
- server runtime/dependencies and justification;
- endpoint/protocol version;
- contracts changed: expected `none` unless separately approved;
- commands/results;
- replay fixtures;
- byte/cancellation evidence;
- physical verification;
- deployment exposure/security controls;
- known throughput/RTT limitations;
- privacy/security implications;
- unresolved questions;
- commit/PR reference.

## Recommended agent role

Measurement/backend implementer comfortable with Android client integration. A different reviewer should attack cancellation, byte accounting, endpoint overload, HTTP-versus-packet-loss semantics and abuse/security boundaries.
