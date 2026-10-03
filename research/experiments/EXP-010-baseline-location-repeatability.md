# EXP-010 — Baseline Location Repeatability

**Protocol maturity:** DRAFT — not executed.

**Experiment status:** PROPOSED under the [canonical experiment contract](../../docs/research/EXPERIMENTS.md).

**Question:** [RQ-001](../questions/RQ-001-spatial-vs-temporal.md).

**Owner / reviewer:** Unassigned; identify before execution.

**Identifier:** EXP-010, registered in the [canonical experiment backlog](../../docs/research/EXPERIMENTS.md#early-experiment-backlog).

## Purpose, hypothesis and question

Establish whether repeated measurements at fixed physical positions are stable enough to justify later spatial recommendation experiments.

Hypothesis: within a declared device/environment and compatible network/endpoint context, some practically useful between-position differences persist through repeat visits and order changes beyond within-position temporal variation.

Question: do A/B/C observations depend reproducibly on position, or are apparent differences better explained by time, order, placement or measurement context?

**Baseline comparator:** stationary-only repeated measurements at a reference position, plus a temporal/no-useful-location-effect explanation of interleaved observations. The estimator remains unresolved.

**Algorithm/change under test:** measurement design and repeatability only; no interpolation, score formula, drift correction or production recommendation algorithm.

**Expected improvement:** knowledge of variability and achievable discrimination; no promised numerical gain.

**Scope:** initially a declared Android device and indoor Wi-Fi environment, without cross-device or cellular performance claims.

## Required instrumentation and prerequisites

- Physically validated TASK-002/003/004 instrumentation for the runtime capture path being used: start/stop/cancellation, byte accounting, timing, context boundaries and inspectable exports. Reference the owning [engineering evidence](../../docs/evidence/README.md); do not modify it.
- Controlled, versioned endpoint and a declared existing bounded probe profile, with health/context observability and documented gaps.
- Session-scoped network records and complete probe intervals, including intervening boundaries and unavailable states. Pause this comparison study if adequate association is unavailable.
- Repeatable physical spot markers and phone placement. AR is optional for a manual-position pilot. Automated spatial attribution requires physically validated pose and documented association limits.
- Applicable consent/export scope, data custodian, local raw-data location, access restrictions and bounded retention/deletion arrangements. This draft grants no new collection permissions.
- Versioned run plan declaring exploratory versus confirmatory use and resolving the parameters below. Freeze confirmatory rules before evaluation data collection.

See the [engineering reference map](../README.md#canonical-ownership-and-engineering-links). Source presence is not proof these prerequisites are satisfied. Runtime wiring and physical validation remain separate work.

## Candidate environment and positions A/B/C

Choose an ordinary indoor Wi-Fi setting with repeatable access and a known test path. Describe it coarsely without exact addresses or private interior imagery. Record known activity and uncontrollable conditions rather than claiming perfect control.

Choose spots before inspecting their network results: A is a reference marker; B and C are distinct reachable markers representing plausible user positions. None is presumed better. Record local placement, height, orientation, grip/support and body position. Spacing/distances: **TBD — requires literature review and/or pilot measurements.**

Hold device, endpoint, procedure, placement and observable network context consistent within a comparison block. Retain transitions and separate incompatible observations. Do not selectively change AP, band, device or endpoint to manufacture a contrast.

## Repeated stationary measurements and visit order

1. Record capability, version, budget, endpoint and initial context; verify inspectable exports and note instrument/operator limitations.
2. Collect repeated stationary observations at A without moving over a duration comparable to the visiting procedure. This characterizes temporal variation without location changes.
3. Revisit A/B/C in a recorded randomized or counterbalanced order. Preserve planned/actual order, method/seed where used and deviations. Multiple observations within a visit are not automatically independent replicates.
4. Include A→B→A and A→C→A reference checks, with alternative orders across blocks so positions do not always occupy the same early/late slot. These are examples, not fixed schedules.
5. Mark travel, arrival, settling and stationary intervals. Use defined stationary windows for the primary comparison. Preserve and flag any transfer overlapping movement; never attach it solely to its completion pose.
6. Reproduce placement on return and record discrepancies. Where AR is used, retain tracking/frame changes and return-to-marker geometry. Good closure alone does not prove all intermediate poses were accurate.
7. Reserve fresh visits or separate runs to check an apparent contrast after selection. Data used to tune metrics, thresholds or candidate spots cannot be independent confirmation.
8. Stop within the declared limits, retain partial/failed runs, and reconcile procedure notes with export completeness before analysis.

Adding capture/scheduling behavior requires its own engineering task; this document does not implement it.

## Temporal-control considerations

A single phone visits positions sequentially. Interleaved A readings can expose drift but cannot measure conditions at the instant B was sampled. Do not assume linear drift or subtract an interpolated reference as measured truth.

Stationary-only blocks, reference revisits and balanced order examine time effects but cannot remove every fast fluctuation or slow trend. A stable observable epoch does not prove stable congestion. Keep incompatible epochs, endpoints and profiles separate.

A simultaneous second reference device is a possible follow-up, not a prerequisite or an implemented capability; it introduces device and clock-alignment uncertainties.

## Parameters to resolve

Every numerical setting and analysis choice below is **TBD — requires literature review and/or pilot measurements**:

| Choice | Required justification |
| --- | --- |
| Devices, environments, runs, blocks and visit counts | Feasibility, variance, dependence and intended inference scope. |
| Stationary duration, cadence, settling and recovery | Freshness, autocorrelation, movement carryover and thermal/load behavior. |
| Primary metric, summaries and useful-effect margin | User outcome, supported measurement semantics and pilot behavior; do not choose the best-looking metric after evaluation. |
| Position spacing and placement tolerance | Physical repeatability and pose/association uncertainty where used. |
| Time/byte limits and thermal stops | Existing enforced limits and study feasibility; no increase to production limits is authorized. |
| Hold-out/replication design, precision target, estimator/test and thresholds | Defensible analysis assumptions and evaluation separate from tuning. |
| Stationarity, freshness, missingness and exclusion rules | Instrument capability and auditable rules fixed before confirmatory analysis. |

The [product gates](../../PRODUCT.md#initial-success-conditions) are not replaced or automatically adopted as statistical thresholds. Implementation defaults are not scientific justification for sample counts or timing.

## Metrics and metadata to record

| Group | Record with units, origin and availability |
| --- | --- |
| Primary/secondary metrics | Primary metric remains TBD. Candidates: within-position variability, between-position contrasts, revisit agreement and held-out reversal of an apparent winner. Label secondary/exploratory measures. |
| Probes | ID/sequence, type/profile/version, endpoint identity/region/service/protocol, start/end/receipt times, supported HTTP application RTT, requested/actual/charged/unconfirmed bytes, outcome/reason, endpoint health/validation, connection semantics, concurrent-load and lower-bound flags where exported. Include failed attempts. |
| Network state | Scoped tokens, epochs and transition times/reasons, transport, observable AP/band/frequency or cellular context, radio availability/source timing/receipt, unknown age and independence. Cellular context does not establish cellular validation. |
| Sessions / pose / location | Session/segment/frame, complete interval boundaries/continuity, movement availability, manual spot/visit/order labels; local pose/orientation/tracking where available. No assumed source-time alignment or cross-session geometry. |
| Procedure / environment | Model/API/build, capabilities/permissions, run-plan revision, endpoint/profile configuration, orientation/height/body placement, coarse surroundings, known background activity and available thermal/battery observations. Label manual notes and unmeasured factors. |

Derive transfer rates only when bytes and the corresponding measured interval support the calculation; retain partial/lower-bound meaning. HTTP application RTT is not raw radio latency. HTTP failures are not packet loss. This study produces no complete OMANII or gaming score.

These desired observations are not a new schema or a claim that every current export supplies them. Record gaps and analytical impact; request instrumentation through its owner.

## Possible stopping rules

Stop for user cancellation, enforced time/byte exhaustion, unsafe device conditions, missing valid context or endpoint failure. A network/tracking discontinuity ends a compatible comparison block. Preserve it and explicitly decide whether another block is feasible; do not silently combine across the break.

Predeclare maximum collection effort and insufficient-data rules. Values: **TBD — requires literature review and/or pilot measurements.** Do not stop because an attractive winner or convenient significance level appears. Adaptive stopping needs prior justification; otherwise label the analysis exploratory.

## Possible statistical analyses and uncertainty

Begin with chronological plots by spot, visit/order, epoch and outcome, showing controls, failures and gaps. Describe within-position variation and between-position contrasts in original units.

Candidates include within-block paired contrasts, repeatability/rank summaries, and models separating position from time/block effects. Any resampling must respect dependence, potentially at visit/block level; packets, polls and render frames are not automatically independent replicates. Estimator, test, interval method and assumptions: **TBD — requires literature review and/or pilot measurements.**

Report effects and justified uncertainty intervals where possible, observations/visits/runs, exclusions/missingness and sensitivity to alternate assumptions. If uncertainty cannot be estimated reliably, say so. Account for candidate/metric selection and multiple comparisons rather than declaring the largest noisy contrast a discovery. Statistical significance alone does not show product usefulness.

Repeatability informs later recommendation work; it does not alone establish causal location effects or broad generalization. Follow [analysis guidance](../analyses/README.md).

## Failure / inconclusive conditions and threats to validity

- Insufficient valid or independent-enough evidence; time and location remain entangled.
- Reference drift, missing telemetry or unknown background load can explain the contrast.
- Incompatible context, movement, timing uncertainty or pose drift prevents attribution.
- Selective failures/exclusions change the conclusion.
- Winners reverse on fresh visits, practical usefulness is unresolved, or resource cost is excessive.
- A narrow room/device/day sample cannot support general claims.

An adequately measured flat environment may support no useful difference there. A noisy/invalid run cannot establish equality. Preserve the distinction and all attempted-run counts.

## Data management and replay compatibility

Follow [dataset guidance](../datasets/README.md). Keep raw exports immutable, retaining clock domains/units, scoped identities and versions. Separate notes and derived joins/filters/labels; record checksums, code/version and reasons. Preserve failed/inconclusive runs subject to approved retention/deletion.

Use existing task-owned formats/parsers. Preserve unavailable values and interval boundary histories. Do not invent a consolidated schema or guess joins. Sanitized reusable regression subsets may later enter [fixtures](../../fixtures/README.md) through engineering ownership. Replay success is not new physical evidence.

Record byte use, duration and available thermal observations. Minimize sensitive content: no routine camera imagery, raw wireless identities, exact addresses or unrelated usage. Resolve storage/access/retention before collection; no privacy default changes here.

## Interpretation and follow-up

A follow-up is justified if compatible repeat visits show a potentially useful contrast that persists under changed order and fresh checks, or if the pilot identifies a feasible stronger control. A pilot may justify refining instrumentation/procedure without validating the hypothesis.

Revisit the product assumption if controlled repetitions across the intended scope show temporal variation or placement/device effects dominating, repeated winner reversal, or unacceptable time/data/thermal cost. Possible responses are narrower supported conditions, coarser recommendations, or emphasis on the existing manual-comparison/diagnostic workflow. Product changes still require canonical approval.

**Results with uncertainty:** None; no data collected.

**Outcome:** Not evaluated.

**Reviewer verdict:** Not requested; review the refined plan before execution and evidence before claims.

**Promotion / ADR / version consequences:** No production promotion proposed. Later algorithm, semantics, profile, scope or claim changes must follow the [canonical promotion gate](../../docs/research/EXPERIMENTS.md).
