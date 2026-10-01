# AGENTS.md - omanii Repository Rules

This file is the root operating contract for human and AI coding agents. It contains omanii-specific rules only.

## 1. Canonical sources

There is one owner for each kind of truth:

- product scope and truth rules: `PRODUCT.md`;
- architecture and module boundaries: `ARCHITECTURE.md`;
- architecture decisions: `docs/adr/ADR-*.md`;
- bounded work: `docs/tasks/TASK-*.md`;
- measurement/protocol semantics: `protocol/README.md`;
- reusable replay/golden data: `fixtures/`;
- task acceptance evidence: `docs/evidence/<TASK-ID>/`;
- experimental method/promotion: `docs/research/EXPERIMENTS.md`;
- detailed agent workflow/gates/repair: `docs/DEVELOPMENT_CONTROL.md`;
- proposed improvements that are not approved architecture: `docs/proposals/`.

Do not create a competing specification in code comments, branch notes, tool-specific instruction files or new markdown files. Link back to the canonical owner.

## 2. Context discipline

Before implementation, load only:

1. the task packet;
2. files listed under `Required context`;
3. contracts/interfaces the task directly depends on;
4. relevant ADRs.

Do not ingest the whole repository by default. Fresh context is preferred to long accumulated chat history.

Nested `AGENTS.md` files may be introduced later for genuinely specialized modules, but they must narrow these rules, not duplicate them.

## 3. Human authority

Francis retains final approval over:

- product scope;
- architecture;
- shared schema/protocol semantics;
- score-model formulas/coefficients;
- sensitive permissions;
- privacy defaults and retention;
- production credentials and signing keys;
- billing/monetization;
- destructive migrations;
- public releases/rollouts;
- marketing claims;
- physical network-performance claims;
- promotion of experimental algorithms into production.

Agents may propose. They may not silently decide these.

## 4. Architecture change controls

Without an approved task/ADR, an agent may not independently:

- change shared record semantics or versions;
- change score formulas or coefficients;
- change what a measurement means;
- add sensitive permissions;
- add SDKs/runtime dependencies;
- add backend services or endpoint classes;
- change privacy/retention defaults;
- add tracking/analytics fields;
- add monetization behavior;
- broaden product scope;
- promote an R&D algorithm into production.

If implementation reveals a conflict, stop and escalate with the smallest concrete proposal.

## 5. Measurement truth invariants

Never:

- fabricate or simulate production measurement data and present it as real;
- map unavailable platform sentinel values to valid “good” values;
- hide uncertainty to make the UI cleaner;
- paint unknown space as measured;
- treat interpolation as observation;
- count render frames or repeated polling as independent samples;
- attach a long moving transfer to only its final pose;
- silently compare across a network/AP/band/transport change;
- infer exact packet loss from HTTP failure rate;
- infer exact ISP/carrier cause without evidence;
- claim endpoint/path results represent the whole internet;
- give a complete score from the wrong/incomplete profile;
- create a fake Boost effect.

When evidence is insufficient, return tie, inconclusive, invalid, unavailable or unknown as defined by the contract.

## 6. Time and units

- Use monotonic time for event ordering and durations.
- Wall clock is metadata for history/display.
- Preserve actual measurement timestamp separately from callback receive time when available.
- Units are explicit in names/contracts or typed wrappers. Do not rely on undocumented implicit units.
- Null/unavailable has a reason where the platform can distinguish it.

## 7. Android/API rules

No hallucinated Android or ARCore APIs.

When platform behavior is uncertain, verify current official Android/ARCore documentation before implementation and record the source in the task evidence/review notes.

API availability, permissions and hardware support are capability-gated. Device model name alone is not proof of feature support.

## 8. Permission guardrail

No unapproved use of:

- Accessibility;
- background location;
- microphone;
- contacts;
- IMEI or stable device identifiers;
- VPN;
- `QUERY_ALL_PACKAGES`;
- broad storage;
- screen capture;
- unrelated installed-app inventory.

Task-specific camera/location/nearby permissions must be progressive and justified.

## 9. Dependency guardrail

A new dependency requires a short change note covering:

- purpose;
- size/native code;
- permissions/data access;
- maintenance risk;
- privacy;
- license if relevant;
- alternatives.

Do not add a framework to avoid writing a small amount of ordinary code.

## 10. Task ownership

Every task declares owned paths. An agent edits only those paths plus explicitly listed shared files.

Changes outside owned paths require escalation before editing. Do not “fix nearby things” opportunistically in a parallel branch.

One integration owner resolves shared build/schema files. One shared schema owner at a time.

## 11. Worktree convention

Branch: `<type>/task-<NNN>-<short-name>`
Worktree: `../omanii-task-<NNN>`

Examples:

- `feat/task-002-capability-radio`
- `feat/task-003-ar-pose`
- `feat/task-004-measurement-probe`

No agent runs destructive Git operations outside its worktree.

## 12. Deterministic gates

Run the task's verification commands. At minimum, once the relevant scaffold exists:

- compile;
- unit tests;
- lint/static checks;
- schema/fixture validation where touched;
- manually specified golden cases where score/measurement semantics are involved.

Integration adds:

- permission-manifest diff;
- dependency diff/review;
- fixture replay;
- cancellation/no-late-traffic tests;
- byte-budget checks;
- sensitive-data/log checks.

Passing automation is necessary, not sufficient, for physical claims.

## 13. Physical evidence

Physical-device evidence is mandatory when a task claims behavior involving:

- AR tracking;
- radio observations/freshness;
- spatial repeatability;
- network improvement;
- battery/thermal behavior;
- real cancellation/byte behavior over networks.

An emulator or AI reviewer cannot approve these claims.

## 14. Independent review

High-risk work should be reviewed by a different agent/model where practical.

Reviewer inputs:

- task packet;
- relevant contracts/ADRs;
- diff;
- evidence.

Allowed verdicts:

- PASS;
- PASS WITH FOLLOW-UP;
- REPAIR REQUIRED;
- ESCALATE.

Self-certification alone is not enough for measurement, permissions, security, billing or shared protocol changes.

## 15. Bounded repair

Repair loops must stay inside the original task.

Circuit breakers:

- maximum two autonomous repair passes before escalation unless the task explicitly sets another number;
- no architecture/schema/permission expansion during repair;
- no production secrets;
- no destructive operations outside the worktree;
- stop on conflicting requirements;
- stop when acceptance cannot be verified;
- stop when physical evidence is required but unavailable.

## 16. Required handoff

Every completed task reports:

- files changed;
- contracts changed (or “none”);
- commands run;
- automated results;
- evidence path;
- physical verification required/completed;
- limitations;
- unresolved questions;
- privacy/security implications;
- deviations from task;
- commit/PR reference.

“Code exists” is not completion. Acceptance evidence is completion.
