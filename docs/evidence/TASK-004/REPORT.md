# TASK-004 evidence

Status: **authorized repair pass 3 complete; task-owned automated gates PASS; independent PASS WITH FOLLOW-UP**. All original 19 JVM tests and five new regressions pass (24/24). Shared integration, full Android scaffold gates and mandatory physical acceptance remain open. Not DONE.

- Worktree: `C:\Users\USER\Desktop\omanii-task-004`
- Branch: `feat/task-004-measurement-probe`
- Approved base and current HEAD: `c1b3903ca9078651d74cefad9c22b67e5acd3f12`
- Initial status: clean. No commits, pushes, merges or branch changes authorized.
- Owned changes: measurement-server, Android probe implementation and matching tests, fixtures/probe, this evidence directory.
- Contracts changed: none in canonical/shared files. Shared Kotlin contracts/clock/context hooks are absent at base; exact proposed integration patch will be stored here, not applied.
- Repair passes: **3 / 3**, including one exceptional third pass explicitly authorized by the user. Final authorized autonomous pass. Scope: controllable preparation tests, unchanged ceilings/context acceptance, timeout/cancellation/no-late-opening regressions and complete gates/evidence. No fourth pass authorized.
- Physical status: **NOT RUN**. No device/network/battery/thermal claims.
- Review: **PASS WITH FOLLOW-UP** after independent pass 3 source/test re-review and final gate results. Historical pass 2 ESCALATE retained below. This does not certify shared integration or physical acceptance.

## Initial decisions

Use Node 24 native TypeScript execution and built-in HTTP/crypto APIs, with TypeScript and Node declarations for development only. No runtime npm dependencies, database or deployment. Bind localhost only. Version `0.1.0`, experimental HTTP procedure `http-probe-alpha-1`; no production protocol/profile promotion.

Client will use standard Java sockets with bounded HTTP/1.1 framing against only this controlled endpoint, TLS hostname verification for non-loopback endpoints, injected shared monotonic clock, caller-owned aggregate budget and start/end context hooks. No independent scan/session scheduler. Explicit socket close is used to interrupt blocked reads/writes. Address resolution is supplied by integration before probing; it is not hidden inside measured work.

## Sources consulted

- https://nodejs.org/api/http.html (request/body/time limits, abort/destroy, backpressure)
- https://nodejs.org/api/crypto.html (randomBytes incompressible payload)
- https://developer.android.com/reference/java/net/Socket (close interrupts blocked I/O)
- https://developer.android.com/reference/javax/net/ssl/SSLParameters (HTTPS endpoint identification)
- https://developer.android.com/reference/android/os/SystemClock (elapsedRealtimeNanos includes deep sleep)

## Acceptance tracking

| Acceptance | Implemented | Verification |
|---|---|---|
| Versioned controlled endpoint | Yes, loopback-only Node 24 | Health/echo tests pass |
| Bounded Android responsiveness/download/upload | Yes, standard sockets; shared wiring proposed | Isolated JVM compile and full 24-test suite PASS; shared scaffold integration pending |
| Endpoint/profile/interval/bytes/error evidence | Yes | Replay and client tests |
| Client/server finite bytes/time; aggregate concurrency | Yes | Server, aggregate budget and client tests PASS; ceilings unchanged |
| Cancellation/new-work/active-close bound | Yes | Local server/client <=500ms close and 300ms no-late-work gates PASS; no-socket preparation regressions PASS |
| Distinct timeout/error/cancel/invalid/partial/exhausted | Yes | Fixtures/client tests PASS |
| No HTTP-derived packet loss | No loss field/API | Fixtures reject packet loss; client test PASS |
| Interval and context boundary hooks | Yes; A-B-A preserved by session-supplied hook | Successful >2s A-B-A interval, retained boundaries and unsafe comparison PASS |
| Eight deterministic fixtures/replay | Yes, explicitly synthetic; Long timestamps encoded as decimal strings | 3 replay tests PASS |
| Scope/security baseline | Loopback-only, no proxy/DB/runtime npm dependency | Owned paths only; independent PASS WITH FOLLOW-UP |
| Real Wi-Fi/alternate path/bytes/cancel | Not run | Mandatory physical gate remains OPEN |

