# omanii Protocol Contracts

Status: Canonical conceptual contract. Serialization is deliberately not frozen in Bootstrap Task 001.

## 1. Version axes

Every persisted/exported measurement carries enough context to determine meaning.

- `schema_version`: record shape and field semantics.
- `protocol_version`: procedure used to collect/aggregate the measurement.
- `test_profile`: named profile controlling schedule, limits and permissible claims.
- `score_version`: model version for a score, when present.
- `endpoint_context`: endpoint identity/region/service/protocol version needed to interpret path measurements.

A readable schema does not imply benchmark comparability. Protocol/profile/endpoint changes can make two scores non-comparable even when the same fields exist.

## 2. Time contract

Use monotonic time for ordering/durations inside a session. Wall-clock time is optional display/history metadata.

Every observation distinguishes:

- `received_at_monotonic`: when the app received/read it;
- `measurement_at_monotonic`: when the underlying measurement occurred, if the platform exposes it;
- `age_known`: whether age is defensible;
- `source`: API/provider/probe source.

Never invent a measurement timestamp for cached platform state.

## 3. Availability contract

Missing numeric values are nullable and accompanied, where useful, by an availability reason such as:

- `available`;
- `not_supported`;
- `permission_denied`;
- `redacted`;
- `not_applicable`;
- `temporarily_unavailable`;
- `unknown`.

Do not encode platform sentinel values as normal measurements.

## 4. CapabilityReport

Conceptual fields:

- `report_id`;
- `created_at_monotonic` and optional wall time;
- `device_model` and API level;
- app/build version;
- ARCore availability/runtime state;
- supported pose/radio/usage observations;
- permission state relevant to requested features;
- supported test profiles;
- capability flags;
- limitations/notes needed for replay interpretation.

The capability report is descriptive. It does not grant permission or assert runtime success.

## 5. ScanSession

Conceptual fields:

- `session_id`;
- `schema_version`;
- `protocol_version`;
- `test_profile`;
- monotonic start/end;
- optional wall start/end;
- capability-report reference;
- selected endpoint context;
- selected byte budget;
- transport/path summary;
- segment IDs;
- status;
- cancellation/stop reason;
- final result state;
- optional consent/export scope for development evidence.

Final result state is one of a bounded set such as `verified_win`, `tie`, `inconclusive`, `invalid`, `cancelled`, `failed`, or `completed_without_recommendation` as the implementing task defines precisely.

## 6. SessionSegment

A segment is a time range during which the comparison context is sufficiently continuous.

Fields:

- `segment_id`;
- start/end monotonic time;
- reason started;
- reason ended;
- coordinate-frame reference;
- network-context epoch/path token;
- tracking continuity state;
- validity flags.

Transitions that can create a new segment include tracking loss/discontinuity, transport change, AP/band/network change, relevant cellular-context change and explicit resume after an invalid pause.

## 7. PoseObservation

Fields:

- monotonic timestamp;
- `x_m`, `y_m`, `z_m`;
- orientation (document representation in implementation);
- tracking state;
- coordinate-frame ID;
- segment ID;
- optional provider quality/confidence;
- optional keyframe ID.

Coordinates are session-local metres, not geographic coordinates. Horizontal projection is derived, not a replacement for the raw session-local pose.

## 8. NetworkContextObservation

Fields:

- receive monotonic timestamp;
- actual measurement timestamp if known;
- age-known flag;
- source;
- segment ID;
- transport;
- validation/metered state where relevant;
- VPN/private-DNS context where available and relevant;
- session-scoped network/path transition token;
- Wi-Fi frequency/band and radio metrics where permitted;
- cellular technology/signal fields where supported;
- active data-subscription label/token where needed;
- explicit availability/null semantics for every optional metric.

Stable raw SSID/BSSID/cell identity is not part of ordinary shared analytics. Development export uses session-scoped tokens unless explicit evidence work requires more and has consent.

## 9. ProbeObservation

Fields:

