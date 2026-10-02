# Integration-owner proposal (not applied)

`proposed-shared-integration.patch` is an exact patch against base `c1b3903ca9078651d74cefad9c22b67e5acd3f12`. Review/apply through the integration owner. Task-owned code intentionally references the ARCore SDK; the normal scaffold cannot compile it until the dependency is integrated.

## Proposed changes and justification

1. Add `com.google.ar:core:1.56.0`: official optional runtime pose source accepted by ADR-001. No rendering engine. No cloud/depth/plane/recording features enabled. The validation-only init script supplies this dependency without modifying the build file.
2. Add CAMERA and explicitly optional camera/autofocus/AR features plus optional ARCore metadata: supported runtime gate on feature entry; unsupported devices still install/use non-AR paths. No background camera or additional sensitive permission.
3. Add debug-only entry to the existing shell: opening the panel does not request permission or initialize a session. Start is a separate explicit action. Leaving the panel cancels capture. No scanner UI or global controller.
4. Supply the frozen shared `MonotonicClock` backed by `SystemClock.elapsedRealtimeNanos()`. This exact minimal proposal is reserved to the integration owner; adapt to an existing shared clock if one is created elsewhere. The task-owned adapter accepts `clock::nowNs`, so it has no competing clock implementation or clock-domain conversion.

No shared models exist at this base. `PoseRecord` and related values are task-local adapter/debug values, not proposed production schema definitions. Integration maps emitted records to its shared `PoseObservation`/session records and supplies IDs through `initialIdentity`/`nextIdentity`. `nextIdentity` must synchronously return fresh segment/frame IDs on pose discontinuity. It must not run long work. The adapter owns no integrated scheduling/network boundaries.

Map `receivedAtMonotonicNs` directly to app receive time; leave compatible `measurementAtMonotonicNs` null and `ageKnown=false`. Retain raw source timestamp/domain/meaning. Do not associate ARCore timestamp numerically with elapsed realtime. Null pose on tracking events is intentional. A new coordinate frame cannot be compared geometrically with its predecessor. Capability contribution includes runtime and permission states plus manual-fallback eligibility; it asserts no physical tracking success.

## Dependency change note

- Purpose/alternative: certified ARCore tracking with anchor-relative local pose. Platform camera/sensors alone do not provide validated room-scale tracking. Manual two-spot remains the fallback; no inertial substitute or larger renderer.
- Pin/maintenance: 1.56.0 verified in [official release notes](https://github.com/google-ar/arcore-android-sdk/releases/tag/v1.56.0), September 2026. Runtime Google Play Services for AR updates independently; capability/install/session failures remain explicit.
- Size/native: downloaded AAR is **356,212 bytes**. It includes `libarcore_sdk_c.so`/`libarcore_sdk_jni.so`; actual APK delta and installed runtime size need integration/device measurement. Native tracking primarily resides in the separately installed runtime.
- Permissions/data: progressive CAMERA only in proposed app declarations. AAR merged manifest adds narrowly scoped queries for `com.google.ar.core`, `com.android.vending`, and the Play install service action, ARCore install activity and minimum-runtime metadata. No QUERY_ALL_PACKAGES or unrelated inventory code. The validation-only merged manifest still lacks the proposed CAMERA/optional metadata and is not a runnable feature acceptance artifact.
- Privacy: omanii never acquires/retains/uploads camera image/depth buffers; anchor/pose processing uses the runtime. Support checks may query Google's service; installation uses Play. The panel includes the runtime disclosure and links. Google runtime data processing must not be described as an omanii guarantee. Release terms/privacy copy remains integration/human-owned.
- License: [SDK LICENSE](https://github.com/google-ar/arcore-android-sdk/blob/main/LICENSE) distinguishes the SDK binary license, Apache-licensed sample content and third-party notices. Do not describe the entire binary SDK as Apache-2.0. [ARCore terms](https://developers.google.com/ar/develop/terms) and [privacy requirements](https://developers.google.com/ar/develop/privacy-requirements) apply. Preserve SDK notices and integrate the required disclosure.

## Validation and next integration gates

Apply the reviewed patch in the integration worktree, reconcile shared clock/ID/model ownership, then run the task's unchanged assemble/unit/lint commands. Review the merged permission/dependency manifest, inspect the debug route on device, and run the physical matrix in REPORT.md. Transient SDK validation is compile/test evidence only. No patch has been applied to this worktree's shared sources.
