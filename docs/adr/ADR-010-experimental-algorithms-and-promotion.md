# ADR-010 - Experimental algorithms and promotion

**Status:** Accepted

## Context

The largest potential technical differentiation may come from better separation of time/space, adaptive scanning, uncertainty modelling, sparse prediction, device normalization and experienced-performance fusion. These are research questions, not established product capabilities.

Without an explicit boundary, an exciting experiment can silently become “truth” because it looks better in a demo.

## Decision

Experimental algorithms are isolated behind stable production contracts and tracked using `docs/research/EXPERIMENTS.md`.

Each experiment declares:

- hypothesis;
- baseline;
- algorithm/change;
- data;
- metric;
- expected improvement;
- failure condition;
- device/environment scope;
- replay compatibility;
- physical validation;
- privacy/resource impact;
- outcome.

Production promotion requires held-out evidence against the baseline, real-device evaluation where relevant, independent review, explicit version/ADR consequences and Francis approval.

The initial baseline should be simple and falsifiable: robust local aggregation plus bounded local interpolation and independent stationary verification. More sophisticated methods are alternatives to beat, not default prestige choices.

## Consequences

- R&D can move aggressively without contaminating public claims.
- Algorithms can be replay-compared on the same evidence.
- Some visually impressive ideas will remain experimental or be rejected.
- Model promotion is slower than a config flip by design.

## Alternatives considered

### Put experiments directly behind remote flags in production

Rejected until an approved experiment framework, user impact and data/privacy design exists.

### Ban advanced methods until after launch

Rejected. Early experiments may discover a real advantage, but must remain clearly experimental.

## Explicitly undecided

- final interpolation method;
- temporal-drift correction;
- adaptive guidance;
- sparse prediction;
- orientation compensation;
- cross-device normalization;
- wall-aware modelling;
- learned models;
- production confidence formula.