- `probe_id` and sequence;
- probe type;
- test profile;
- endpoint identity/version;
- start/end monotonic times;
- requested bytes and actual bytes;
- timing outputs appropriate to the probe;
- timeout/error/cancellation state;
- network-context/segment at start and end;
- movement span/distance where available;
- concurrent-load state;
- warm/cold connection semantics where relevant.

If a probe crosses a context transition, its spatial/comparative validity is explicitly downgraded or invalidated.

A multi-second transfer is an interval measurement. It is never attached only to the completion pose.

## 10. TestProfile

Conceptual profiles:

- `low_data_responsiveness`;
- `scan_survey`;
- `verification_low_data`;
- `verification_deep`;
- `deliberate_benchmark`;
- `gaming_assessment`.

A profile defines:

- required/optional probe classes;
- duration/cadence constraints;
- byte cap policy;
- movement/stationary requirements;
- completeness requirements;
- permissible claims;
- invalidation rules.

Exact budgets/cadences remain experimental until tested.

## 11. ByteBudget

Fields/semantics:

- budget ID;
- total allowed bytes;
- consumed bytes;
- reserved/in-flight bytes if implementation needs it;
- reason/profile;
- exhausted state;
- cancellation state.

Server and client enforce limits independently. “Stop” means no late measurement transfer after cancellation beyond unavoidable transport teardown that is explicitly measured and bounded.

## 12. CandidateRegion

Fields:

- region ID;
- session/segment ID;
- spatial bounds/support representation;
- measured support count/windows;
- interpolated support metadata;
- age/temporal-control context;
- confidence/support features;
- reason shortlisted;
- rank within this survey.

A candidate is not yet a recommendation.

## 13. StationaryVerification

Fields:

- verification ID;
- reference region/spot;
- candidate region/spot;
- stationary window definitions;
- measurement summaries;
- effect/difference by relevant metric;
- absolute and relative difference where meaningful;
- confidence/support;
- endpoint/path context;
- outcome: `win`, `tie`, `inconclusive`, or `invalid`;
- invalidation/limitation reasons.

Selection samples are not reused as independent verification evidence.

## 14. ScoreInput

Shared sufficient statistics may include:

- valid download/upload throughput windows;
- idle RTT distribution;
- loaded RTT distribution by load direction;
- fixed jitter statistic plus retained distribution quantiles;
- valid sequenced-probe loss if available;
- confirmed failure-time fraction;
- stability/throughput variability summaries;
- measurement/profile/endpoint context;
- completeness and data-quality flags.

A model declares which fields are required.

## 15. OmaniiScoreResult

Fields:

- `value` when valid;
- score version;
- measurement profile;
- completeness;
- lower-bound/provisional flag;
- confidence;
- limiting metrics;
- endpoint context;
- invalid/incomplete reason when no value is issued.

## 16. GamingScoreResult

Same outer philosophy as OmaniiScoreResult, but with its own:

- score version;
- declared required inputs;
- weighting/model semantics;
- validity/completeness rules;
- endpoint/game-scope context.

No implication of exact game-server performance without relevant endpoints.

## 17. DiagnosticFinding

Fields:

- stable finding code;
- evidence references/sufficient statistics;
- confidence category;
- consumer interpretation key;
- possible action key;
- limitations;
- optional conflicting evidence.

## 18. ShareResult

A first-class sanitized output:

- result type;
- score/verification summary allowed for sharing;
- date/profile/endpoint-scope text;
- human-readable implication key;
- redaction policy/version.

Excluded by default: room label, coordinates/route, SSID/BSSID/cell identity, exact location and sensitive app-usage history.

## 19. Future BoostAction

Concept only:

- action ID;
- eligibility evidence;
- mechanism;
- expected effect category, not promised magnitude;
- pre-action measurement reference;
- execution type: user-guided / OS setting handoff / future network service;
- post-action verification reference;
- result: improved / no improvement / inconclusive / invalid;
- disclaimer/copy key.

No Boost action exists merely because this contract exists.
