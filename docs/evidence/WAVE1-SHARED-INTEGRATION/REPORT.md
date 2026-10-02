# First-wave shared integration foundation

Status: **BOUNDED REPAIR COMPLETE; READY FOR INDEPENDENT RE-REVIEW**, not merged or independently accepted.

Date: 2026-10-01 (Africa/Lagos). Integration worktree: `C:/Users/USER/Desktop/omanii`.
Branch: `feat/wave1-shared-integration`. Approved base and unchanged HEAD:
`c1b3903ca9078651d74cefad9c22b67e5acd3f12`. All changes are uncommitted.

This report records implementation and acceptance evidence, not a competing specification.
Canonical semantics remain [protocol](../../../protocol/README.md),
[architecture](../../../ARCHITECTURE.md) and the relevant accepted ADRs.
Scope is the human-authorized shared foundation from this integration chat.

## Checkout correction and preservation

Both existing feature branches were verified at the approved base before switching.
TASK-002 initially held the integration branch with 50 untracked files. Its porcelain
status, file lengths and SHA-256 values for all 94 Git-listed tracked/untracked files
were recorded. The authorized switch back to `feat/task-002-capability-radio` preserved
all 94 files and all 50 status entries exactly. The clean main checkout was then switched
to the existing integration branch and verified clean before implementation.
See [checkout-verification.json](checkout-verification.json). No stash, reset, clean,
worker-file overwrite, commit, push or worker implementation repair was performed.
The main switch required approved sandbox access to Git metadata after an initial
permission-denied attempt; automatic approval did not reject either authorized switch.

## Shared files created and changed

Created under `android/app/src/main/java/com/omanii/app/`:

- `model/Availability.kt`: single `Availability` and `ValueState<T>`.
- `model/MonotonicClock.kt`: single `nowElapsedRealtimeNs()` contract.
- `model/ObservationTiming.kt`: source domain kind/instance token/event meaning, separate
  source and receive timestamps, optional defensible measurement time and derived age-known flag.
- `model/Identity.kt`: session-scoped `SegmentRef`, `NetworkEpochRef`, `CoordinateFrameRef`.
- `model/Geometry.kt`: finite metre coordinates and normalized Hamilton XYZW quaternion values.
- `model/DevelopmentMetadata.kt`: record/session/producer/app-build/version axes and explicit profile applicability.
- `time/AndroidElapsedRealtimeClock.kt`: the Android `SystemClock.elapsedRealtimeNanos()` adapter.
- `session/ProbeContextHooks.kt`: typed snapshots, boundaries, interval evidence, continuity enum and probe-ID-based hooks.

Created corresponding `AvailabilityTest`, `ObservationTimingTest`, `GeometryMetadataTest`
and `ProbeIntervalContextTest` unit suites. Changed only `android/app/build.gradle.kts`
and `android/app/src/main/AndroidManifest.xml` among pre-existing source files.
Evidence files are in this directory; [SOURCE-SHA256.json](SOURCE-SHA256.json) identifies
the exact 14 shared source/build/manifest/test files validated in this working tree.

Contracts changed: new authorized Kotlin bindings; bounded repair strengthens boundary
immutability and explicitly documents the approved spatial binding, retaining the same
constructor/property/copy/destructuring signatures and value semantics. **No canonical protocol, ADR, product,
score, production serialization or version-policy change**. No production version
numbers, experimental cadence/budget/tolerance parameters or schema registrations assigned.

The timing binding accepts known measurement time only for a documented direct
Android elapsed-realtime MEASUREMENT source, with identical source timestamp and no
future measurement time. ARCore capture and modem receipt remain separately represented.
It preserves source timestamps with unknown age rather than estimating clock alignment.
Callers remain responsible for documented API provenance and using the correct run-scoped domain token.