## First gate results

- `npm install --package-lock-only --ignore-scripts` and initial `npm ci`: sandbox npm registry access/cache writes failed. Retried with approved escalation and task-local cache: pinned install succeeded, 0 vulnerabilities reported. No dependency installation scripts executed.
- `npm run typecheck`: PASS.
- `npm test`: 8/10 PASS. Stream deadline and slow upload deadline correctly close the client in <100ms, but assert server active slot release before the asynchronous server close event. Will repair test observation to await close within the same 500ms bound, not loosen traffic/time limits.
- First canonical Android invocation: JAVA_HOME supplied; wrapper chose unwritable `C:\.gradle`.
- Retry with task-local GRADLE_USER_HOME: wrapper network fetch blocked. Cached Gradle 9.6.1 was then found; canonical offline gate retry uses that cache through approved escalation.
- Isolated JVM runner compiles actual probe sources/tests against three shared additions extracted from the unapplied patch. Uses cached Kotlin 2.3.21/JUnit 4.13.2 and JBR; no Gradle/shared build change or Android stub.

## Accounting and cancellation method

Budget counts conservative application HTTP plaintext bytes: outgoing request headers, received response headers and payload. It reserves payload + 8192 response header ceiling + 1024 outgoing header ceiling before opening/scheduling a request. Successful payload reads/writes count as actual body bytes; a failing upload write is conservatively charged in full and recorded separately as unconfirmed bytes. Server-confirmed upload count is retained separately. TLS/TCP/IP/DNS/retransmission/teardown bytes are excluded, so this is not a carrier-billing cap. Integration must visibly disclose this boundary and reserve an appropriate physical overhead allowance after evidence; no hidden overhead estimate is asserted here.

Cancel is terminal per ProbeClient, closes the owned socket and prevents all later samples/runs from opening a connection. Blocking I/O has an independent <=5s sample watchdog and <=30s profile limit. Automated cancellation target: client return and peer close <=500ms on local test host; application payload flat for 300ms after observed close. Kernel-queued bytes from before cancellation may arrive during teardown. Physical Android/TLS verification is still required. Budget cancellation stops checks/new reservations; session cancellation must also call each client's cancel to promptly close active work.

RTT is HTTP application completion time, with first sample COLD and later samples REUSED. COLD includes socket/TLS setup. No warm claim is attached to COLD; no ICMP ping or peak-link throughput claim. Every transfer remains a lower-bound gross-throughput building block, including successful bounded completion.

## Commands/results

Initial Git branch/HEAD/status verified; required task, canonical context, ADRs and development controls read. Android shared contracts and INTERNET permission absent. Java is installed in Android Studio JBR but is not on PATH; cached Kotlin compiler/JUnit are present. Exact verification results will be appended.

## Exact next actions

1. Pass 3 implementation, complete gates and independent re-review are complete. No further owned-source repair is requested or authorized. The user expressly rejected applying `next-repair-proposal.patch` unchanged; it remains unapplied, superseded historical evidence. No deadline was moved to exclude preparation.
2. Integration owner must approve/reconcile/apply `shared-integration.patch` (shared models, elapsedRealtimeNanos clock, boundary-preserving hooks, INTERNET) and choose approved endpoint/TLS/foreground session wiring. No endpoint or sensitive permission is silently added by this branch.
3. With shared wiring, rerun `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`; repository discovery in tests now works from Gradle module directories. Existing Node 24 is needed for loopback JVM integration tests.
4. Run real Android Wi-Fi and alternate/poor-path experiments, including bounded download/upload, observed app/server bytes with overhead boundaries, timeout, cancellation, and battery/thermal notes. Add sanitized evidence and independent acceptance review. Until then no physical/network-performance release claim.

## Initial Android gates and repair 1

