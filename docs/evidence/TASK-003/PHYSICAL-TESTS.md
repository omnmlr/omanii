# TASK-003 pending physical evidence

Status: **not performed**. This is the task-required test checklist, not results or a research-promotion proposal. Canonical acceptance remains TASK-003 and the later experiment gate in PRODUCT/EXPERIMENTS.

Before running, the integration owner must apply/reconcile the reviewed shared patch and produce the integrated debug build. Record device model, Android API/build, app source/build hash, ARCore installed runtime version and runtime AVAILABLE/permission states. A model name or sideloaded runtime alone does not establish support.

| Required run/check | Evidence to retain | Observed result |
| --- | --- | --- |
| Same marked-start short room path, run 1, slow movement | Coarse light/texture condition, JSONL, start/end sample IDs, frame/segment IDs, horizontal and 3D closure | Pending |
| Same path, run 2, normal movement | Same evidence, independent run/session | Pending |
| Same path, run 3, repeated movement | Same evidence, independent run/session | Pending |
| Deliberately degraded tracking (safe lower light/low texture) | Failure reason, tracking transition, absence of paused coordinates, new frame/segment after recovery | Pending |
| Foreground interruption/background and explicit restart | BACKGROUNDED stop, camera release, no later valid sample; new session/origin on explicit restart | Pending |
| Cancellation during capture/startup | CANCELLED reason, no valid samples after terminal event, pause/close completion, camera available to another foreground camera app | Pending |
| Denied camera or unsupported path | No crash, explicit runtime/permission fallback; basic shell usable, no implicit camera request | Pending |
| Display rotation/GL recreation | Physical camera orientation retained independently of display geometry; recreation marks a discontinuity | Pending |
| Export/replay inspection | Explicit document save; no imagery/depth/geography/radio identifiers; schema/source clocks and segment timeline preserved | Pending |

For a continuous coordinate frame, closure is the distance between actual first and return-to-mark samples: report `sqrt(dx²+dz²)` horizontally and `sqrt(dx²+dy²+dz²)` in 3D, in metres. Record which samples were selected and the wait/steady condition. Across a tracking/frame break, report closure **unavailable/incomparable** rather than joining frames. Report drift, dropouts and anchor-local limitations as observed.

No handful of development runs proves the product median/p90 accuracy targets. Do not record fabricated numerical results, imagery or precise geographic location. Resource/battery/thermal behavior is not claimed by this task implementation. `connectedDebugAndroidTest` remains conditional on an appropriate authorized device and integration; no instrumented result is reported here.
