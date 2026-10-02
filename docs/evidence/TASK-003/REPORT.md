# TASK-003 implementation evidence

## Authorized exceptional scoped repair — 2026-10-02

Status: scoped repair complete; independent re-review **PASS WITH FOLLOW-UP** for code. TASK-003 physical acceptance remains pending. User explicitly authorized one exceptional additional pass for the two independent adaptation-review defects only. Cumulative repair count: **3 total (2 autonomous + 1 explicitly authorized exception completed)**; no further repair authorized. Branch/HEAD remain `feat/task-003-ar-pose` / `5a4db9567a1314e854600ec7511939e4ff7ec0c2`; adaptation is uncommitted, nothing staged/pushed, and no adaptation commit is authorized.

The proposed patch was inspected against current sources, not blindly applied. Four focused regressions were added for collision, recovery ordering, queued cancellation and nested/FIFO callbacks. The targeted pre-fix run compiled and **failed all four tests as expected** in 20s, reproducing the review findings; exact log/XML and source hashes are in `adaptation/repair-3/regressions-before-fix.log`, `regressions-before-fix/` and `REGRESSION-RED-SHA256.txt`.

Repairs implemented only in PoseCanonicalMapping/PoseProvider and their tests:

- PoseCanonicalMapping: encode session/run as `<session length>:<session>:<run length>:<run>` for the ARCore clock-domain instance and development record-ID scope. This keeps distinct allowed token tuples distinct, including underscore boundaries; sequence suffixes still distinguish records within a run. Raw source time and meaning are unchanged.
- PoseProvider: validate session/token on segment requests, enqueue requests made reentrantly during update, and drain them FIFO after the accepted event batch. Requests made by a boundary callback join the same FIFO. Cancellation closes the acceptance gate, suppresses further samples and clears pending requests; accepted control records stay ordered before STOP. Existing source reset/frame/cadence logic is unchanged.
- Tests: add four focused regressions below. Only the two exact token-format expectations in existing enriched-export/tamper tests changed; no assertions/tests were removed or suppressed. Compared with the prepared proposal, collision coverage includes three ambiguous token pairs with actual mapped frame timestamps, and the fourth regression covers nested/FIFO callbacks. The proposal was a reviewed reference, not blindly applied.

New regressions:

1. `PoseCanonicalMappingTest.distinctSessionRunPairsCannotAliasClockInstancesOrRecordIds`: three valid tuple pairs that previously aliased; equal raw frame timestamps produce distinct source domains and record IDs, with FRAME_CAPTURE and null measurement time preserved.
2. `PoseProviderTest.reentrantSegmentChangeDuringRecoveryPreservesCanonicalAndReplayOrdering`: TRACKING→PAUSED→TRACKING, request segment from recovered TRACKING callback; original boundary/sample precede the network-only boundary, with valid canonical mapping and raw/enriched replay.
3. `PoseProviderTest.queuedReentrantSegmentChangeCannotSurviveCancellation`: queue from recovery callback then cancel; no queued segment boundary or late sample survives, while the accepted physical boundary precedes STOP.
4. `PoseProviderTest.reentrantSegmentRequestsStayFifoWhenBoundaryCallbackAddsAnother`: two requests from TRACKING plus another from the first boundary callback; FIFO boundaries, no origin reset, unchanged frame and valid canonical/replay stream.

Full normal gate: `cd android; .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`, using installed Android Studio JBR/SDK and no dependency injection. **BUILD SUCCESSFUL in 56s**, 51 tasks (16 executed, 35 up-to-date). **96 tests / 64 task-owned**, zero failures/errors/skips. Task suites: mapping 14, capture 19, provider 16, capability 7, replay 8. Exact log/XML/summary: `adaptation/repair-3/android-gates.log`, `unit-results/`, `test-summary.txt`.

Lint: **0 errors, same 6 pre-existing warnings**, `adaptation/repair-3/lint.txt`. The compiler retains the same 12 redundant non-null assertions in older mapping tests. No unrelated warning cleanup, suppression or acceptance weakening. Existing geometry, tracking-state, cancellation and all three synthetic-fixture replay tests remain passing.