- Canonical offline Gradle gate reached task graph but stopped: Android SDK location not found. No SDK/shared wiring edit was made. Assemble/unit/lint cannot pass on this host as configured.
- Isolated Kotlin compilation with `-Werror -jvm-target 17`: PASS. JUnit 16 tests: 13 PASS, 3 FAIL (short timeout consumed before parsing/body, A-B-A integration transfer timeout, oversized header timeout). Unbuffered per-character socket I/O was inefficient; repair 1 uses a 1024-byte bounded input buffer and charges actual socket reads including parser read-ahead. Header/payload caps remain enforced.
- Server tests now observe asynchronous server close within 500ms instead of assuming event ordering between peer close and server close. Application traffic and timeout bounds are unchanged.
- Independent review dispatched per AGENTS.md section 14. Review is read-only and does not certify physical behavior.

## Repair 2 scope

Independent verdict initially REPAIR REQUIRED: absolute pre-header server deadline missing (80ms drip request remained open >550ms), replay allowed contradictory successful upload confirmation, and Gradle tests required a repo property only the isolated runner set. Reviewer additionally reproduced a close/reservation race that could schedule onto the shut-down watchdog and throw instead of returning CANCELLED.

Final repair adds explicit per-connection incomplete-header deadline (rearmed for reuse), strict replay upload applicability/confirmation, portable parent-directory repo discovery, and synchronized cancellation/watchdog scheduling. HTTP ID generation is moved before interval start so random-generator initialization is not included in RTT. Comparison safety now also requires successful endpoint-validated evidence.

The A-B-A case is strengthened to a controlled >2s transfer with identical final refs and retained intermediate boundaries; existing real TypeScript download/upload tests remain. Added actual concurrent clients sharing one budget, close/reservation race regression, and a stalled upload sink cancellation/500ms peer-close/300ms flat accounting regression. No deadlines/caps or failed-outcome assertions are relaxed.

Repair 1 follow-up: all 10 server tests PASS; isolated 16 tests: 15 PASS, A-B-A Node cold-start timeout remained. Final interval test explicitly controls multi-second timing, avoiding startup variance while checking stronger interval evidence. Existing Node transfers still require success within 1s samples.

## Repair 2 gate results (historical, uncommitted working tree, 2026-10-01)

| Gate | Result |
|---|---|
| Node 24.19.0; npm 11.17.0 pinned install / `npm ci --ignore-scripts --cache ./node_modules/.npm-cache` | PASS after approved registry access; lockfile created; npm audit reported 0 vulnerabilities |
| `npm run typecheck` (TypeScript 5.9.3, strict/no-unused) | PASS |
| `npm test` | **12/12 PASS**; independent reviewer also ran and confirmed 12/12 |
| Production entry point smoke (`src/main.ts`, local port 18787; startup/health GET) | PASS: visible endpoint/service/procedure version, loopback bind, healthy caps; child stopped |
| `./docs/evidence/TASK-004/run-isolated-android-tests.ps1` | Kotlin 2.3.21 `-Werror -jvm-target 17` compile PASS; JUnit **18/19 PASS**, **1 FAIL** |
| Android canonical gates, JBR/cached Gradle 9.6.1/existing SDK selected in env, `--offline --no-daemon` | Reached compileDebugKotlin; FAIL because proposed shared model/time/session contracts deliberately absent. Assemble/test/lint remain unverified in shared scaffold. No shared build/manifest edit. |
| `git apply --check` shared-integration.patch / next-repair-proposal.patch | PASS; neither applied |
| `git diff --check`; explicit untracked source trailing-whitespace check | PASS |
| Forbidden/canonical path diff; permission/build dependency diff | No modifications |
| Sensitive data/log review | Only startup versions/local listen metadata logged; no request/IP/radio/location logging, credentials, stable user identifier or arbitrary proxy |

The SDK does exist at `C:\Users\USER\AppData\Local\Android\Sdk`; normal sandbox inspection denied access. Approved read access and gate retry used it via ANDROID_HOME. The earlier missing-SDK result was configuration, not proof of no installation.

### Historical failing Android case before authorized pass 3