Constructor checks reject contradictory value availability, cross-session references,
regressing intervals, out-of-interval/unordered boundaries, known boundaries labeled
continuous, invalid geometry and fabricated negative/non-finite movement distance.
Matching endpoints never erase recorded intermediate boundaries. Continuous context is
explicit caller-provided evidence; these structures do not discover transitions,
prove path binding, declare stationarity or authorize comparison/recommendation.
Hooks have no implementation here: begin/end association, race-free boundary journaling,
terminal-path completion and cancellation remain consumer integration work.

## Authorized bounded repair after independent review

Repair pass 1 is complete and confined to the two foundation findings and the stale TASK-004
handoff. No worker adaptation, implementation repair or worktree change is included.

- `session/ProbeContextHooks.kt` snapshots boundary input into a new `ArrayList` and
  exposes a `Collections.unmodifiableList` view. Validation reads that owned snapshot.
  `ProbeIntervalContext` uses an ordinary final class with an explicit validating
  `copy` because a generated data-class copy cannot implement defensive snapshotting
  for the existing public list property. Equality, hashing, string representation
  and all five destructuring components are preserved; no new fields/dependencies.
- `ProbeIntervalContextTest.kt` adds six regressions: clearing the source A-to-B-to-A
  journal; inserting into a source journal after CONTINUOUS construction; replacement
  journals supplied through copy and reconstruction; mutation through properties and
  destructuring; invalid continuity/order/interval evidence in construction and copy;
  preservation of value/copy/destructuring behavior. Existing tests remain intact.
- `model/Geometry.kt` explicitly documents session-local metres, a right-handed +Y-up
  frame, XZ horizontal projection with retained height, and camera-local-to-session-local
  Hamilton XYZW orientation. No projection math or pose-provider logic is added.
- TASK-004 status below is corrected from its current worker report. The authorized
  third repair is already complete; no outstanding timing repair is assigned here.

The canonical protocol/ADRs, permissions, dependencies, worker code and feature scope
are unchanged by this repair. Independent re-review and worker physical gates remain open.

## Worker proposal arbitration and mappings

| Worker | Accepted | Rejected or deferred | Required consumer mapping |
| --- | --- | --- | --- |
| TASK-002 | Ordinary `ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE` declarations | Shadow clock/availability; location/phone-state permission expansion; debug wiring; task-owned repairs and unsupported physical claims | Replace shadow contracts with canonical imports. Preserve per-field reasons. Scope each network epoch to the injected session/run. Use source event meaning: CellInfo modem receipt is not exact signal measurement time. Keep freshness/independence separate from availability. |
| TASK-003 | Official ARCore 1.56.0 dependency; CAMERA declaration; all camera/AR features optional; optional-runtime metadata; elapsed-realtime backing | Its `time.MonotonicClock.nowNs()` declaration and wholesale MainActivity patch | Inject `AndroidElapsedRealtimeClock::nowElapsedRealtimeNs`. Keep pose/debug geometry task-local behind mapping. Map raw ARCore source timestamp to `ARCORE_FRAME_UNDEFINED`, a caller-supplied AR session/run domain-instance token and `FRAME_CAPTURE`; compatible measurement time stays null. Map physical-camera-to-local XYZW/metre values and caller-owned identities. Global network-only segmentation must not force a new pose frame. Tracking loss invalidates placement immediately, before recovery boundary emission. |
| TASK-004 | INTERNET declaration; interval and A-to-B-to-A preservation requirements; explicit continuity outcomes | `nowNanos()` clock; Any/token hooks; wholesale ProbeContracts shared registration; endpoint execution configuration | Consume typed `begin(probeId, startAtElapsedRealtimeNs)` and `end(probeId, endAtElapsedRealtimeNs)` hooks. Replace nullable strings with scoped references/value states. Retain reasons and before/after snapshots for every boundary. Movement absence has an availability reason. Keep host/port/address/TLS execution settings and probe/budget implementation-specific DTOs task-local; map only common primitives. Separate endpoint context, experimental profile/version and actual configuration in evidence. |

No worker patch was applied wholesale and no worker source was imported. MainActivity,
shared resources and scaffold test remain unchanged because feature debug components
are not yet present in this integration tree. This avoids premature feature activation
and compile-time coupling to unrepaired TASK-002 code. No permission requests or ARCore
support/install/session calls occur in the unchanged shell.

