# OMANII research

Research exists to reduce uncertainty about what OMANII can measure, what people understand, and what is worth building. It should increasingly connect:

research → hypothesis → prototype → measurement → observation → decision → implementation

Engineering execution remains the product's critical path. This is evidence infrastructure, not a reason to stop building or an additional gate for ordinary engineering work. Keep records short enough to change a decision; do not create documents merely to fill folders.

## Start here

- **What are we trying to learn?** [RQ-001: spatial quality versus temporal variation](questions/RQ-001-spatial-vs-temporal.md).
- **What evidence do we have, and how trustworthy is it?** No research measurements, literature entries, or findings are added by this scaffold. Software checks and synthetic fixtures are not physical validation.
- **What do we test next?** Refine the [draft baseline repeatability experiment](experiments/EXP-010-baseline-location-repeatability.md) after Wave 1 physical instrumentation is validated.
- **What decision did it affect?** None yet. Use [findings](findings/README.md) to summarize evidence and [decisions](decisions/README.md) when evidence informs a real choice.

The stationary repeatability draft is registered as **EXP-010** in the [canonical experiment backlog](../docs/research/EXPERIMENTS.md#early-experiment-backlog). Existing EXP-001 through EXP-009 assignments are unchanged.

## Four research tracks

| Track | Question it answers |
| --- | --- |
| Technical research | Does the measurement methodology actually work, including separation of location effects from changing network conditions? |
| User research | Do people understand spatial quality, uncertainty, scores, recommendations, permissions, and later concepts such as Boost? |
| Product research | Do people care, act on recommendations, return, share, or pay? |
| Design research | Which visuals and metaphors explain invisible network behavior clearly without misleading people? |

A useful explanation is not evidence of accurate measurement. Reliable measurement is not evidence of demand. Product research does not authorize new tracking, billing, or scope.

## Use the smallest useful record

Start with a question and a falsifiable hypothesis. Link relevant [literature](literature/README.md), define an experiment, preserve [data provenance](datasets/README.md), and retain a reproducible [analysis](analyses/README.md). Summarize the finding and resulting decision, including uncertainty and what would change our mind. Negative and inconclusive results count.

Keep experiment-specific procedures with the experiment. Extract a [reusable protocol](protocols/README.md) only when another study needs it. [User research](user-research/README.md) has different evidence needs from physical network measurement.

## Canonical ownership and engineering links

This workspace organizes questions, drafts and evidence. It does not create a second product, architecture, protocol, or promotion specification.

- [PRODUCT.md](../PRODUCT.md), [ARCHITECTURE.md](../ARCHITECTURE.md), and [ADRs](../docs/adr/) remain authoritative.
- [docs/research/EXPERIMENTS.md](../docs/research/EXPERIMENTS.md) owns experimental method and promotion; [ADR-010](../docs/adr/ADR-010-experimental-algorithms-and-promotion.md) owns the production boundary.
- [protocol/README.md](../protocol/README.md) owns measurement semantics; [fixtures/](../fixtures/README.md) owns reusable regression/replay data; [docs/evidence/](../docs/evidence/README.md) owns engineering acceptance evidence. Link to these rather than rewriting or moving them.
- Research-informed changes still require applicable tasks, ADRs and Francis's approvals under [AGENTS.md](../AGENTS.md). A research decision alone does not authorize production changes.

These implementation references were inspected at integration snapshot `2f2db9c2249cdaa7ecd9571a8d47acfc0cfbc3d5`. Source-level building blocks do not establish completed runtime wiring or physical validation.

| Concept | Existing source and research consequence |
| --- | --- |
| Clocks and availability | [ObservationTiming](../android/app/src/main/java/com/omanii/app/model/ObservationTiming.kt) preserves receipt, source timestamp/domain/meaning and known measurement time. [Availability](../android/app/src/main/java/com/omanii/app/model/Availability.kt) preserves unavailable states. Never invent freshness. |
| Network epochs | [NetworkEpochTracker](../android/app/src/main/java/com/omanii/app/network/epoch/NetworkEpochTracker.kt) and [network observations](../android/app/src/main/java/com/omanii/app/network/model/NetworkObservation.kt) describe observable changes and scoped context. A stable token does not prove stable congestion or absence of an unobservable handover. |
| Sessions and intervals | [ProbeContextHooks](../android/app/src/main/java/com/omanii/app/session/ProbeContextHooks.kt) carries session, segment, epoch, frame, interval boundaries, continuity and movement availability. Matching interval endpoints alone does not establish continuity. |
| AR pose | [PoseValues](../android/app/src/main/java/com/omanii/app/pose/PoseValues.kt) and [canonical mapping](../android/app/src/main/java/com/omanii/app/pose/PoseCanonicalMapping.kt) preserve local geometry, tracking, frames and source timing. ARCore frame time is not assumed aligned to elapsed realtime; receipt-based association has uncertainty. |
| Probes | [ProbeModels](../android/app/src/main/java/com/omanii/app/probe/ProbeModels.kt) carries endpoint/profile, interval, outcomes, bytes, HTTP application RTT, connection semantics and validity/lower-bound flags. These are not a complete quality score. |
| Replay | [Network](../fixtures/network/README.md), [pose](../fixtures/pose/README.md) and [probe](../fixtures/probe/README.md) fixture guides distinguish synthetic checks from physical observations. Existing task evidence stays in its owning directory. |

No literature search, physical experiment, production change, or scientific validation is performed by this scaffold.