`boundariesPreserveAToBToAEvenWhenFinalContextMatchesStart`: expected SUCCESS for a controlled >2s transfer, actual TIMEOUT **before connection**. `actualBytes=0`, `httpBytesCharged=0`, connection NOT_OPENED; interval elapsed 22.6ms, sample cap 3s, total profile cap 5s. The total profile clock starts before first UUID generation. Suspected cold initialization consumes the profile budget before sample start; no successful movement/network observation is fabricated. Read-only independent diagnosis requested. Exact proposed preparation patch is stored in `next-repair-proposal.patch`, **not applied**; it prepares the bounded correlation ID list before starting the network-profile clock and keeps socket/cancellation/byte caps unchanged. This proposal needs human authorization and verification. It does not claim the failed test passes.

Independent read-only diagnostic subsequently ran the unchanged compiled A-B-A test successfully in 2717ms, with instrumented profile-start→sample-start gap 151ms. That confirms preparation is charged against profile duration but omitted from the observation interval. It did **not** reproduce a >5s UUID-specific stall; UUID/class initialization and/or host scheduling/load remain inferred causes of the prior pre-connection timeout. Reviewer verdict: ESCALATE for unresolved full-suite timing evidence, exhausted repair count, shared integration and physical gates.

Final unchanged standalone full-suite rerun: Kotlin compile PASS, JUnit **18/19 PASS**, same A-B-A pre-connection TIMEOUT (`httpBytesCharged=0`, interval 11.8ms). This confirms the full-suite failure remains reproducible outside concurrent Gradle/server gates. No source edits were made after repair 2, no assertions/deadlines were relaxed, and no further repair was attempted. Stop/escalation now applies. The diagnostic's single-method pass is not acceptance evidence for the full suite.

Passed JVM cases include real client→TypeScript warm/reused echo and bounded download/upload, actual aggregate accounting, concurrent clients sharing one cap, reservation/charge safety, budget/cancel refusal, timeout with retained partial bytes, HTTP failure without loss, early EOF partial, overload/version invalidation, UNKNOWN/NOT_EVALUATED context safety, blocked-read cancellation <=500ms and no later samples, malformed/oversized response framing, validation gates, close/reservation race, and stalled-upload exchange cancellation/peer close <=500ms with flat budget accounting for 300ms. The upload test does **not** establish that every platform was blocked specifically in write(): kernel buffering varies. Android/TLS physical I/O behavior still requires observation.

Server gates show independent requested/body caps, incompressible bounded stream, sink actual byte confirmation, saturation/unhealthy marker, stream/upload duration caps, disconnect closure and flat queued payload for 300ms, no proxy/chunked-body behavior, absolute drip-header deadline. Replay checks all eight required synthetic cases, round-trip precision beyond JavaScript safe integer, invalid context/times/bytes/version/loss claims, and upload confirmation consistency.

## Repair 2 acceptance snapshot (superseded by pass 3 results below)

- Acceptance 1, 9, 10: automated local implementation evidence available.
- Acceptance 2–8: implemented and mostly covered; context multi-second SUCCESS gate remains failing, approved shared wiring/build is pending. Cancellation evidence is local JVM/Node application behavior only.
- Acceptance 11: **NOT RUN**. No physical device, Wi-Fi/cellular comparison, real carrier byte accounting, resource or real-network cancellation claim.
- Exact profile budgets/cadence/procedure versions are experimental and not promoted into production claims. No scores or packet loss produced. Endpoint health detects obvious software saturation; it cannot certify server/path capacity.
- Runtime dependencies none; development declarations/compiler installed footprint ~26.25MB (TypeScript 23.63MB, @types/node 2.52MB, undici-types 0.11MB), no native addon or lifecycle scripts. See INTEGRATION.md for purpose, privacy, licenses, maintenance and alternatives.
- Shared/canonical contracts changed: **none**. Proposed exact Kotlin/permission patch is pending integration-owner reconciliation/approval. HTTP framing/replay representation is task-local experimental implementation, not canonical protocol revision.
- Deployment: local loopback only. Public TLS, grants/abuse/capacity/operations are not deployed or approved.
- Privacy/security: fresh debug UUID, no stable user ID or sensitive payload; no database/storage/telemetry. No extra Android runtime library or sensitive permission. HTTP plaintext budget is not a carrier billing cap.
- Deviations: no functional scope expansion; full canonical Android gates cannot pass without shared contracts. Isolated contract-proposal tests supplement rather than replace those gates. Implementation remains uncommitted by explicit request.
- Commit/PR: **none created**. Base and HEAD unchanged at `c1b3903ca9078651d74cefad9c22b67e5acd3f12`.

