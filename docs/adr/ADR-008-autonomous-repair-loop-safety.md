# ADR-008 - Autonomous execution and repair-loop safety

**Status:** Accepted

## Context

Automated coding/review loops can improve throughput, but repeated self-repair can also widen scope, rewrite architecture, add permissions/dependencies or chase failing tests indefinitely. omanii also contains physical claims that software automation cannot verify.

## Decision

Autonomous repair is bounded by the original task packet.

Default circuit breakers:

- maximum two autonomous repair passes before escalation unless the task explicitly overrides;
- owned-path restriction remains active;
- no architecture/schema/score/permission expansion during repair;
- no production secrets/credentials;
- no destructive operations outside the task worktree;
- stop on conflicting requirements;
- stop when acceptance cannot be deterministically verified;
- stop when required physical evidence is unavailable;
- stop when fixing the issue would require changing a forbidden interface.

A fresh reviewer/agent context is preferred to endlessly accumulating repair context.

Automation may prepare a change and evidence. It may not approve protected human-authority decisions.

## Consequences

- Some failures escalate earlier instead of being “fixed” by broad rewrites.
- Repair cost is bounded.
- Human attention focuses on real ambiguity/authority decisions.
- Future automation can select and run tasks without becoming an architecture owner.

## Alternatives considered

### Unlimited repair until CI passes

Rejected because passing tests can be achieved by weakening requirements, deleting checks or changing semantics.

### No autonomous repair

Rejected because small bounded fixes are an efficient use of coding agents.

## Explicitly undecided

- future orchestration implementation;
- per-risk task repair limits;
- automated rollback/release controls;
- automatic task prioritization.
