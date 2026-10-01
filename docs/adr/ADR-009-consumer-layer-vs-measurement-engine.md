# ADR-009 - Consumer layer versus measurement engine

**Status:** Accepted

## Context

omanii needs personality, visual drama, shareability and simple language to become a consumer product. The same product can become deceptive if presentation logic invents certainty, edits numbers, hides invalid states or hardcodes measurement interpretations in screens.

## Decision

Measurement/domain systems own facts. Presentation owns expression.

The measurement/domain layer emits typed results/events with enough context for honest simplification, including confidence, validity, completeness, limiting metrics and support.

Consumer UI may choose:

- copy;
- animation;
- haptics;
- mascot/personality response;
- visual hierarchy;
- share prompt.

It may not:

- alter score/verification values;
- convert unknown/tie/inconclusive into a win;
- create a measurement that did not occur;
- suppress material validity limitations;
- claim Boost/improvement from an attempted action without post-action evidence.

Consumer-facing copy is centralized behind stable keys/providers rather than scattered through measurement/UI code. Core operation cannot depend on remote copy.

## Consequences

- The product can become more playful without risking formula drift.
- Localization/personality changes avoid touching networking code.
- Domain events need careful design.
- UI snapshots alone are insufficient acceptance for measurement claims.

## Alternatives considered

### Let each screen interpret metrics directly

Rejected because it duplicates semantics and creates contradictory claims.

### Keep the product purely technical

Rejected because correctness alone does not produce an approachable consumer experience.

## Explicitly undecided

- mascot;
- final visual identity/colors;
- final copy style library;
- achievement catalog;
- final animation/haptic language;
- remote content management.