## Final changed / untracked files

Tracked modified: `measurement-server/README.md`. All remaining deliverable files below are untracked; ignored compiler/Gradle/npm caches are not deliverables.

- `measurement-server/.gitignore`, `package.json`, `package-lock.json`, `tsconfig.json`
- `measurement-server/src/{main,server,replay}.ts`
- `measurement-server/test/{server.test,replay.test,android-endpoint}.ts`
- `android/app/src/main/java/com/omanii/app/probe/{ProbeClient,ConcurrentByteBudget}.kt`
- `android/app/src/test/java/com/omanii/app/probe/{ProbeClientTest,ConcurrentByteBudgetTest}.kt`
- `fixtures/probe/{README.md,observations.json}`
- `docs/evidence/TASK-004/{REPORT.md,INTEGRATION.md,.gitignore,shared-integration.patch,next-repair-proposal.patch,run-isolated-android-tests.ps1}`

No commit/push/merge/switch/delete-worktree operation performed. No forbidden source/shared/canonical path changed.

Additional official runtime reference for the header deadline repair: https://nodejs.org/docs/latest-v24.x/api/http.html#httpcreateserveroptions-requestlistener (default connectionsCheckingInterval 30000ms; explicit socket deadline avoids relying on that poll).

## Authorized exceptional repair pass 3

User/integration-owner authorization received in this chat: make preparation timing controllable; keep byte/sample/total ceilings and successful multi-second A-B-A/unsafe-comparison assertions; prove slow preparation TIMEOUT with no socket, cancellation and no socket opening after total-budget exhaustion; run complete isolated Android/server gates; update evidence. Explicitly forbidden: applying the old preparation-excluding patch unchanged, shared wiring edits, deadline inflation, weakened acceptance, physical claims or Git lifecycle changes.

Exact implementation changes are confined to `ProbeClient.kt` and its task-owned tests:

- Keep the public two-argument constructor and production UUID/SecureRandom behavior. Add an **internal** constructor seam for ID and upload-payload preparation functions. Tests use synthetic per-test IDs and deterministic Java Random payloads, independent of cold secure-entropy preparation. No shared interface/model/clock/session change.
- Keep profileStart **before** ID preparation and HTTP sample start **after** it. Preparation continues consuming total-profile time; no pre-generated ID list or shifted profile deadline.
- Derive watchdog delay from the original absolute sample/profile deadline after reservation. Check this same deadline before socket allocation/registration/connect and after upload payload preparation. Expired preparation cannot start network work or write late body data. Existing byte/time ceiling values are unchanged.
- Explicit SocketTimeoutException maps to TIMEOUT unless cancellation wins; cleanup releases reservations and closes an existing warm socket.
- Retain every original test/assertion, including >2s successful A-B-A transfer, identical start/end refs, intermediate boundaries, unsafe comparison, 80ms timeout/partial bytes, 500ms cancellation and 300ms no-late-work checks. Add five regressions: slow ID preparation exhausts total budget/no socket; cancellation during preparation/no socket; reservation delay exhausts total budget/no socket; later slow preparation closes warm socket/no second request/connection; slow upload preparation times out before any body byte.

New no-socket tests bind an actual loopback listener and assert accept times out after the run, not merely checking a result flag. Slow-preparation tests advance only the injected monotonic clock; they do not raise any deadline or claim physical timing evidence. Cancellation during preparation is checked once the blocked test preparation is released; this does not claim an arbitrary preparation function is interruptible. Active socket cancellation bounds remain covered by the original tests.

### Pass 3 final commands and results

