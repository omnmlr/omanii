# Score Architecture

Canonical scope: model boundaries, versioning and an experimental starting hypothesis. No production score implementation exists in Bootstrap Task 001.

## 1. Shared principles

Omanii Score and OG Score are separate versioned models over shared measurement primitives. They are not UI calculations and are not interchangeable.

A score is valid only when the named measurement profile supplies the model's required evidence. Missing inputs do not become zero or “good.” A low-data test may produce a lower bound, provisional result or no score.

Every result carries:

- value, if valid;
- model version;
- measurement/protocol/profile version;
- endpoint/path context;
- completeness;
- lower-bound/provisional flag;
- confidence/data-quality summary;
- limiting metrics;
- invalid/incomplete reason when applicable.

Changing coefficients, guardrails, required inputs or transforms creates a new score version and requires Francis approval plus updated golden fixtures.

## 2. Omanii Score intent

Omanii Score summarizes general connection usability. It is benchmark-style, not a percentage.

- 100 = documented reference-quality profile, not perfection.
- >100 is expected for materially stronger connections.
- High throughput cannot erase catastrophic responsiveness, loss or instability.
- A score multiple must not be marketed as an equal multiple of subjective quality.
- Walk-time radio-only cells do not receive complete benchmark scores.

## 3. Experimental model `os-exp-0.1`

This is copied from the research as a starting hypothesis to test. It is **not** an approved production model.

For a fixed valid stationary benchmark profile to a recorded endpoint class:

- `D`: sustained download Mbps from valid windows;
- `U`: sustained upload Mbps from valid windows;
- `L`: median idle RTT ms;
- `LL`: larger p90 loaded RTT from separate download/upload load tests, ms;
- `J`: fixed idle RTT-variation statistic in ms;
- `p`: observed sequenced-probe loss fraction where the protocol validly measures it;
- `f`: fraction of scheduled probe time represented by confirmed failure intervals in this test.

Experimental formula:

```text
S_raw = 100 × (D/25)^0.35 × (U/5)^0.20
            × (40/(20+L))^0.20
            × (70/(30+LL))^0.15
            × (10/(5+J))^0.10
            × exp(-12p - 20f)
```

Experimental reference point:

- 25 Mbps down;
- 5 Mbps up;
- 20 ms idle RTT;
- 40 ms loaded p90 RTT;
- 5 ms jitter statistic;
- no observed sequenced-probe loss;
- no confirmed failure intervals.

This yields 100 by construction.

Research-proposed conservative experimental guardrail: cap at 80 if observed loss >=3%, confirmed failure time >=1%, or loaded p90 RTT >=500 ms. This is a discontinuous hypothesis intended to stop huge bandwidth from disguising instability. It must be replaced or retained only after calibration evidence.

### Golden synthetic expectations for the experiment

These are initial independent examples from the research and belong in future score fixtures when score code is created:

| Case | D/U Mbps | Idle/loaded RTT ms | Jitter; loss/failure | Expected experimental score |
|---|---:|---:|---|---:|
| Very poor | 2 / 0.5 | 100 / 300 | 30 ms; 2% / 2% | ~8 |
| Modest less responsive | 10 / 2 | 50 / 100 | 15 ms; 0.5% / 0% | ~43 |
| Reference | 25 / 5 | 20 / 40 | 5 ms; 0% / 0% | 100 |
| Strong | 100 / 20 | 15 / 30 | 4 ms; 0% / 0% | ~228 |
| Exceptional symmetric | 1000 / 1000 | 5 / 10 | 1 ms; 0% / 0% | ~1319 |
| Huge capacity, unstable | 1000 / 1000 | 150 / 500 | 40 ms; 3% / 1% | 80 after experimental cap |
| Modest capacity, very responsive | 10 / 5 | 5 / 10 | 1 ms; 0% / 0% | ~91 |

No implementation task may silently “tune” these to make a demo look better.

## 4. Completeness and lower bounds

The score engine must distinguish at least:

- `COMPLETE`: all required measurements valid for this profile/model;
- `PROVISIONAL`: enough evidence for a provisional value but validation rule says not fully comparable;
- `LOWER_BOUND`: a bounded transfer established at least a level but could not measure peak capability;
- `INCOMPLETE`: required evidence missing; no complete score;
- `INVALID`: context/procedure invalidated the measurement.

Exact enum names may be refined by the implementing task, but these semantics may not be removed.

A bounded 80 MB transfer cannot prove sustained 1 Gbps over several seconds. In such cases, throughput may be a lower bound and the complete benchmark may be withheld depending on model requirements.

## 5. Endpoint/profile comparability

Latency and throughput include endpoint/path effects. Score history therefore records endpoint context.

Comparisons are valid only under declared compatibility rules. A regional endpoint result is not “the internet” and is not exact game-server ping.

## 6. OG Score intent

OG Score measures gaming suitability and has its own model family.

It should weight/shape evidence so that gaming is dominated by:

- idle latency;
- latency consistency;
- jitter;
- valid loss;
- loaded responsiveness;
- short-term instability/failure;

with rapidly diminishing value from throughput once the profile has enough bandwidth for the assessed gaming category.

No final formula, coefficients or reference thresholds are accepted in Task 001. The first OG model must arrive as a separate evidence-backed task with golden cases and endpoint-scope wording.

## 7. Limiting metrics

Score results expose the main factors limiting the current result so diagnostics/presentation can say useful things without reverse-engineering the formula in UI.

Examples of limiting categories, only when supported:

- insufficient upload capacity for profile;
- high idle latency;
- poor loaded responsiveness;
- instability/failure intervals;
- valid observed loss;
- incomplete/lower-bound throughput;
- endpoint/context limitation.

## 8. UI prohibition

UI may format/round a returned score and select presentation from the typed result. UI must not:

- reweight inputs;
- apply hidden bonuses/penalties;
- fill missing metrics;
- recalculate historical scores;
- change limiting metrics;
- infer a complete score from provisional scan color.