TASK-002's independent defects and unsupported physical evidence are supplied by Francis
in the authorization; its existing worker report is not treated as acceptance evidence.
TASK-003's `REVIEW.md` records PASS WITH FOLLOW-UP, 44 tests with validation-only SDK
injection and physical validation pending. TASK-004's current `REPORT.md` records
authorized repair pass 3 completed, 24/24 isolated JVM tests and 12/12 server tests
passing, with independent verdict PASS WITH FOLLOW-UP. Shared Android integration
and physical acceptance remain open; no additional timing repair is outstanding.
These are worker evidence states, not results of this foundation gate. The completed
worker repair limits are not reset by integration; TASK-004 has used its authorized 3/3 passes.

## Build, permission and dependency decisions

Source permissions now declare INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE and
CAMERA only. Camera permission remains intended for explicit spatial feature entry;
no runtime request flow is introduced here. Fine/coarse location, READ_PHONE_STATE and
nearby-device additions await corrected task-specific API/grant justification and
review. No background, inventory, identifier, storage, VPN or unrelated permission added.
Backup remains disabled. No cleartext exception, endpoint, cloud storage or telemetry configured.

ARCore change note:

- Purpose: optional certified pose runtime for the accepted TASK-003/ADR-001 direction;
  no rendering framework, geospatial/cloud/depth recording or raw-image processing added.