Hygiene: `git diff --check` and raw untracked whitespace checks pass. Hash comparison against the pre-repair gate snapshot confirms **exactly the four authorized source/test files changed**; all other pose/spatial/fixtures are unchanged. No changes to shared contracts, build, manifest, navigation or other packages. Sensitive-data/API scan remains clear for imagery/depth acquisition, recording, pose logs, display-oriented pose and competing clock calls. Evidence: `adaptation/repair-3/scope-and-hygiene.txt`, `SOURCE-SHA256.txt`, `FILES.txt`. Source/fixture hashes match the final gate snapshot and were verified by the independent reviewer. No new Android/ARCore API behavior was introduced; official references below remain applicable.

Independent re-review: **PASS WITH FOLLOW-UP**, both prior findings resolved, no other blocking findings. Full verdict and retained limits: `adaptation/repair-3/REVIEW.md`. No shared permission/dependency/privacy change or global controller was introduced. Only session/run-scoped IDs, local pose and existing explicit debug provenance are retained/exported.

Build artifact (not installed/run): 10,086,915 bytes, SHA256 `69D86405DB0995017743DF0EF8D07A54CA60F30BD6C6B4F054E78390AB6DD6B6`; `APK.json` and `merged-manifest.xml` retained under `repair-3/`. No device/instrumented test was performed and no physical claim is made.

Shared contracts, ARCORE_FRAME_UNDEFINED/FRAME_CAPTURE with null measurement time, coordinate-frame behavior and immediate tracking-loss invalidation must remain unchanged. Physical status remains **not performed**.

Those invariants are preserved and independently reviewed. Network-only segment changes retain the same coordinate frame/anchor; physical discontinuities retain fresh caller-owned frame/segment identities. Synthetic fixtures remain explicitly synthetic. The exception repairs only the two authorized findings; no TASK-005, field/scoring/network logic or ScanSessionController was added.

Current acceptance: the code/build/timing/ordering/export requirements have passing automated evidence and independent review. Permission/fallback, live pose, lifecycle/camera release, actual debug route and closure/degradation still need the physical/integration evidence described in PHYSICAL-TESTS.md. Code is ready for integration-owner review; TASK-003 is not marked fully accepted/DONE.

Exact next actions: integration owner provides approved debug navigation; confirm a supported ARCore phone/runtime and run the full physical checklist (three independent paths/closure, degraded tracking, interruption/cancellation/camera release, denied/unsupported fallback, rotation, sanitized export/replay); review the actual evidence with Francis. Keep the repaired adaptation uncommitted until separately authorized. No repair approval question remains pending.

## Historical pre-repair shared-foundation adaptation — 2026-10-02

Current status: checkpoint/merge complete; authorized task-owned adaptation implemented; normal foundation gates pass. Independent adaptation review: **ESCALATE**, with two scoped defects requiring authorization for another repair pass. Post-merge adaptation remains uncommitted. The prior checkpoint verdict below does not apply to the adaptation.

- Verified worktree `C:\Users\USER\Desktop\omanii-task-003`, branch `feat/task-003-ar-pose`, original HEAD `c1b3903ca9078651d74cefad9c22b67e5acd3f12`. Only the 36 reviewed task-owned files were untracked.
- Checkpoint **`3f5436b625185298f5945a21d0e65bc3a135f964`** contains those files unchanged, before any content edits; clean status after commit.
- Approved foundation **`4be02dce58b2b070de6bff669309ead2bbe384e9`** merged cleanly by ort, no conflicts. Merge HEAD **`5a4db9567a1314e854600ec7511939e4ff7ec0c2`**; parents are checkpoint + exact approved commit. Clean status before adaptation.
- User explicitly authorized checkpoint, merge and contract adaptation; no push or post-merge adaptation commit authorized/performed.
- Shared contracts changed by this adaptation: **none**. Only task-owned pose/spatial/tests/fixtures/evidence may differ from merge HEAD.
- Prior cumulative repair-pass count remains **2/2**; merging does not reset it. This is explicitly authorized foundation adaptation, not an extra autonomous review/repair loop.

