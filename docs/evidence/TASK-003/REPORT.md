# TASK-003 implementation evidence

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
