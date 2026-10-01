# omanii System Architecture

Status: Canonical after Bootstrap Task 001 approval
Scope: Architecture and boundaries, not feature implementation

## 1. Architectural thesis

omanii is built as a truthful measurement system with a consumer experience layered above it.

The architecture optimizes for five properties:

1. **Measurement truth is centralized.** UI, copy and animations cannot redefine measurement meaning.
2. **Evidence survives implementation changes.** Raw sufficient observations and versions support replay and recalculation.
3. **Uncertainty is structural.** Missing, stale, interpolated, provisional and invalid are model states, not strings added at the end.
4. **The first implementation stays small.** One Android app, a minimal controlled measurement service, local-first persistence and language-neutral protocol contracts. No microservices platform.
5. **AI agents can work in parallel without silently rewriting shared truth.** Stable contracts, path ownership, task packets, fixtures and review gates provide control.

## 2. System overview

### Android client

Native Kotlin with Jetpack Compose. Android is the first production platform. ARCore is optional at runtime and supplies pose only for supported spatial workflows. Basic connectivity checks must remain useful without ARCore and without optional radio permissions.

The initial Android codebase should prefer one app module with clear packages over many Gradle modules. Split modules only when build/test boundaries demonstrably help parallel work or isolation.

Suggested package boundaries:

- `app`: composition, navigation, lifecycle wiring;
- `capability`: device/API/permission capability reporting;
- `network`: transport and radio context observations;
- `pose`: ARCore pose adapter and non-AR capability state;
- `probe`: active measurement client and byte accounting;
- `session`: foreground scan state machine and segment ownership;
- `field`: aggregation, baseline interpolation and confidence;
- `score`: pure deterministic score models;
- `diagnostics`: pure evidence-to-finding rules;
- `storage`: local persistence/export;
- `presentation`: Compose presentation, centralized copy keys and share rendering.

Do not create empty future packages merely to match this list.

### Pose subsystem

`PoseProvider` emits timestamped session-local pose observations with tracking state and coordinate-frame identity.

Initial implementation:

- ARCore where supported;
- local session origin;
- pose and orientation;
- height retained where useful;
- horizontal projection for analysis;
- tracking transitions explicitly recorded;
- sample placement paused while tracking is unusable;
- discontinuities create segment boundaries when continuity cannot be defended.

It does not promise centimeter precision, floor-plan reconstruction or cross-session alignment.

### Network context subsystem

`NetworkContextProvider` reports the path and radio context actually associated with a measurement.

Responsibilities:

- current transport and validation state;
- metering/VPN/private-DNS context where available and relevant;
- connected Wi-Fi context where permitted;
- cellular context where supported and permitted;
- observation source and timing;
- explicit unavailable/redacted/not-applicable states;
- network identity transition tokens scoped to the session;
- transport/AP/band/cell-context transition events.

It never converts platform unavailable sentinels into valid signal values. A callback arrival time is not automatically the time the modem measured the value.

### Active measurement subsystem

`ProbeClient` performs only bounded, declared measurement profiles against controlled endpoints.

Initial capabilities:

- tiny warm responsiveness probes;
- bounded transfer for gross throughput constraints;
- explicit timeout/cancellation;
- byte accounting;
- profile and endpoint identity/version;
- start/end monotonic timing;
- network context association;
- optional future sequenced UDP for valid packet-loss measurement.

A small HTTP transfer cannot claim peak access-link capacity. HTTP failures are not reported as exact packet loss.

### Scan session controller

`ScanSessionController` is the sole owner of active session scheduling. No pose, radio or probe adapter independently schedules a scan.

Canonical state flow:

Idle
→ capability/permission check
→ establishing tracking if needed
→ survey
→ paused/invalid segment OR candidate selection
→ stationary verification
→ result
→ optional save/share

Every active state supports cancellation and byte-limit termination.

The controller:

- owns session/segment IDs;
- associates observations by monotonic time;
- reacts to network changes;
- enforces probe budgets;
- prevents late traffic after cancellation;
- prevents active measurement while the app is no longer in an allowed foreground state;
- records why sessions pause/end;
- never silently carries an old network context into a new verification window.

### Field and confidence subsystem

`FieldBuilder`, `CandidateSelector`, `ConfidenceEstimator` and `VerificationEngine` remain pure/replayable where practical.

Baseline production candidate for validation:

