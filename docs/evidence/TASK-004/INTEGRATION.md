# TASK-004 proposed integration changes — unapplied

The integration owner owns all shared files. `shared-integration.patch` is an exact apply-checkable proposal against base `c1b3903ca9078651d74cefad9c22b67e5acd3f12`. It has **not** been applied. Canonical semantic contracts remain [protocol/README.md](../../../protocol/README.md); this note describes missing wiring, not a competing specification.

The patch proposes `model/ProbeContracts.kt`, `time/MonotonicClock.kt`, Android `ElapsedRealtimeClock.kt`, `session/ProbeContextHooks.kt`, and INTERNET permission. It requires integration-owner approval/reconciliation with concurrent Tasks 002/003. No duplicate models are defined in the probe package. Isolated tests extract those same proposed shared additions into ignored `.jvm-test` only, excluding the Android-specific clock adapter; they do not claim approved shared API compatibility.

The clock adapter uses `SystemClock.elapsedRealtimeNanos()` for the common injected receive clock. All timestamp/duration fields are Long nanoseconds. The probe's source timestamp is the explicit locally measured interval end; receive time is separately read when emitting the record. Source/callback times are not merged.

Session hooks must capture interval start and preserve every known boundary through end, including A→B→A transitions whose final snapshot matches the start. Provide fast thread-safe `begin(startNanos)` / `end(token,endNanos)` implementations from the session owner. `CONTINUOUS` requires evidence, `BROKEN` retains known boundary times, `UNKNOWN` or `NOT_EVALUATED` cannot support comparison. Movement distance stays null until the session can supply it. Comparison safety also requires SUCCESS and validated endpoint metadata; it does not certify peak-throughput or stationary spatial validity.

Exact construction after the shared patch is approved (the session chooses when to execute, off the main thread):

```kotlin
val budget = ConcurrentByteBudget(sessionBudgetId, selectedHttpPlaintextBudgetBytes)
val client = ProbeClient(ElapsedRealtimeClock, sessionProbeContextHooks)
val observations = client.run(approvedEndpoint, selectedExperimentalProfile, budget)
// Reuse this same budget for all clients/probes in the session.
// Foreground stop/cancel:
budget.cancel()
client.cancel()
// Dispose at session teardown:
client.close()
```

`EndpointContext` is supplied by integration: approved endpoint identity/version/region/procedure plus a pre-resolved address, host, port and TLS flag. Pre-resolution avoids unbounded DNS during a timed probe. Refresh endpoint selection/address outside active comparisons; changing endpoint/path invalidates comparability. TLS uses platform default trust and HTTPS hostname verification; arbitrary remote cleartext is rejected. No production endpoint or secret is embedded in the client. For a private physical experiment, an approved TLS host can terminate TLS in front of this loopback service; production exposure is not authorized by this task.

No Android HTTP dependency/build patch is required. Proposed INTERNET permission permits foreground network work; no sensitive radio/location/camera/background permission is introduced. The probe is not connected to MainActivity and does not create a session controller. Integration must ensure foreground lifecycle, thread ownership, user-visible data budget, endpoint capacity qualification, and cancellation of every active client. Cancelled budgets reject new reservations, but active I/O is closed promptly by `client.cancel()`.

Budget reservation refusal reports BUDGET_EXHAUSTED because the next bounded request cannot fit. Snapshot `exhausted` means all allowed bytes have actually been charged; small unusable remainder may still exist after refusal. Accounting is HTTP plaintext, including parser read-ahead and conservative failed writes, not total carrier usage. Physical evidence must measure overhead before a total-network-byte claim.

## Dependency review

- Node runtime: existing local Node 24, built-in HTTP/crypto/perf hooks only; no new runtime npm dependency or native addon.
- Development-only: TypeScript 5.9.3 (Apache-2.0), @types/node 24.10.1 (MIT), transitive undici-types (MIT). Compiler/declarations only, excluded from a runtime service install with `--omit=dev`; pinned lockfile. No permissions, analytics, data access or lifecycle install scripts. Maintenance risk: Node 24 support and dev type-version updates must be deliberately reviewed.
- Alternative: JavaScript plus runtime-only tests would avoid TypeScript declarations but lose the requested TypeScript/static contract. No HTTP server framework or Android HTTP library is needed.
- Android: platform Java sockets/TLS/SecureRandom, existing Kotlin and JUnit scaffold. No added Gradle dependency, manifest edit, storage, telemetry or stable identifier. Fresh UUID per request is debugging correlation only.

## Remaining gates

Apply/reconcile the shared patch and validate build/lint; independently approve APIs/permission; verify platform TLS and cancellation on a real Android device. Run the task's Wi-Fi and alternate/poor-path steps, download/upload byte comparison, cancellation, timeout and qualitative battery/thermal notes. Record real evidence in REPORT.md. The implementation cannot pass the physical acceptance gate without those observations.
