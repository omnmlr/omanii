# ADR-007 - AI-agent development workflow

**Status:** Accepted

## Context

omanii will be implemented quickly with multiple coding agents. The main risk is not only bad code; it is concurrent agents independently changing schemas, semantics, permissions, dependencies and assumptions so the repository stops representing one product.

## Decision

Use repository-native control rather than a custom orchestration platform.

Workflow:

specification
→ bounded task packet
→ interface freeze
→ isolated branch/worktree
→ implementation
→ deterministic gates
→ independent review
→ bounded repair
→ acceptance evidence
→ integration
→ required physical verification

Every task declares owned paths, required context, contracts relied upon, forbidden interfaces, acceptance criteria, verification commands, physical evidence, privacy/security considerations and stop conditions.

Canonical state lives in Git/docs/fixtures/evidence/ADRs. Chat history is not the system of record.

One schema owner and one integration owner control shared changes.

## Consequences

- Agents can run with smaller, fresher context.
- Parallel work is safer but requires path discipline.
- Some “helpful” cross-cutting refactors must wait for explicit tasks.
- Review/evidence becomes part of throughput, not ceremony added later.

## Alternatives considered

### One giant autonomous agent/chat

Rejected because context drift, self-review and hidden state become failure modes.

### Build a custom agent orchestration service immediately

Rejected as premature infrastructure. Git, task docs, worktrees and CI are enough to start.

## Explicitly undecided

- which model/tool fills each role long term;
- future automated task selection/orchestration;
- merge queue implementation;
- code-owner tooling beyond the documented contract.