Implemented adaptations:

- PoseCapture/PoseProvider consume canonical `com.omanii.app.model.MonotonicClock`; PoseDebugPanel defaults to `AndroidElapsedRealtimeClock` and accepts an explicit build label. No second clock implementation exists.
- Task-owned mapping copies metre position and normalized Hamilton XYZW orientation into shared geometry. Right-handed +Y-up, XZ horizontal projection and the reviewed physical-camera transform remain intact.
- `ObservationTiming` preserves raw frame nanoseconds as `SourceTimestamp(ARCORE_FRAME_UNDEFINED, FRAME_CAPTURE)` with a session/run domain token. Compatible measurement time remains null and age unknown. The token encoding requires the repair described below.
- Frame/segment identities map to separate session-scoped `CoordinateFrameRef`/`SegmentRef`. Caller-driven segment-only changes preserve anchor, coordinate frame, sample cadence and source deduplication; reentrant delivery ordering requires repair.
- Every tracking control event maps synchronously before UI posting. Loss immediately clears spatial frame/pose association; recovery needs an actual sample to restore it.
- `DevelopmentRecordMetadata` carries record/session/producer/build/schema/protocol fields and passive-profile NOT_APPLICABLE. Original task-local debug profile and LIVE_ARCORE/SYNTHETIC_FIXTURE provenance remain explicit. Enriched JSONL round-trips canonical metadata while legacy raw fixtures remain readable.

No pose implementation moved into shared packages. No ScanSessionController, TASK-005, field, score or network/probe logic was added.

Current acceptance against the canonical task:

| Criteria | Current evidence / remaining gate |
| --- | --- |
| 1. Build with shared ARCore scaffold | Normal foundation build passes; SDK/optional CAMERA manifest come from the approved merge. Debug navigation remains integration-owned and unwired. |
| 2–3. Progressive permission and explicit fallback | Reviewed gate/debug components and fake tests preserved; real permission/unsupported behavior pending. |
| 4. Monotonic local pose/orientation | Shared-clock/geometry/timing tests pass; domain-token collision must be repaired; supported-phone tracking pending. |
| 5–6. Tracking loss and discontinuities | Immediate association invalidation and frame continuity tests pass; reentrant segment ordering must be repaired. |
| 7. Cancellation/background cleanup | Existing startup/in-flight/reentrant stop tests preserved and passing; deferred segment cancellation regression is proposed; physical camera release pending. |
| 8–9. Export/replay and evidence-only debug | Legacy/new fixture replay and canonical export tests pass; no final scanner geometry; approved debug host wiring and physical inspection pending. |
| 10. Physical closure/degradation | Not performed; confirmed supported phone required. |

Normal command (Android Studio JBR and installed Android SDK; no init script/dependency injection): `cd android; .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`. Result: **BUILD SUCCESSFUL in 5m 33s**, 51 tasks, **92 tests / 60 TASK-003 tests**, zero failures/errors/skips. Task suites: mapping 13, capture 19, provider 13, capability 7, replay 8. Logs, JUnit XML and summary: `adaptation/android-gates.log`, `adaptation/unit-results/`, `adaptation/test-summary.txt`.

Lint: **0 errors, 6 pre-existing warnings**, retained in `adaptation/lint.txt`: shared target/backup/icon warnings, programmatic debug View constructor and two toUri suggestions. Kotlin also reports 12 redundant non-null assertions in the new mapping tests. No suppression, test removal or acceptance weakening. The two review defects demonstrate gaps in regression coverage despite successful gates.