- robust per-region aggregation, initially medians and spread;
- local IDW interpolation only within bounded supported neighborhoods;
- unknown outside supported regions;
- measured and interpolated support retained separately;
- candidate ranking from walk-time evidence;
- recommendation only after independent stationary comparison;
- confidence based on actual support, age, independent windows, tracking quality, repeat agreement, local sample distance and temporal-control drift.

Exact cell size, interpolation radius, smoothing, confidence formula and candidate policy are experimental parameters, not Task 001 constants.

### Score engine

Two separate interfaces consume shared sufficient statistics:

- `OmaniiScoreModel`
- `GamingScoreModel`

Both are deterministic and versioned. Neither UI nor backend code may duplicate formulas.

A score result includes value, model version, measurement profile, completeness, lower-bound/provisional status, endpoint context, confidence and limiting metrics.

The score engine accepts only inputs that satisfy the required profile. It must be possible to refuse a score.

### Diagnostics engine

`DiagnosticEngine` is a deterministic rule engine that turns evidence into bounded findings.

Each finding carries:

- stable finding code;
- evidence references/sufficient statistics;
- confidence category;
- consumer interpretation key;
- possible action key;
- limitations.

It may say “consistent with congestion” when supported. It must not declare the ISP, router, DNS or radio as the exact cause without evidence that distinguishes alternatives.

### Future Boost orchestration

No Boost behavior is implemented now.

The future boundary is `BoostAction` plus `BoostOrchestrator`:

eligibility
→ baseline measurement
→ mechanism/action
→ post-action measurement
→ verified effect/no effect/inconclusive
→ consumer copy key

Actions can be user-guided, an OS setting handoff, or a future network service. Attempting an action never implies it worked.

### Future personality/content layer

Consumer copy, reactions, achievements, notifications and localization must not live inside measurement logic.

A future `ReactionProvider` maps truthful domain events and product context to presentation keys/assets. The measurement domain emits facts such as:

- `ScanCompleted`;
- `ImprovementVerified`;
- `NoClearDifference`;
- `ResultInconclusive`;
- `ExceptionalScoreObserved`;
- `ExceptionalGamingScoreObserved`;
- `UnstableResult`.

Future achievement logic consumes validated results and cannot modify them.

### Local persistence

Local-first is the default.

Store:

- session metadata and summaries;
- schema/protocol/model versions;
- necessary observations/sufficient statistics;
- sanitized named-place references where the user chooses them;
- preferences separately.

Implementation direction after bootstrap:

- Room for structured session/history data;
- DataStore for preferences;
- append-only or batched raw observation storage for high-frequency development evidence;
- no main-thread database write for every pose frame;
- retain only necessary pose keyframes in normal saved product history.

Sensitive room coordinates, AP identities and usage history do not enter analytics by default. Automatic cloud backup of sensitive map/database data remains disabled until an explicitly approved backup design exists.

### Measurement backend

The first backend is a controlled measurement service, not a general cloud platform.

It should provide:

- endpoint identity/version and health;
- bounded responsiveness/transfer paths;
- signed or otherwise scoped bounded test grants before public scale;
- server-side byte/time limits in addition to client limits;
- cancellation-aware streaming;
- saturation/health signals so a server bottleneck invalidates tests rather than becoming the user's score.

A tiny control plane may choose a healthy endpoint. Same-scan comparisons use the same selected path unless a path failure forces an explicit invalidation/restart.

No arbitrary URL proxying. No global speed-test infrastructure in Task 004.

### Future website

`tryomanii.com` is a separate, lightweight future client of matched measurement semantics.

Boundary requirements:

- static/progressively enhanced shell;
- system fonts;
- near-zero first-load media;
- test begins only after user intent;
- browser limitations are explicit;
- no Android-specific radio/pose assumptions in cross-platform protocol definitions;
- same score only when the browser ran a truly comparable measurement profile.

The website is not implemented in Task 001.

### Future usage subsystem

Usage intelligence is a separate domain from spatial measurement truth. It may later support totals, budgets, per-app breakdowns where legitimate, alerts, widgets and recaps.

It must not contaminate spatial result schemas or require Accessibility as a shortcut.

### Future monetization subsystem

Billing, ads and entitlements are boundaries only.

Measurement must remain valid without monetization traffic. Future ad SDK activity must be isolated from measurement windows because network/CPU/thermal activity can bias results.

## 3. Core interfaces

These names are stable conceptual contracts. Exact Kotlin signatures are implementation work.

### `CapabilityRepository`

Returns current platform capabilities and permission states. No feature assumes ARCore/radio detail from device model alone.

### `PoseProvider`

