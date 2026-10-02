# Probe replay fixtures

`observations.json` contains **synthetic** Task-004 golden records, never production/physical evidence. Canonical meaning and boundaries remain in [protocol/README.md](../../protocol/README.md) and [TASK-004](../../docs/tasks/TASK-004-controlled-endpoint-probe-replay.md).

Eight cases: success, timeout, cancellation, partial transfer, budget refusal, endpoint overload, context change during transfer (A→B→A), and HTTP failure without packet loss. Identity tokens are synthetic/session-local. The timestamp exceeds JavaScript's safe integer range intentionally; decimal strings preserve Long nanoseconds. Receive time and measured interval end are distinct.

Task-local serialization `probe-wave1-alpha-1` maps the approved Wave 1 context contracts: actual begin snapshot, interval start/end, scoped refs, availability/reasons, continuity and the complete boundary journal. The A→B→A case retains both boundaries with before/after snapshots and movement evidence. Older flattened task-local records are not silently reinterpreted; the checkpoint retains the prior fixture version. HTTP procedure version and measurement ceilings are unchanged.

Expected semantics: outcomes in file order are SUCCESS/TIMEOUT/CANCELLED/PARTIAL/BUDGET_EXHAUSTED/SERVER_INVALID/SUCCESS/ERROR. Only the first record is comparison-safe; the successful context-crossing transfer remains unsafe. Transfers are always lower-bound building blocks. Errors have no successful RTT, and no fixture exposes packet loss or score.

Run `npm test` in `measurement-server` for validation/round-trip and rejection cases. `src/replay.ts` exposes `parseReplay` and `replay` for pure future consumers. This is an experimental task-owned replay representation, not an approved shared schema revision or promotion of profile values to production.