`git diff --check` passes. Checkpoint-to-merge diff is empty for TASK-003-owned paths. Approved foundation ancestry and exact merge parents verified. Build/manifest/MainActivity/model/time/session and all other non-owned paths have no adaptation diff. Current source/fixture hashes and changed/untracked inventory are in `adaptation/SOURCE-SHA256.txt` and `adaptation/FILES.txt`; these identify the uncommitted gate snapshot, as required by the user's no-adaptation-commit instruction. No imagery/depth acquisition, recording, pose logs, display-oriented pose or competing receive-clock calls found in task source scan.

Built APK (not installed): 10,086,417 bytes, SHA256 `AD2AB63F940D486373F9800B96991D5CA636D4079EFE30CDFA148895275C818B`. `adaptation/merged-manifest.xml` includes the approved foundation's CAMERA/optional AR declarations. This establishes build integration only, not a runnable navigation route or physical tracking evidence.

Direct contract references: approved commit's `model/MonotonicClock.kt`, `time/AndroidElapsedRealtimeClock.kt`, shared geometry/identity/observation-timing/development-metadata/value-state types, and Wave 1 integration arbitration. Existing official Android/ARCore references below still apply; adaptation adds no new uncertain platform API behavior.

Independent review details: `adaptation/REVIEW.md`. Findings: P1 reentrant changeSegment can interleave identities inside an accepted update batch; P2 underscore concatenation aliases distinct session/run pairs' source domains and record IDs. An exact scoped proposal, including three added regressions and the changed token expectation, is in `adaptation/proposed-repair.patch`; `git apply --check` passes, **not applied and not compiled/tested**. Justification and approval boundary: `adaptation/PROPOSED-REPAIR.md`. No third repair pass has been performed; cumulative count stays **2/2**.

Old `proposed-shared-integration.patch` and SDK init script below are checkpoint history. Do not apply them to this foundation: the proposed shadow clock and wholesale navigation patch were rejected/deferred by the integration owner. MainActivity/debug navigation remains intentionally unwired.

Physical status: unchanged, no phone evidence. Independent review completed with ESCALATE. **Not ready for integration approval** until the two defects are repaired under explicit authorization and gates/re-review pass. No permission/privacy expansion occurred in task code; source-clock tokens and development IDs are session/run scoped, with no stable device identifiers. No unresolved shared-contract change is proposed.

Exact next actions:

1. Obtain explicit authorization for one additional scoped repair pass using `adaptation/proposed-repair.patch`; the repository's two autonomous passes are already used.
2. If authorized, apply that task-owned patch, run the normal assemble/unit/lint gate with all existing tests plus its three regressions, then obtain independent re-review. Keep adaptation uncommitted and do not push.
3. Integration owner supplies approved debug navigation using `PoseDebugPanel(activity, appBuild, clock)`; review remains limited to pose/debug capture, with no global scan controller.
4. Confirm an ARCore-supported phone/runtime and execute `PHYSICAL-TESTS.md` for three independent paths/closure, degradation, lifecycle/cancellation/camera release, denial/fallback, rotation and sanitized export/replay. Retain actual observations and do not claim physical acceptance from automation.

## Reviewed checkpoint evidence — 2026-10-01

Status: task-owned implementation and automated review complete. **PASS WITH FOLLOW-UP** for code; integrated build/physical acceptance pending. TASK-003 is not fully accepted/DONE.

- Worktree: `C:\Users\USER\Desktop\omanii-task-003`
- Branch: `feat/task-003-ar-pose`
- Approved base / HEAD: `c1b3903ca9078651d74cefad9c22b67e5acd3f12`
- Initial Git status: clean. No commit, push, merge, branch switch or worktree deletion authorized.
- Contracts changed: none. Canonical owners remain PRODUCT, ARCHITECTURE, protocol and ADRs.
- Cumulative autonomous repair passes: **2 / 2** (final pass: startup cancellation race and omitted dependency in proposal). No further autonomous repair authorized.

## Scope and state

Only task-owned pose/spatial code, corresponding tests, pose fixtures and this evidence directory changed (all currently untracked). The base contains no shared model/time/session interfaces and no ARCore dependency or camera manifest declarations. Task-local adapter values require integration mapping; they do not define a global scan controller.

