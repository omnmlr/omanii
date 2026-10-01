# TASK 003 - ARCore Pose & Scan Session Adapter

**Status:** READY after Bootstrap Task 001 approval and shared Android build scaffold
**Type:** Implementation / spatial evidence

## Objective

Build the smallest foreground spatial subsystem that can truthfully record session-local pose observations during a short movement session, including tracking loss/discontinuity and a non-AR capability fallback.

This task does not build the final scanner visualization or network heatmap.

## Product reason

omanii's hero workflow needs to know where the phone moved well enough to associate later network evidence with regions. The first technical question is whether session-local movement/tracking is reliable enough for coarse room-scale decisions, not whether the app can render a futuristic room.

## Dependencies

- Bootstrap Task 001 approved and merged.
- Shared minimal Kotlin/Compose Android build scaffold created once by the integration owner.
- `PRODUCT.md`, `ARCHITECTURE.md`, `protocol/README.md` accepted.
- ADR-001, ADR-002, ADR-003, ADR-004, ADR-005 accepted.

It may run in parallel with Task 002. It must not depend on Task 002's network implementation for its core pose evidence.

## Owned paths

Target paths after scaffold:

- `android/app/src/main/java/**/pose/**`
- `android/app/src/main/java/**/spatial/**` for session-local pose capture/debug only
- corresponding tests
- `fixtures/pose/**`
- `docs/evidence/TASK-003/**`

Shared manifest/build files required for ARCore/camera capability must be coordinated with the integration owner.

## Required context

Read only:

- `PRODUCT.md` - Hero experience/truth/permissions.
- `ARCHITECTURE.md` - Pose subsystem, spatial session model, state/cancellation boundaries.
- `AGENTS.md`.
- `protocol/README.md` - `PoseObservation`, `ScanSession`, `SessionSegment` semantics.
- ADR-001, ADR-002, ADR-003, ADR-004, ADR-005.
- This task packet.

Verify uncertain ARCore/Android API behavior against current official documentation during implementation.

## Interfaces/contracts used

- `PoseProvider`
- `CapabilityReport` contribution for AR pose support
- `PoseObservation`
- `ScanSession`/`SessionSegment` pose-side fields
- monotonic time contract
- tracking-loss/segment semantics

## Interfaces forbidden to change

This task may not redefine:

- network/radio observation semantics;
- active probe profiles;
- candidate/verification logic;
- score models;
- privacy defaults;
- product scope;
- the meaning of measured/interpolated/unknown.

## Implementation requirements

### 1. ARCore capability gate

Determine runtime ARCore support/availability using supported mechanisms rather than device-name guessing.

States must distinguish at least:

- supported/available;
- supported but runtime/install/update action required if that is a real platform state used by implementation;
- unsupported;
- permission unavailable/denied;
- initialization/tracking failure.

The exact Kotlin enum may follow official API states, but consumer/product code must not collapse unsupported into a crash.

### 2. Progressive camera permission

Camera permission is requested only when the user/developer starts the spatial feature/debug session, not for basic app startup.

Denial leaves a clear non-AR capability/fallback state. No background camera.

### 3. Session-local origin

On successful tracking start:

- establish a local coordinate frame/origin for that scan;
- record x/y/z in metres and orientation;
- record coordinate-frame ID;
- retain height;
- expose a documented horizontal projection for debug/analysis;
- do not claim geographic coordinates or architectural alignment.

### 4. Pose observations

Emit pose observations with:

- monotonic timestamp;
- x/y/z;
- orientation;
- tracking state;
- segment/frame ID;
- optional provider quality if defensibly exposed.

Pose update cadence may be higher than stored/replay cadence. Do not persist every camera/render frame by default if it is unnecessary. The task should make later downsampling/keyframing possible without losing tracking transitions.

### 5. Tracking loss and discontinuity

When tracking is paused/lost:

- stop placing valid spatial samples;
- emit tracking state transitions;
- resume only when state is valid;
- create a new segment if continuity cannot be defended;
- never bridge a discontinuity by drawing invented geometry.

### 6. Foreground lifecycle and cancellation

A short capture session must:

- stop/pause when the owning foreground lifecycle no longer permits active scan behavior;
- be cancellable immediately;
- release AR/camera resources cleanly;
- record stop/cancel reason.

No background AR service.

### 7. Development export/replay

Export a sanitized inspectable pose/session stream containing schema/protocol IDs and tracking transitions. JSONL is acceptable for development evidence.

