# RQ-001 — Distinguishing Spatial Network Quality From Temporal Variation

## Research question

Can measurements collected using an ordinary Android phone provide enough evidence to infer that one physical position is genuinely better than another, rather than merely being measured during a temporarily better network state?

## Product relevance

A physical-location recommendation must offer a useful, repeatable benefit within a stated device, environment, endpoint/path and measurement profile. A convincing map is insufficient. See [product truth rules](../../PRODUCT.md) and [ADR-002](../../docs/adr/ADR-002-measurement-truth-and-uncertainty.md).

## Current hypothesis

In some ordinary indoor Wi-Fi conditions, repeated stationary measurements, balanced order, temporal reference visits and independent verification can reveal practically useful location differences despite temporal variation. This is unvalidated and does not assert feasibility for every room, device, metric or cellular connection.

## Null / competing explanations

- Positions have no useful difference; the apparent winner is noise or selection bias.
- Conditions change over time regardless of position.
- Orientation, body obstruction, device behavior, a network transition or the endpoint explains the apparent effect.
- Spatial effects exist but require more time, data or thermal cost to resolve than a useful product session allows.

## Variables OMANII may observe

The [engineering reference map](../README.md#canonical-ownership-and-engineering-links) identifies source interfaces, not proof of availability on every device.

| Group | Potential observations and limits |
| --- | --- |
| Probes | HTTP application RTT, bounded transfer observations, outcomes/bytes, endpoint health/context, profile, connection reuse and interval validity where exported. HTTP errors are not packet loss. |
| Network/radio | Transport, scoped network/AP/cell tokens, epochs, band/frequency, permitted radio values, availability reasons, source/receipt timing and defensible age. Freshness and independence may be unknown. RSSI/link speed are not internet throughput. |
| Position | Manual A/B/C labels; where usable, session-local x/y/z in metres, orientation, frame/segment, tracking and movement. AR pose is not ground truth or cross-session alignment. |
| Procedure/context | Visit order, stationary/moving intervals, device/API/build, permissions/capabilities, placement, coarse environment and operator notes. Thermal state, background traffic, congestion and interference are not assumed instrumented. Label manual observations and missing information. |

## Important confounders and potential controls

| Confounder | Candidate control / limitation |
| --- | --- |
| Network congestion changing during the scan | Stationary-only baseline, reference revisits and varied order; a single phone cannot sample all positions simultaneously. |
| Wi-Fi AP/band changes | Preserve observed epoch changes; missing identity is not proof of stability. |
| Cellular handovers | Retain observable serving-cell/technology changes; handover visibility may be incomplete. |
| Wi-Fi ↔ cellular transitions | Retain boundaries; do not pool before/after as a location effect. |
| Stale Android radio telemetry | Preserve unknown age and source timing; repeated polling is not independent sampling. |
| Server/probe variability | Consistent endpoint/profile/connection procedure; retain health and invalid outcomes. Some path variation remains unobserved. |
| Background traffic | Document controllable activity and known shared-network use without invasive monitoring. |
| Device thermal state | Available state or clearly manual notes, duration and recovery conditions; not assumed measured. |
| Movement effects | Separate travel from stationary windows; preserve intervals and pilot settling requirements. |
| AR pose drift | Repeatable physical markers and closure checks where available; preserve frame/segment boundaries. |
| Sparse sampling | Revisit positions, report coverage/uncertainty, and never paint unsampled space as observed. |
| Device antenna/orientation effects | Initially hold height, orientation, grip and body placement consistent; test variation separately. |
| Different devices behaving differently | Declare the initial device cohort and replicate before generalizing or calibrating across devices. |

## Potential controls

Use compatible network/endpoint/profile context, repeated stationary-only blocks and randomized or counterbalanced position visits. Keep initial placement consistent, preserve exceptions and reserve fresh verification visits.

Randomization/counterbalancing reduces order confounding without guaranteeing its removal. A→B→A reference checks cannot prove conditions at B matched either A visit. A future simultaneous reference device adds its own device/clock confounders.

## Evidence that would support the hypothesis

Useful location contrasts persist through changed order, reference revisits and fresh stationary verification. Effects exceed unresolved uncertainty and are not adequately explained by reference drift or placement changes. Replication defines the supported devices/environments; radio correlation alone is insufficient.

## Evidence that would weaken or falsify it

Winners reverse on held-out visits, follow time/order, disappear when orientation is controlled, or are no larger than temporal variability. Persistent failure across the intended scope or impractical resource demands undermines the product assumption. A flat room alone does not falsify all spatial effects; it should produce no winner.

## When to say “No clear better location found”

Use this outcome when no useful advantage survives valid independent verification, uncertainty is too large, temporal variation dominates, sampling is sparse/stale, or a comparison cannot be made validly.

Preserve the underlying distinction: a defensible tie differs from inconclusive evidence; broken context/tracking can make a comparison invalid. Missing support is not evidence of equality. The [protocol](../../protocol/README.md) and implementing task own exact result states and consumer wording; this question creates no production threshold.

## Open questions

What benefit matters for which activity/metric? How should conflicting metrics be interpreted? What cadence, window length, repetitions, settling and resource budget suffice? How dependent are observations? Which temporal patterns evade reference checks? What timing/pose uncertainty permits useful resolution? What metadata can reject unsafe comparisons? How far do results transfer across devices, environments, days and technologies?

Numerical settings and analysis choices: **TBD — requires literature review and/or pilot measurements.** Existing [product gates](../../PRODUCT.md#initial-success-conditions) remain engineering/product targets, not statistical justification.

## Dependencies on Wave 1 instrumentation

[TASK-002](../../docs/tasks/TASK-002-android-capability-radio-logger.md) supplies capability/radio observations and observable epochs. [TASK-003](../../docs/tasks/TASK-003-arcore-pose-scan-session-adapter.md) supplies optional local pose/tracking. [TASK-004](../../docs/tasks/TASK-004-controlled-endpoint-probe-replay.md) supplies bounded probes, accounting and replay.

Runtime wiring, interval association/export and physical validation are separate engineering work. Software checks do not establish this hypothesis or physical readiness.

## Candidate experiments

Begin with the [draft baseline repeatability study](../experiments/EXP-010-baseline-location-repeatability.md), including stationary-only temporal controls and A/B/C revisits. Follow-up candidates include orientation sensitivity, stronger temporal controls, cross-device replication and held-out recommendation verification. These follow-up candidates are not executed or assigned experiment IDs here.

## Current confidence

Low / unvalidated. No supporting research dataset or literature review is created by this scaffold. Instrumentation correctness and hypothesis confidence are separate.

## Status

OPEN — draft question. Next: review literature and refine the pilot once Wave 1 physical instrumentation is available.