Starts/stops a foreground pose stream and emits `PoseObservation` with monotonic timestamps, coordinates, orientation, tracking state, frame/segment identity and optional quality.

### `NetworkContextProvider`

Emits `NetworkContextObservation`, current path snapshots and transition events. It preserves unavailable/redacted state and measurement age semantics.

### `ProbeClient`

Runs a named `TestProfile` against an approved endpoint under a `ByteBudget`, supports cancellation, and emits `ProbeObservation`/summary records.

### `ByteBudget`

Accounts requested and actual transfer bytes across the session. The budget can refuse a probe before it begins and terminate one at the cap. It is not UI-only accounting.

### `ScanSessionController`

Owns the scan state machine, observation association, segmentation, cancellation, byte policy and result lifecycle.

### `FieldBuilder`

Builds a field representation from valid session evidence while preserving support type: measured, interpolated or unknown.

### `ConfidenceEstimator`

Produces confidence/support metadata from explicit factors. It cannot increase confidence merely because more render frames exist.

### `CandidateSelector`

Shortlists regions from survey evidence. It does not produce the final “better” claim.

### `VerificationEngine`

Compares stationary candidate/reference windows and returns `win`, `tie`, `inconclusive` or `invalid` with effect and confidence.

### `OmaniiScoreModel`

Consumes a complete-enough `ScoreInput` for its declared profile and returns `OmaniiScoreResult` or an invalid/incomplete state.

### `GamingScoreModel`

Consumes shared measurement primitives through its own model/version and returns `GamingScoreResult` or invalid/incomplete.

### `DiagnosticEngine`

Produces evidence-backed `DiagnosticFinding` records.

### Future boundaries

`ReactionProvider`, `AchievementEngine`, `BoostAction`, `UsageRepository`, `EntitlementRepository` exist only as architectural names until a real task needs them.

## 4. Canonical measurement pipeline

observation produced
→ assign monotonic receive time
→ preserve actual measurement timestamp if known
→ derive observation age only when defensible
→ associate pose interval/time
→ associate network context
→ segment on tracking/network discontinuities
→ local aggregation into independent-enough windows
→ field construction with support metadata
→ candidate selection
→ stationary verification against reference
→ confidence/validity
→ score/diagnostics where profile permits
→ truthful domain event
→ consumer presentation

Rules:

- monotonic time orders events and computes duration;
- wall clock exists for human-readable history, not causal ordering;
- observations with only receive time are marked as such;
- a long probe is an interval observation;
- if movement during a transfer exceeds the active profile's tolerance, do not assign it to one spatial point;
- network context transitions split/invalid comparisons;
- missing data is explicit all the way to presentation.

## 5. Spatial session model

A `ScanSession` contains one or more `SessionSegment`s.

### Session-local frame

- origin is local to the session;
- units are metres;
- pose retains x/y/z and orientation from the provider's declared coordinate frame;
- horizontal analysis uses a documented projection;
- height is retained where useful;
- no global geospatial coordinates are required for an indoor session.

### Segment boundary triggers

At minimum:

- unrecoverable tracking discontinuity;
- transport change;
- selected network/AP/band context change relevant to comparison;
- cellular context change that invalidates equivalence;
- explicit pause/resume when continuity is not preserved.

### Region states

Each field region carries support, not merely a number:

- `MEASURED`;
- `INTERPOLATED`;
- `UNKNOWN`.

Staleness and confidence are orthogonal metadata. A stale measured point does not become “unknown” silently, and an interpolated point never becomes measured because it was rendered repeatedly.

### Candidate locations

A candidate is a shortlist with reasons and support. It becomes a recommendation only if verification passes.

## 6. Measurement profiles and budgets

Profiles are versioned definitions. Initial conceptual profiles:

- `low_data_responsiveness`;
- `scan_survey`;
- `verification_low_data`;
- `verification_deep`;
- `deliberate_benchmark`;
- `gaming_assessment`.

Research hypotheses to validate, not frozen constants:

- survey: roughly 60-120 seconds, tiny responsiveness probes, active probe target <=1 MB;
- candidate verification: visible total cap, roughly 8-20 MB low-data or up to roughly 80 MB deep comparison;
- full benchmark: separate explicit action with a larger disclosed cap.

A profile contains its own permissible claims. A low-data profile may output a lower bound rather than pretend to measure gigabit peak capacity.

## 7. Protocol and versioning

Canonical semantics live in `protocol/README.md`.

Every exported/replayed session carries:

