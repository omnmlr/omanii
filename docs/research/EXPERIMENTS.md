# R&D Experiment Contract

Experimental algorithms are isolated from production truth. “Interesting” is not a promotion criterion.

## Experiment record

Every experiment defines:

- Experiment ID and title
- Status: `PROPOSED`, `RUNNING`, `REJECTED`, `CONTINUE`, `PROMOTE_CANDIDATE`, `PROMOTED`
- Hypothesis
- Production/baseline comparator
- Algorithm/change under test
- Data required
- Primary and secondary metrics
- Expected improvement
- Failure condition
- Device/environment scope
- Replay compatibility
- Physical validation procedure
- Byte/thermal/privacy impact
- Results with uncertainty
- Known failure modes
- Reviewer
- Outcome
- Required ADR/version change if promoted

## Promotion gate

An experiment may become a production candidate only if:

1. it beats the declared baseline on the declared metric using data not used to tune/select it;
2. physical-device evidence agrees with replay where physical behavior matters;
3. it does not achieve the gain by hiding unknown space, consuming unreasonable data, or worsening false confident recommendations;
4. limitations and supported cohort are documented;
5. a different reviewer approves the evidence;
6. protocol/model/version consequences are explicit;
7. Francis approves promotion;
8. the relevant ADR/contract is updated before production selection.

No experiment can silently become the default through a configuration toggle.

## Early experiment backlog

- EXP-001: IDW baseline characterization
- EXP-002: uncertainty-aware interpolation vs IDW
- EXP-003: temporal-drift normalization using interleaved reference/control windows
- EXP-004: adaptive next-sample recommendation / uncertainty reduction
- EXP-005: orientation/body-effect compensation
- EXP-006: sparse-scan field prediction
- EXP-007: cross-device normalization/calibration
- EXP-008: low-data experienced-performance fusion
- EXP-009: bottleneck classification confidence calibration
- EXP-010: [Baseline location repeatability](../../research/experiments/EXP-010-baseline-location-repeatability.md) — PROPOSED; draft protocol, not executed

## Baseline principle

Prefer the simplest baseline that exposes its uncertainty. Gaussian processes, kriging or learned models are not upgrades unless held-out physical evidence shows a practical improvement over robust local aggregation/IDW.
