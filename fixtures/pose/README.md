# TASK-003 pose replay inputs

All JSONL files are **synthetic deterministic test inputs**, not camera/device measurements.
See `protocol/README.md`, ADR-003 and `docs/evidence/TASK-003/REPORT.md` for contract ownership and validation limits.

- `tracking-loss.jsonl`: origin (0,0,0), then (2,1,-1) metres; paused tracking with no coordinates; new segment/frame after recovery, then (-1,0.5,2). Stop reason CANCELLED. Horizontal golden points are (0,0), (2,-1), (0,0), (-1,2).
- `explicit-break.jsonl`: caller-requested segment/frame boundary followed by a new origin. Stop reason BACKGROUNDED.
- `network-only-segment.jsonl`: external comparison segment change retains coordinate frame `f0`, origin and pose source; later position (2,1,-1) stays in the original local frame. No network implementation or measurement is present.

The task-local debug dialect carries schema/procedure IDs and an explicit synthetic provenance marker. It is not a registered shared production schema. Nanosecond receive times and ARCore undefined-clock source times are distinct; compatible measurement time and age are unavailable. Frames cannot be joined across a boundary.