No raw camera video/image recording by default.

Provide replay/test input support sufficient for later deterministic session/field work. This may be a parser/test helper rather than a production replay UI.

### 8. Minimal debug visualization

A minimal debug view may show:

- tracking state;
- local x/z route;
- origin;
- segment breaks;
- current pose coordinates.

It must not:

- render a finished scanner UI;
- invent walls/room geometry;
- interpolate network quality;
- use fake measurement colors.

### 9. Fallback state

On unsupported AR hardware or denied camera permission, expose the capability state needed for the product to offer a manual two-spot workflow later. Do not implement the full two-spot product in this task.

## Acceptance criteria

1. App builds with ARCore capability integrated through the shared scaffold.
2. Spatial feature does not request camera permission until explicitly started.
3. Unsupported/denied devices enter an explicit fallback-capable state without crash.
4. Supported device produces monotonic timestamped local pose/orientation records.
5. Tracking loss causes invalid/paused sample placement and is visible in export/debug state.
6. A discontinuity can create a new segment; no code silently interpolates across it.
7. Cancellation/background lifecycle releases active capture and records reason.
8. Development export/replay contains enough information to reproduce the pose path and tracking-state timeline without raw camera imagery.
9. Debug visualization shows only actual pose/path/segment evidence and contains no final scanner geometry.
10. Physical evidence records return-to-start closure and at least one deliberately degraded tracking condition; the task reports the observed error rather than claiming the research threshold has been proven globally.

## Verification commands

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
git diff --check
```

If instrumented tests/device are available:

```bash
cd android
./gradlew :app:connectedDebugAndroidTest
```

Record exact output under `docs/evidence/TASK-003/`.

## Automated tests

At minimum using fakes/replay:

- local origin initialization;
- monotonic ordering;
- horizontal projection math on known poses;
- tracking valid → paused → valid transition;
- discontinuity creates segment boundary when requested by provider/session policy;
- cancellation stops provider collection;
- lifecycle stop does not keep emitting valid pose samples;
- export contains schema/protocol/coordinate-frame/tracking state;
- unsupported capability returns fallback state;
- denied camera permission returns fallback state;
- replay preserves segment boundaries.

Do not assert physical AR accuracy in unit tests.

## Physical-device evidence

On at least one confirmed supported ARCore phone:

- three short repeated room paths;
- return to a marked start point and report closure error each run;
- at least one slow/normal movement case;
- one deliberate tracking degradation case (e.g. lower light/low texture, safely performed);
- pause/resume or app interruption;
- cancellation;
- export/replay inspection.

Also record behavior on an unsupported or capability-denied path, which may be a different phone or camera denial.

Evidence should include actual observed limitations. Do not claim the full product's `<=0.5 m median / <=1 m p90` gate from a handful of development runs; that gate belongs to the later experiment matrix.

## Security/privacy considerations

- No camera frames/images/video retained or uploaded by default.
- Debug export contains coordinates only relative to the session and no precise geographic location.
- No analytics SDK receives pose/room data.
- Camera permission is progressive.
- No background camera/location.

## Out of scope

- final scanner UI/visual identity;
- plane/wall/room reconstruction;
- network/radio measurements;
- field interpolation;
- candidate selection/verification;
- score/diagnostics;
- cross-session alignment;
- cellular mapping;
- raw AR recording for research unless a separate explicit task/consent exists.

## Stop / escalation conditions

Stop and escalate if:

- required AR behavior cannot be implemented using supported APIs without changing the privacy/permission contract;
- a renderer/dependency larger than the accepted ARCore/native approach appears necessary;
- the task would require inventing geometry to satisfy a UI expectation;
- real-device tracking cannot produce usable session-local pose at all in ordinary supported conditions;
- a shared schema/Gradle conflict would require editing another task's owned path without integration-owner approval;
- physical evidence required for a claim is unavailable.

## Handoff

Report:

- files changed;
- ARCore/Android APIs and official references for uncertain behavior;
- contracts changed: expected `none` unless separately approved;
- permissions/dependencies;
- commands/results;
- evidence path;
- physical device/model and closure observations;
- tracking failure modes;
- fallback behavior;
- privacy/security implications;
- unresolved questions;
- commit/PR reference.

## Recommended agent role

AR/Android measurement implementer with device-debugging access. Independent reviewer should focus on lifecycle/cancellation, tracking-loss handling, privacy and any accidental geometry claims.