| Acceptance | State |
| --- | --- |
| 1. Integrated app build | Pending integration-owner dependency/manifest/wiring patch; shared files frozen |
| 2. Progressive camera permission | Implemented gate/debug entry; fake tests pass; real permission flow pending |
| 3. Unsupported/denied fallback | Explicit states, fake tests pass; real-device denied path pending |
| 4. Monotonic local pose/orientation | Anchored same-frame transform and Hamilton quaternion implemented; fake golden tests pass; physical pending |
| 5. Tracking loss invalidates placement | Explicit events, no paused coordinates; fake tests pass |
| 6. Explicit segment/frame discontinuity | Caller assigns fresh IDs; re-origin after loss; replay tests pass |
| 7. Cancellation/background cleanup and reason | Repaired and reviewed; startup/in-flight/reentrant cancellation and owner-dispatched failure cleanup fake tests pass; physical camera release pending |
| 8. Sanitized export/replay | Strict debug JSONL dialect and two labeled synthetic fixtures; tests pass |
| 9. Evidence-only debug route | Observed x/z keyframe dots/guides, separate frame panels; no geometry/field; shared wiring proposed only |
| 10. Physical closure/degraded tracking | **Not performed; confirmed supported phone required** |

## API decisions and official references

Checked 2026-10-01. References guide implementation; they are not physical evidence.

