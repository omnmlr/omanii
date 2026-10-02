# Proposed scoped repair — not applied

Historical pre-repair reference. The user subsequently authorized one exceptional scoped pass. The two fixes were inspected and implemented manually with four focused regressions; final gates and independent re-review pass with follow-up. **Do not apply this draft to the repaired tree.** See `repair-3/REVIEW.md` and the current REPORT.md section for actual results. The original draft/check below is retained as historical evidence.

Canonical requirements remain TASK-003, protocol/README.md and the approved Wave 1 contracts. This proposal addresses the two findings in REVIEW.md without changing shared contracts, permissions, dependencies or scan scheduling.

`proposed-repair.patch` changes only PoseCanonicalMapping.kt, PoseProvider.kt and their matching tests:

1. Use length-prefixed session/run tuple encoding for source-clock instance and development record IDs. Existing allowed tokens remain valid, including underscores. Raw frame timestamp/domain/FRAME_CAPTURE meaning and unknown measurement time remain intact. Update the exact token expectation in export/tamper tests and add a collision regression for `(a_b, c)` versus `(a, b_c)`.
2. Queue caller-driven segment changes made reentrantly during an accepted provider update. Finish that update's ordered control/sample batch before applying queued boundaries. Cancellation closes the acceptance gate and discards pending changes; origin/frame/cadence are preserved. Add recovery-order canonical/replay and cancellation regressions.

`git apply --check docs/evidence/TASK-003/adaptation/proposed-repair.patch`: **PASS**, against the current uncommitted source snapshot. The patch is **not applied, compiled or tested**. The proposal adds three tests; it removes no tests and weakens no acceptance criteria. If approved, the expected full count becomes 95 tests / 63 task tests, subject to actual gate results.

Approval is required because AGENTS.md §15 states “maximum two autonomous repair passes before escalation unless the task explicitly sets another number,” and docs/DEVELOPMENT_CONTROL.md §9 defines at most two bounded repairs. The earlier reviewed implementation already used both passes, and the approved foundation does not reset that count. The user authorized checkpoint/merge/adaptation, but did not explicitly authorize another repair pass. Cumulative count remains 2/2.

Requested scope: one additional repair pass applying this patch, unchanged normal build/unit/lint gates and independent re-review; no shared edits, push or adaptation commit. This proposal is concrete and ready for that authorization.
