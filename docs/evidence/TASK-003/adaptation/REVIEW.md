# Pre-repair shared-foundation adaptation independent review — 2026-10-02

Historical verdict against the pre-repair snapshot. The authorized exceptional repair and new PASS WITH FOLLOW-UP verdict are recorded in `repair-3/REVIEW.md`; the findings below are resolved in the current source.

Reviewer: independent agent `/root/review_pose`, read-only review of current task sources, canonical contracts, diff and normal-gate evidence.

Verdict: **ESCALATE**. Two defects remain and the cumulative autonomous repair budget is already 2/2. No reviewer edits or third repair occurred. The checkpoint's PASS WITH FOLLOW-UP does not approve this adaptation.

- **P1 — Reentrant segment changes corrupt event ordering.** PoseProvider.kt:88 can change capture identity while update is emitting a batch already accepted at lines 67–70. During recovery, capture has advanced to f1 but the first emitted TRACKING record still refers to f0. A callback changing segment emits a boundary from f1 before the pending f0→f1 boundary. Replay rejects the sequence; the canonical mapper can throw. Required regression: TRACKING→PAUSED→TRACKING, segment change from recovered TRACKING callback, ordered canonical/replay output; cancellation must discard the queued change.
- **P2 — Ambiguous session/run encoding.** PoseCanonicalMapping.kt:46 and :94 concatenate allowed underscore-bearing tokens. Valid pairs `(a_b, c)` and `(a, b_c)` produce the same ARCORE_FRAME_UNDEFINED domain instance and record ID. Required regression: distinct tuple identity for both source domain and record ID. Use an unambiguous encoding without changing shared contracts or task-local provenance.

Reviewer verified normal gates: 92 tests / 60 task tests, zero failures/errors/skips, successful build and lint. These two cases are absent from the current tests. No other blocking adaptation defects found.

An initial suspected JsonNumber export defect was withdrawn after the reviewer read the current encoder tail: `is JsonNumber -> v.raw` is already present. It was never an actual finding against the gate snapshot, and no repair was needed.

Physical acceptance remains pending: three independent supported-phone paths with measured closure, deliberately degraded tracking, background/interruption/cancellation and camera release, rotation, denied/unsupported fallback, and export/replay inspection. Debug navigation remains integration-owned and unwired.

Concrete unapplied proposal: `proposed-repair.patch`, with rationale/approval boundary in `PROPOSED-REPAIR.md`. No new PASS is asserted.
