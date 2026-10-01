# Autonomous Development Control System

This is the detailed operational contract behind the concise rules in `AGENTS.md`.

## 1. Durable state

The repository, not chat history, is the shared memory.

Durable state lives in:

- canonical product/architecture/ADRs;
- task packets;
- protocol/fixtures;
- task evidence;
- approved proposals;
- Git history/PRs.

Agents start from fresh context when practical and load only required material.

## 2. Task packet schema

Every implementation task contains:

- ID
- title
- status
- objective
- product/user reason
- dependencies
- owned paths
- required context
- interfaces/contracts relied upon
- interfaces forbidden to modify
- implementation requirements
- acceptance criteria
- verification commands
- automated tests
- physical-device evidence
- security/privacy considerations
- out-of-scope items
- stop/escalation conditions
- completion/handoff format
- recommended agent role

Canonical status values:

- `DRAFT`
- `BLOCKED`
- `READY`
- `IN_PROGRESS`
- `REVIEW`
- `REPAIR_REQUIRED`
- `DONE`

`DONE` requires acceptance evidence.

## 3. Parallel worktree model

Branch naming:

- `feat/task-002-capability-radio`
- `feat/task-003-ar-pose`
- `feat/task-004-measurement-probe`

Worktree naming:

- `../omanii-task-002`
- `../omanii-task-003`
- `../omanii-task-004`

Rules:

- branch from the same approved integration point;
- task agent owns only declared paths;
- one shared schema owner;
- one integration owner handles shared Gradle/manifest/schema conflicts;
- do not alter another task's owned path without escalation;
- do not rebase/force-push another agent's branch;
- clean worktrees only after merge/evidence retention.

### Empty-repo bootstrap correction

Because Tasks 002 and 003 both need the same Android project, they must not independently create competing Gradle/app scaffolds.

Immediately after Task 001 approval, the integration owner creates **one minimal non-feature Kotlin/Compose Android build scaffold** on the integration branch. No scanner/network features are added. Tasks 002 and 003 then branch from that commit. Task 004 may begin its server-side/replay work in parallel and rebase its Android probe subpath onto the shared scaffold before integration.

This is a merge-control step, not a new product task.

## 4. Interface freeze

Before parallel coding:

- protocol semantics in `protocol/README.md` are frozen for the task set;
- task-owned paths are declared;
- any shared Kotlin model/adapters created during the scaffold are owned by the integration/schema owner;
- agents may implement adapters against the contracts but may not change field meaning.

If a contract proves impossible on Android, the implementer stops and files a concrete proposal rather than silently changing it.

## 5. Fast local gates

Each branch runs the task's exact commands.

Typical Android gate after scaffold:

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Typical TypeScript measurement-server gate after Task 004 scaffold:

```bash
cd measurement-server
npm ci
npm run typecheck
npm test
```

Repository hygiene:

```bash
git diff --check
```

No command is considered evidence if the output was not produced on the actual task commit.

## 6. Integration gates

Before merge of measurement-critical changes:

- branch local gates pass;
- fixture/replay tests pass;
- manually specified golden cases pass where applicable;
- Android manifest permission diff reviewed;
- runtime dependency diff reviewed;
- sensitive-data/log review passes;
- cancellation/no-late-traffic check passes for active probes;
- byte budget check passes;
- network-transition invalidation tests pass;
- reviewer verdict is PASS or an explicitly accepted PASS WITH FOLLOW-UP.

## 7. Physical gates

Physical evidence is required for claims involving pose, radio, actual network behavior, resource use or real improvement.

Initial evidence should record:

- exact phone model/API/build;
- feature capability/permissions;
- environment/coarse test condition;
- profile/version/endpoint;
- number of independent runs;
- byte usage;
- relevant tracking/network transitions;
- thermal/battery observations where in scope;
- raw/sanitized evidence location;
- failures/limitations.

No amount of emulator output upgrades a physical claim.

## 8. Independent review

Reviewer gets only what is needed:

- task packet;
- relevant contracts/ADRs;
- diff;
- evidence.

Reviewer checks the requirements independently, not merely the author's explanation.

Verdicts:

- `PASS`
- `PASS WITH FOLLOW-UP`
- `REPAIR REQUIRED`
- `ESCALATE`

High-risk concerns include timing, measurement meaning, permissions, sensitive data, cancellation, budgets, score math, credentials and security boundaries.

## 9. Bounded repair loop

Default:

implementation
→ gates
→ review
→ at most two bounded repair passes
→ gates/review
→ handoff or escalation

A repair pass may fix defects inside task scope. It may not:

- change architecture;
- change schema/measurement meaning;
- change score coefficients;
- add permissions;
- add major dependencies/services;
- touch production credentials;
- weaken acceptance criteria;
- delete a failing test because it is inconvenient.

## 10. Evidence format

Every task creates/updates `docs/evidence/<TASK-ID>/REPORT.md` at handoff, containing:

- task/commit;
- files changed;
- contracts changed;
- commands and results;
- acceptance table;
- reviewer verdict;
- physical evidence status;
- known limitations;
- unresolved questions;
- privacy/security impact;
- deviations.

Large/sensitive logs remain outside Git when necessary; the report records where/why and stores only sanitized summaries needed for review.

## 11. Proposal channel

Agents may create a small proposal when they discover:

- bad/missing API contracts;
- recurring friction;
- architecture weakness;
- flaky/unhelpful tests;
- automation opportunities;
- privacy/resource concerns.

A proposal is not permission to implement the change.

## 12. Human approval matrix

Founder approval is mandatory for:

- product-scope changes;
- ADR/architecture changes;
- protocol/score meaning changes;
- permissions and privacy/retention;
- credentials/secrets;
- monetization/billing;
- destructive migrations;
- public release/rollout;
- marketing/performance claims;
- experimental algorithm promotion.

Automation may auto-run tests, open a review, prepare a fix and pause optional server features in a future approved operations design. It may not silently cross this matrix.

## 13. No custom orchestrator yet

Do not build a task daemon, multi-agent scheduler or autonomous deployment platform in Bootstrap/first experiments. Git worktrees, task docs, CI and explicit evidence are sufficient until repeated friction proves otherwise.
