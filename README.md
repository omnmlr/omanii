# omanii

omanii is an Android-first connectivity product whose first technical goal is to help a user find and verify where their current connection works better, while exposing uncertainty instead of manufacturing precision.

This repository is intentionally in **Bootstrap Task 001** state. It contains architecture, contracts, agent controls, experiment boundaries and the first implementation tickets. It does not yet contain application feature implementation.

## Start here

Humans:

1. Read `PRODUCT.md` for the product/truth contract.
2. Read `ARCHITECTURE.md` for system boundaries and the measurement pipeline.
3. Read `docs/adr/` for accepted decisions.
4. Approve Bootstrap Task 001 using the checklist in the delivered package.

Coding agents:

1. Read `AGENTS.md`.
2. Open only your assigned file in `docs/tasks/` plus its required context.
3. Work only inside owned paths.
4. Finish with evidence, not an implementation claim.

## Repository map

```text
omanii/
├── android/                  # Android implementation begins after bootstrap approval
├── web/                      # Future lightweight website boundary; not implemented now
├── measurement-server/      # Controlled measurement service begins in TASK 004
├── protocol/
│   └── README.md             # Canonical conceptual records/version semantics
├── fixtures/
│   └── README.md             # Golden/replay fixture contract and initial catalog
├── docs/
│   ├── adr/                  # Accepted architectural decisions
│   ├── tasks/                # Bounded implementation packets
│   ├── evidence/             # Per-task acceptance evidence
│   ├── research/             # Experiment contract and R&D proposals
│   └── proposals/            # Non-canonical improvement proposals
├── PRODUCT.md
├── ARCHITECTURE.md
├── AGENTS.md
└── README.md
```

## Decided in Bootstrap Task 001

- Android first, native Kotlin, Jetpack Compose.
- ARCore is optional and initially a pose source.
- Basic connectivity testing works without ARCore.
- Wi-Fi is the first spatial validation target; cellular spatial mapping requires separate evidence.
- One session/protocol semantics model, explicit uncertainty and monotonic timing.
- Controlled measurement endpoints with byte/cancellation limits.
- Local-first sensitive data.
- Separate, versioned Omanii Score and OG Score models.
- Independent stationary verification before a “better spot” recommendation.
- Manual two-spot fallback.
- Consumer personality is above the measurement engine.
- Multiple agents use bounded tasks/worktrees, deterministic gates, independent review and physical evidence.

## Not decided yet

Cell size, interpolation, confidence equation, temporal-drift correction, scan cadence, exact probe schedule, score coefficients, OG coefficients, cellular spatial mapping, cross-device calibration, cross-session alignment, predictive field completion and wall-aware modelling are experimental until evidence justifies a decision.

## Development principle

> Serious measurement underneath. Consumer magic above it.

Neither half is allowed to corrupt the other.