- `schema_version` - record shape/field semantics;
- `protocol_version` - measurement procedure;
- score model version(s) when a score is present;
- `test_profile`;
- endpoint identity/version/context.

Schema compatibility, measurement comparability and score comparability are different questions. A schema can remain readable while a protocol change makes two benchmark scores non-comparable.

Serialization is deliberately not frozen in Task 001. Development export may use JSONL because it is inspectable, but conceptual contracts own meaning.

## 8. Consumer experience architecture

### Centralized content

UI requests content by stable keys/domain state. Avoid personality strings scattered through screens.

Core operation must not require remote copy. Remote configuration, if ever introduced, cannot redefine measurement truth.

### Reveal events

Measurement/domain code emits facts. Presentation decides animation/haptics/copy.

Examples:

- scan completed;
- verified significant improvement;
- no clear difference;
- unstable/time-varying conditions;
- exceptional valid score;
- result invalidated by network transition.

### Share result model

A share result is a sanitized projection, not a screenshot of internal state.

Default exclusions:

- room label;
- coordinates;
- SSID/BSSID/cell identity;
- raw route;
- sensitive per-app usage;
- exact location.

The user may deliberately include additional context in a future approved design.

## 9. Permission architecture

Permissions are requested at feature entry, not first launch.

Stages:

1. basic internet check - minimum network access/state only;
2. spatial scan - camera when initiated;
3. richer radio context - only applicable nearby/location permissions for chosen APIs;
4. usage tracking - separate OS Usage Access flow;
5. notifications - when the user creates an alert.

Forbidden in V1 without a new approved ADR:

- Accessibility;
- background location;
- microphone;
- contacts;
- IMEI/device identifiers;
- VPN;
- `QUERY_ALL_PACKAGES`;
- broad storage;
- screen capture;
- broad sensitive inventory access.

Denial preserves a simpler product path where technically possible.

## 10. Privacy and security defaults

- Camera imagery/depth: process for tracking, do not retain/upload by default.
- Room coordinates/labels/AP identity: local by default; excluded from analytics.
- Exported research: use session-scoped tokens instead of stable AP/cell identifiers where possible.
- Usage history: local and isolated from advertising SDKs.
- Product analytics: minimal taxonomy, no map/SSID/BSSID/app inventory.
- Measurement backend: TLS, scoped credentials, bounded grants, no arbitrary proxy.
- Crash logs: redact radio identifiers, coordinates and sensitive payloads.
- Signing keys/secrets: never placed in repository or agent prompts.
- Raw research captures: explicit consent, bounded retention and deletion.

A hash of a BSSID is still a linkable location proxy; hashing is not automatic anonymization.

## 11. Dependencies

Dependencies are a cost, privacy and supply-chain decision. A new runtime dependency must document:

- purpose;
- approximate size/native binaries;
- permissions/data access;
- maintenance/activity;
- privacy implications;
- alternatives;
- why platform APIs are insufficient.

Initial direction from research:

- Kotlin/Compose;
- ARCore where supported;
- Kotlin coroutines/Flow;
- maintained conventional HTTP client such as OkHttp when Task 004 is implemented;
- Room/DataStore when persistence work starts.

Do not add Cronet/QUIC, large rendering engines, DI frameworks, analytics/ad SDKs or billing libraries before a task demonstrates the need.

## 12. R&D isolation

Experimental algorithms live behind production contracts and are never selected merely because they are more sophisticated.

Each experiment declares baseline, data, metric, failure condition, replay support and physical validation. Promotion requires evidence, independent review, versioning and Francis approval.

Candidate research directions:

- time-versus-space separation;
- uncertainty-aware interpolation;
- adaptive next-sample guidance;
- sparse-scan prediction;
- orientation compensation;
- cross-device normalization;
- low-data measurement;
- experienced-performance fusion;
- bottleneck classification.

Production starts with the simplest baseline that can fail visibly.

## 13. Physical validation boundary

Emulator, unit tests and AI review cannot validate:

- AR tracking quality;
- radio freshness/behavior;
- real spatial differences;
- orientation/body effects;
- repeatability;
- byte behavior on real networks;
- battery/thermal impact;
- real improvement after an action.

These require physical-device evidence stored with the task.

## 14. Future boundaries, not implementations

The architecture leaves room for iOS, website, usage intelligence, monetization, achievements, mascot/personality, Boost, monitoring nodes, professional workflows and router integrations.

None of these may pull their assumptions backward into the V1 measurement contract before their own evidence/task exists.