- Version: `com.google.ar:core:1.56.0`, checked against the
  [official release](https://github.com/google-ar/arcore-android-sdk/releases/tag/v1.56.0).
- Cached AAR: 356,212 bytes, SHA-256
  `20A66FB176C62234879A94202F1424ADF14F3CC1159AEDBD247D3517B65F1F50`.
  The SDK includes native libraries; separately installed Google Play Services for AR
  has its own size/lifecycle. Final APK-size attribution and runtime/device cost are not measured here.
- Maintenance: official SDK pin; Google runtime updates independently. Availability,
  installation and permissions must be capability-gated at feature entry, as documented
  in [optional AR setup](https://developers.google.com/ar/develop/java/enable-arcore).
- Manifest review: merged artifact contains scoped ARCore/Play package and install-service
  queries, a non-exported ARCore install activity and minimum-runtime metadata. Existing
  AndroidX signature receiver permission remains. DUMP restricts a receiver's callers;
  the app does not request DUMP. See [merged-debug-manifest.xml](merged-debug-manifest.xml).
- Privacy: runtime camera processing and support/install service behavior require truthful
  disclosure; no omanii imagery retention/upload or SDK usage is implemented here.
  Integrate the [ARCore privacy requirements](https://developers.google.com/ar/develop/privacy-requirements)
  before activating its feature route.
- License: SDK binary terms and third-party notices apply; the entire SDK must not be
  labeled Apache-2.0. See the [official license](https://github.com/google-ar/arcore-android-sdk/blob/main/LICENSE).
- Alternatives: platform sensors/camera alone do not provide validated room tracking;
  manual fallback remains available to future product integration. No larger renderer or
  unvalidated inertial substitute is added. TASK-004 uses existing platform sockets;
  no HTTP library, coroutines/Flow dependency, DI framework or additional runtime added.

Official timing references: [Android SystemClock](https://developer.android.com/reference/android/os/SystemClock),
[ARCore Frame](https://developers.google.com/ar/reference/java/com/google/ar/core/Frame),
[CellInfo](https://developer.android.com/reference/android/telephony/CellInfo).

## Commands and automated results

From `android/`, using existing tools and cached dependencies:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
$env:ANDROID_HOME = 'C:/Users/USER/AppData/Local/Android/Sdk'
$env:GRADLE_USER_HOME = 'C:/Users/USER/.gradle'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --no-daemon --console=plain
```

Post-repair logged invocation: **BUILD SUCCESSFUL**, exit 0, 1m29s;
51 actionable tasks, 16 executed and 35 up-to-date. Compile, unit tests and lint analysis
executed against the repaired source; assemble completed successfully.
Its [android-gates.log](android-gates.log) records normal shared-tree gates without worker
init scripts or substitute contracts. Output was captured with `Tee-Object` to that path,
and the command returned exit 0. The initial sandbox invocation stopped before Gradle
execution on an unwritable cache lock; approved escalation used the existing Gradle
cache/Android SDK for the successful gate. Actual JUnit XML artifacts were inspected;
their fresh timestamps and hashes are retained in [unit-results.json](unit-results.json).

| Gate | Result |
| --- | --- |
| Assemble debug | PASS; APK 11,507,624 bytes, SHA-256 `47E129BB08D14965E6EB24585D0E1470B1C44F87E148293F623276F740257801` |
| Unit tests | PASS: 32 tests, 0 failures/errors/skips; 31 foundation tests + 1 scaffold test, including all six new boundary regressions; suite timestamps and XML hashes in [unit-results.json](unit-results.json) |
| Lint | PASS: 0 errors, 3 scaffold warnings (OldTargetApi, DataExtractionRules, MissingApplicationIcon); no lint suppression or SDK target change; [lint-results-debug.txt](lint-results-debug.txt) |
| Source manifest/dependency diff | Only four permissions, optional camera/AR declarations/runtime metadata and one ARCore dependency |
| Merged manifest review | PASS for intended declarations and scoped SDK additions; no forbidden permission |
| Single-contract/sensitive-code scan | One clock contract and one availability enum; no alternate clock, endpoint address/configuration, logging, controller, task implementation or analytics code |
| Whitespace/hygiene | PASS: `git diff --check`; all 19 untracked deliverables checked for trailing whitespace/conflict markers and all untracked JSON parsed (generated Gradle log excluded); no source hygiene errors |
| Evidence refresh | All 14 source/build/manifest/test SHA-256 values refreshed; all five JUnit XML summaries/hashes refreshed; gate log replaced and lint/merged-manifest artifacts recopied from the successful gate |
| Scope preservation | Three repaired source/test files plus this report and refreshed log/hash/unit evidence only; shared build/manifest, remaining foundation files, canonical docs and all three worker worktrees unchanged by repair |

No server/probe, fixture parser, cancellation/network-byte or physical test was run:
this change implements no active scheduler/transfer/provider or serializer and touches
no fixtures. Boundary and truth-invariant unit regressions are included. Full worker
replay/cancellation/budget tests must run when corrected consumers join this foundation.

## Review handoff, limitations and unresolved questions

Initial independent review: **REPAIR REQUIRED** for boundary-list mutability and
the incomplete shared geometry convention. Francis authorized only those bounded
repairs, TASK-004 evidence cleanup and refreshed gates. Independent re-review is
pending; this foundation is not self-certified for merge.
No physical verification performed or claimed. No supported-device, radio freshness,
AR accuracy, cancellation/network-byte, battery/thermal or improvement claim made.

Review inputs: this report, shared source/build/manifest/test diff, hash manifest, unit
results, lint, merged manifest, original task packets and canonical contracts/ADRs.
No ScanSessionController, scores, field construction, candidate selection, TASK-005,
production serializer or endpoint execution configuration was implemented.

Remaining work: independent review of these Kotlin bindings and merged permissions;
TASK-002's correctness/evidence repairs and permission justification; TASK-003's model/
identity/domain-instance mapping and physical validation; TASK-004's typed-hook/DTO
adaptation; later race-free session association,
foreground cancellation/budget wiring, debug entry/disclosure and physical gates.
The primitive constructors do not replace those consumer correctness gates.

Privacy/security impact: explicit camera declaration and optional SDK/native/runtime
integration; ordinary network access/state declarations. No identifiers, credentials,
storage, retention changes, telemetry or network/camera activation added by this foundation.
Deviations: none in feature scope; richer TASK-002 permissions/debug routes are deliberately
deferred instead of applying worker proposals wholesale. Commit/PR reference: none,
as instructed. HEAD remains the approved base; source hashes identify the reviewed candidate.