- [Optional AR and runtime installation](https://developers.google.com/ar/develop/java/enable-arcore): AR remains optional; permission/session work follows explicit feature entry.
- [ArCoreApk](https://developers.google.com/ar/reference/java/com/google/ar/core/ArCoreApk) and [availability](https://developers.google.com/ar/reference/java/com/google/ar/core/ArCoreApk.Availability): preserve unsupported, pending, timeout/error, installed and install/update states. Installation continuation uses `userRequestedInstall=false` after the initial prompt.
- [Frame](https://developers.google.com/ar/reference/java/com/google/ar/core/Frame): frame timestamp is nanoseconds in an undefined time base. Preserve `ARCORE_FRAME_UNDEFINED` / camera image capture meaning; measurement age and compatible app measurement time remain unavailable. Timestamp zero is startup; repeated image timestamps are not independent samples.
- [Camera](https://developers.google.com/ar/reference/java/com/google/ar/core/Camera): use physical `getPose()`, never display-oriented pose. Untracked pose is unusable.
- [Pose](https://developers.google.com/ar/reference/java/com/google/ar/core/Pose): metre units, right-handed Hamilton quaternion. World coordinates can change; use a nearby origin anchor's pose from the same frame rather than subtracting a stale initial world position.
- [Anchor](https://developers.google.com/ar/reference/java/com/google/ar/core/Anchor): anchor tracking must also be usable; detach on discontinuity/cleanup.
- [Session](https://developers.google.com/ar/reference/java/com/google/ar/core/Session) and [Config](https://developers.google.com/ar/reference/java/com/google/ar/core/Config): foreground session, GL camera texture/update contract, nonblocking latest-image mode, explicit pause/close, no plane/depth/recording work.
- [Android permission flow](https://developer.android.com/training/permissions/requesting): user action, denial/rationale state, no automatic repeated permission prompt.
- [SystemClock](https://developer.android.com/reference/android/os/SystemClock): integration supplies the shared elapsed-realtime nanosecond clock through an injected function; no competing time package.
- [Official SDK releases](https://github.com/google-ar/arcore-android-sdk/releases/tag/v1.56.0): pinned proposed dependency 1.56.0 (latest official release verified).

## Commands/results

- Initial `git branch --show-current`, `git rev-parse HEAD`, `git status --short`: expected branch/base, clean.
- Prescribed Windows equivalent `cd android; .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`: **failed**, expected missing shared ARCore dependency; exact output `gate-shared-base.log`.
- Same tasks with `-I ..\docs\evidence\TASK-003\validate-with-arcore.init.gradle`: **BUILD SUCCESSFUL** in 2m53s; 37 tests (36 task-owned), zero failures/errors. Exact output `gate-validation-initial.log`. This validates task-owned compilation/tests with the proposed dependency only; it does not prove integrated manifest/navigation acceptance.
- Initial lint: zero errors, four warnings, one hint. Shared scaffold warnings: target API 36, backup extraction rules, missing icon. Task warnings/hint: programmatic custom view constructor and boxed Int state. No tests or acceptance criteria weakened.
- Repair pass 1 SDK-injected gate: **BUILD SUCCESSFUL** in 2m39s; 43 tests, zero failures/errors; `gate-repair-1.log`. Re-review found blocked-resume cancellation could reopen acceptance plus the dependency proposal omitted its actual addition (CRLF-sensitive proposal generation); both are addressed in final pass 2 with a startup-race regression and corrected patch.
- Final repair pass 2: **BUILD SUCCESSFUL** in 57s; **44 tests (43 task-owned), zero failures/errors**; exact output `gate-repair-2.log`, individual JUnit XML in `unit-results/`. Final unchanged task list: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`, SDK injected only through the evidence init script. No tests removed/weakened.
- Final lint: **0 errors, 6 warnings** (`lint-final.txt`). Three shared warnings (OldTargetApi, DataExtractionRules, MissingApplicationIcon); three task debug warnings (programmatic ViewConstructor, two suggestions to use String.toUri). These do not represent acceptance failures; shared settings remain frozen and no warning suppression was added.
- `git apply --check docs/evidence/TASK-003/proposed-shared-integration.patch`: passed, proposal not applied.
- Corrected patch content verified to contain `implementation("com.google.ar:core:1.56.0")`; syntax check alone had not caught the initial omitted addition.
- `git diff --check` plus untracked `git diff --no-index --check -- NUL <file>`: passed under normal Windows CRLF handling. A check with autocrlf disabled initially flagged generated CRLF log/XML line endings; final raw check explicitly accepts CR at EOL while retaining blank-at-eol/blank-at-eof/space-before-tab checks. No source/test change or acceptance weakening. Ownership: all additions in allowed paths; `git diff --name-only` empty. Branch/HEAD unchanged.
- Sensitive-data/API scan of production task packages: no camera/depth acquisition, recording, pose logs, display-oriented pose, alternate app receive clock, radio/geographic identifiers or network measurement code. The only URL uses are explicit Google disclosure links.
- Gates used installed Android Studio JBR/SDK and Gradle caches under sandbox escalation. No auto-review rejection.

Validation APK (ignored build output, no install/device run): `android/app/build/outputs/apk/debug/app-debug.apk`, 10,073,446 bytes, SHA256 `F8A3F6EE4EF90ED60FA0D8104E0F549F4150195DAC9E32E6031FE1152B6CB941`. Its manifest lacks the proposed CAMERA/optional metadata/navigation, so this is compile evidence only. `validation-only-merged-manifest.xml` retains the manifest inspected for the SDK's scoped queries/native integration; it is not a source manifest proposal.

## Changed/untracked files

Exact inventory: `FILES.txt`; task source/fixture SHA256 values: `SOURCE-SHA256.txt`.

- `pose/`: PoseValues, PoseCapture, PoseCapability, PoseProvider, ArCoreRuntime, ArCoreFrameSource, PoseJsonl.
- `spatial/`: PoseDebugPanel, PoseDebugSurface, PoseDebugRoute (pose/debug only).
- Task-owned tests: PoseCaptureTest (16), PoseProviderTest (12), PoseCapabilityTest (7), PoseReplayTest (8).
- `fixtures/pose/`: README and two synthetic JSONL streams (tracking loss and explicit boundary).
- `docs/evidence/TASK-003/`: report, integration proposal/justification, review, physical checklist, exact gate logs, final JUnit/lint evidence, validation manifest and source inventory/hashes.

No tracked/shared files modified; no Kotlin files added to model/time/session. Contracts changed: **none**. Task-local debug serialization IDs are not shared production protocol registrations.

## Proposed shared patches

Exact `proposed-shared-integration.patch` with justification/change note in `INTEGRATION.md`: ARCore dependency, progressive CAMERA with optional camera/AR declarations, debug-only entry and shared receive-clock proposal. Shared model mapping documented; no production model/schema registered here. No shared file edited.

## Physical status and limitations

No physical-device tracking, closure, drift, cancellation, camera-release or resource-use claim has been verified. Synthetic fixtures are labeled replay input, never production measurements. Initial and pass-1 re-review verdicts were **REPAIR REQUIRED**; final independent agent verdict is **PASS WITH FOLLOW-UP**, after inspecting actual pass-2 files, successful gates and corrected proposal. See `REVIEW.md` for findings/resolutions. This verdict covers code only.

Implementation limits: origin-anchor-relative tracking remains local/coarse and does not defend cross-frame continuity after tracking loss; loss conservatively re-origins with new caller-owned IDs. Stored debug keyframes default to 100 ms cadence, deduplicate source timestamps and preserve all tracking/control transitions. The debug host stops at its bounded 3,000-record memory limit (terminal/control records can add a small tail); it saves only on explicit user document export. Physical behavior of GL/camera release, rotation and accuracy remains unverified.

`connectedDebugAndroidTest` was not run: no confirmed supported/authorized physical test target was provided for this session, and shared manifest/debug entry integration is pending. No instrumented or emulator evidence is claimed. The task-required three repeated paths, closure values, degraded tracking, interruption/cancellation and denied/unsupported path are all **pending**, with exact checklist in `PHYSICAL-TESTS.md`.

Official disclosure/terms references added: [ARCore privacy requirements](https://developers.google.com/ar/develop/privacy-requirements), [ARCore terms](https://developers.google.com/ar/develop/terms) and [SDK license](https://github.com/google-ar/arcore-android-sdk/blob/main/LICENSE). Debug panel prominently names Google Play Services for AR and links Google Privacy Policy/Terms. Integration must include release terms/privacy text as applicable.

## Privacy/security

Only session-scoped IDs and local coordinates/orientation/tracking events are exported on explicit request. No imagery, depth, geography, radio identifiers, analytics or background camera. No new actual manifest permissions/dependencies in shared files.

The proposed dependency contains native code and scoped runtime/install queries; see INTEGRATION.md for size, maintenance, data-access/privacy and license review. Export uses the user's selected document provider; choosing a cloud document destination is an explicit user action, not an automatic upload or retention change. No production secrets/signing keys used or changed. Development debug APK uses the scaffold's existing debug signing behavior.

## Deviations, unresolved matters and handoff

- Expected limitation from the explicit shared-file freeze: normal build cannot pass until the integration owner adds ARCore. Validation-only dependency injection proves compilation/tests and is not a substitute for acceptance criterion 1.
- Shared signatures do not exist at this base. Integration owner must reconcile clock/model/capability mapping and provide integrated session/segment/frame IDs; no competing ScanSessionController was implemented.
- No scope, schema, score, privacy-default or permission expansion implemented. No further code questions found in review; physical/integration acceptance remains unresolved.
- Commit/PR reference: **none** as requested. HEAD remains approved base; all task additions are uncommitted/untracked. No push, merge, branch change or worktree deletion performed.

## Next actions

1. Integration owner reviews/applies/reconciles `proposed-shared-integration.patch`, supplies shared model/capability/ID mapping and checks the final merged manifest/dependency changes.
2. On the integrated tree, run the task's normal unmodified assemble/unit/lint gate; no init injection should be needed after integration.
3. Confirm a supported ARCore phone/runtime and execute `PHYSICAL-TESTS.md`, retaining actual sanitized exports and observed closure/tracking/release results. Do not claim the larger product accuracy threshold from these development runs.
4. Review physical evidence and remaining task acceptance with Francis. Any additional code repair must be explicitly authorized because **2/2 autonomous repair passes are used**. No approval question is pending for this handoff.