| Command/gate | Result |
|---|---|
| `./docs/evidence/TASK-004/run-isolated-android-tests.ps1` | PASS: actual task-owned Kotlin sources/tests compiled with Kotlin 2.3.21 `-Werror -jvm-target 17`; JUnit **24/24 PASS**, 5.759s. All original **19/19** pass; five added regressions also pass. |
| `npm ci --offline --ignore-scripts --cache <worktree>/docs/evidence/TASK-004/.npm-cache` in measurement-server | PASS: pinned lockfile clean install, 3 packages installed, npm reported 0 vulnerabilities. Cache copied from prior task-local npm cache before install; no network request/escalation or install lifecycle scripts. |
| `npm run typecheck` in measurement-server | PASS |
| `npm test` in measurement-server | **12/12 PASS**, 2.351s |
| `git diff --check` and explicit untracked source whitespace scan | PASS |
| `git apply --check docs/evidence/TASK-004/shared-integration.patch` | PASS; patch remains unapplied and unchanged |
| Forbidden shared/canonical path diff | No changes |
| Independent pass 3 re-review | **PASS WITH FOLLOW-UP**; no actionable owned-source findings or further repair requested |

Verification suites were run sequentially to avoid concurrent host-load interference with the existing timing bounds. No failed test was removed, skipped or relaxed. The earlier 18/19 runs remain historical evidence; final success follows controllable preparation, not deadline exclusion or inflation.

Exact pass 3 edited files: `android/app/src/main/java/com/omanii/app/probe/ProbeClient.kt`, matching `ProbeClientTest.kt`, this REPORT.md, and evidence `.gitignore` (ignore relocated `.npm-cache/`). Measurement-server source/tests, fixtures, shared-integration.patch, canonical/shared/build/manifest/MainActivity/resource files were not edited in pass 3. The rejected `next-repair-proposal.patch` was not applied or rewritten.

Final independent verdict confirms that preparation still consumes the original profile deadline; unchanged byte/sample/total ceilings and successful >2s A-B-A/unsafe assertions remain intact; new regression coverage proves timeout/cancellation/no late connection or payload behavior. The full isolated suite is ready for integration-owner review, and independent re-review has already completed.

Physical evidence remains **NOT RUN**. Full Android assemble/test/lint still requires integration-owner approval/reconciliation of the unapplied shared contracts and permission/endpoint/foreground wiring; that known scaffold blocker is unchanged, so those gates were not redundantly rerun in pass 3. Local JVM/Node results do not certify Android/TLS/real network or resource behavior. No new privacy/security permission, dependency or logging is introduced; production preparation defaults remain UUID/SecureRandom.

Base/HEAD remain `c1b3903ca9078651d74cefad9c22b67e5acd3f12`, branch remains `feat/task-004-measurement-probe`; tracked/untracked deliverables remain in the earlier file list. Contracts changed: **none**. No commit, push, merge, branch switch or worktree deletion. Repair count **3 / 3 (final authorized pass)**; no additional repair pass is authorized.

## Checkpoint before approved Wave 1 adaptation (2026-10-02)

The user authorized checkpointing this reviewed implementation, then merging exactly shared-foundation commit `4be02dce58b2b070de6bff669309ead2bbe384e9`, followed by TASK-004-only adaptation left uncommitted. No additional timing repair or shared-contract change is authorized. No push/branch switch/worktree deletion.

Pre-checkpoint verification: worktree `C:\Users\USER\Desktop\omanii-task-004`; branch `feat/task-004-measurement-probe`; HEAD `c1b3903ca9078651d74cefad9c22b67e5acd3f12`; status contains only the reviewed TASK-004 deliverables listed above. `run-isolated-android-tests.ps1`: compile PASS, **24/24 JVM PASS**, 5.854s. Server `npm ci --offline --ignore-scripts --cache ../docs/evidence/TASK-004/.npm-cache`, `npm run typecheck`, `npm test`: PASS, **12/12**, 2.121s. The baseline ceilings/cancellation/context tests and all repair-pass-3 evidence are retained in this checkpoint. Its commit SHA will be recorded in the later adaptation evidence, since a commit cannot contain its own SHA.
