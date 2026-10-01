# omanii Product Contract

Status: Canonical after Bootstrap Task 001 approval
Owner: Francis
Last bootstrap revision: 2026-10-01

## Product definition

omanii is a global, Android-first connectivity companion that helps ordinary people understand how usable their current internet connection is, find and verify a better place to use it when a spatial difference exists, understand likely bottlenecks without pretending certainty, assess gaming suitability, and later track data use and meaningful changes over time.

Nigeria is the first founder-accessible validation environment, not a product boundary. The product is carrier-agnostic and must work with ordinary access paths including Wi-Fi, mobile data, shared Wi-Fi, home routers, fiber delivered through Wi-Fi, Starlink delivered through Wi-Fi, lodge/apartment Wi-Fi, public hotspots, and similar internet connections.

The product philosophy is:

> Technical truth underneath. Consumer simplicity and delight on top.

The first public promise is not “a perfect room heatmap” and not “we boost your ISP.” It is:

> Find where your current connection works better, verify the difference, and understand what may be holding it back.

## Core user outcome

A nontechnical user should leave a useful session with one of four honest outcomes:

1. a verified better place or setup for the activity they care about;
2. a verified tie or “no clear better spot” result;
3. an inconclusive/invalid result with a plain explanation of what prevented a recommendation;
4. a likely bottleneck or next action supported by measured evidence.

A beautiful scan that cannot support one of these outcomes is not product success.

## Core loop

connection problem or planned activity
→ quick check, two-spot comparison, or spatial survey
→ bounded measurement
→ candidate discovery
→ independent stationary verification
→ verified win / tie / inconclusive / invalid
→ useful recommendation or diagnostic explanation
→ optional save/share
→ optional before/after action and later comparison

The product should provide basic value without an account where feasible.

## Hero spatial experience

The hero experience is a short, guided spatial network survey. On supported Android devices, the phone moves through a physical environment while omanii associates session-local pose with radio/network context and low-data active probes.

The hero experience answers a consumer question:

> “Where around me will my internet actually work best?”

The scanner is allowed to feel cinematic: dark spatial canvas, sparse geometry, traces, measured patches, candidate regions, satisfying motion and a reveal. The presentation must not upgrade uncertain evidence into fake precision.

Product rules for the spatial experience:

- ARCore is initially a pose source, not a requirement to reconstruct a house.
- A session uses a local coordinate origin; cross-session geometric alignment is not promised.
- Measured, interpolated, stale and unknown areas are different states.
- Unknown space stays unknown.
- Radio context may guide candidate discovery; benchmark-quality claims require an appropriate active measurement profile.
- A transfer that spans movement is associated with its trajectory/time interval, not its final phone position.
- Network/AP/band/transport changes create segment boundaries or invalidate comparison as appropriate.
- Candidate regions are independently re-tested while stationary before a “better spot” claim.
- A flat field or tie is a valid successful result.
- Unsupported AR devices get an honest non-AR fallback, initially a manual two-spot comparison.
- Cellular spatial scanning is not a V1 promise until it passes its own physical validation gate.

## Omanii Score

Omanii Score is a proprietary benchmark-style measure of overall connection usability for sufficiently complete, valid measurement profiles.

It is not a percentage. A score of 100 represents a documented reference-quality connection, not perfection. Scores may exceed 100 substantially as capability improves.

The score may use evidence-backed inputs including download throughput, upload throughput, idle responsiveness, loaded responsiveness, jitter, valid packet-loss observations, stability and confirmed failure intervals.

Requirements:

- deterministic for a given version and sufficient statistics;
- centrally implemented outside UI code;
- versioned;
- reproducible;
- tied to a named measurement profile and endpoint context;
- explicit about completeness, lower-bound/provisional status and confidence;
- able to expose limiting metrics;
- raw sufficient statistics retained where appropriate so historical results can be reinterpreted after future calibration.

Walk-time map colors are not automatically Omanii Scores. Low-data or incomplete profiles must not receive a complete score by silently treating missing inputs as zero or good.

The research report's first equation is an experimental calibration hypothesis, not a permanent product truth. It is documented in the score architecture and must not be changed without a new version and approval.

## OG Score

OG Score (Omanii Gaming Score) measures how suitable the measured connection is for gaming.

It shares measurement primitives with Omanii Score but is a separate model with separate versioning and validity rules. It should care much more about latency, latency consistency, jitter, packet loss, loaded responsiveness and short-term instability, and much less about extreme throughput once bandwidth is sufficient.

Therefore a connection may legitimately have a high Omanii Score and mediocre OG Score, or a moderate Omanii Score and excellent OG Score.

No final OG equation is frozen in Bootstrap Task 001.

The app must not claim exact game-server ping unless it actually tests relevant infrastructure and labels that scope accurately.

## Future Boost concept

“Boost” is a legitimate future consumer surface only when it orchestrates real actions and verifies their effect.

The required loop is:

measure
→ identify an eligible action
→ perform or guide the action
→ measure again
→ report improvement, no improvement, or inconclusive

Possible future actions include guiding the user to a better physical position, identifying obvious local issues, identifying heavy network activity, opening appropriate system settings, testing DNS where relevant, comparing useful settings, router recommendations, and later independently validated routing/VPN or supported-router integrations.

Boost must never imply arbitrary bandwidth creation, hidden carrier control, RAM cleaning, guaranteed latency reductions, or a percentage improvement that was not measured.

Boost is not implemented in Bootstrap Task 001.

## Supporting capabilities

Near-term supporting capabilities, once their dependencies are validated, include:

- quick connection checks without AR;
- manual two-spot comparison;
- bounded controlled performance probes;
- simple deterministic diagnostics;
- local history and named places;
- sanitized share results;
- free generic gaming assessment;
- later data-usage totals, budgets, one widget and richer usage intelligence;
- later paid export/technician workflows;
- later optional monitoring, router integrations and professional capabilities only if a recurring job is demonstrated.

## Consumer-experience principles

1. **Immediate value.** The shortest useful path wins. Do not force a room scan when a quick check or two-spot comparison answers the question.
2. **Progressive permissions.** Ask for camera, radio/location detail, usage access and notifications only when the user chooses a feature that needs them.
3. **Personality above complexity.** Consumer copy can be playful, meme-aware and human while domain outputs stay rigorous.
4. **Delight follows truth.** Animations, haptics and dramatic reveals respond to validated domain events; they never manufacture them.
5. **Visible uncertainty.** Tie, unknown, stale and inconclusive states are product features, not error screens to hide.
6. **Shareability by design.** Share models are first-class, redacted by default and do not leak room coordinates, Wi-Fi identity or sensitive usage data.
7. **No mandatory account before basic value** where feasible.
8. **Low-data respect.** Byte budgets are visible and enforced. A free scan is not permission to consume unbounded bandwidth.
9. **Accessible language.** The default UI says what the result means; advanced diagnostics may expose dBm, RSRP, jitter definitions, loaded latency and other technical details.
10. **No engineered addiction.** Do not reward repeated bandwidth-heavy testing, scan-count streaks or statistical outliers.

Consumer-facing copy must be centralized so future personality, localization, mascot reactions, achievement text and notifications can evolve without changing measurement logic.

## Explicit V1 exclusions

V1 does not include:

- VPN/routing/bonding services;
- root or OEM “optimizer” behavior;
- automatic router configuration;
- continuous background AR;
- perfect architectural room reconstruction;
- guaranteed cross-day/cross-room map alignment;
- public coverage maps or carrier rankings;
- public leaderboards;
- marketplace functionality;
- aggregate data sales;
- custom measurement hardware;
- router/mesh hardware;
- white-label SDKs;
- chatbot-first diagnosis;
- subscription or ad systems until measurement correctness and product value are established;
- full data-usage product in the bootstrap;
- consumer Gaming Mode UI in the bootstrap;
- Boost implementation in the bootstrap.

## Truth rules

These are non-negotiable product invariants:

- missing != zero;
- unavailable != good;
- stale != fresh;
- received time != measurement time;
- repeated polling != repeated independent measurement;
- RSSI != throughput;
- link speed != internet throughput;
- throughput != total connection quality;
- HTTP request failures != exact packet loss;
- one endpoint != the entire internet;
- one regional endpoint != exact game-server latency;
- interpolated != measured;
- estimated != observed;
- unknown stays unknown;
- a network transition breaks naive comparability;
- score requires the correct measurement profile;
- incomplete tests expose incompleteness/lower bounds;
- recommendation requires sufficient independent evidence;
- candidate selection data must not also masquerade as independent validation;
- a successful action attempt != proven improvement;
- no fake optimization;
- no cause attribution beyond the available evidence;
- no scientific-validation claim before actual validation.

## Initial success conditions

The following are engineering/product gates, not universal scientific standards. They may be revised only with recorded evidence and approval.

- A supported-device room session has median return-to-start closure error at or below roughly 0.5 m and p90 at or below roughly 1 m, or the spatial resolution/session design is widened until honest.
- In stable environments, repeated region rankings reach median Spearman agreement around 0.7 or better and the same top-two region appears in at least 80% of repeats before continuous mapping is trusted.
- In deliberately selected real problem environments, a confidently chosen region produces a held-out meaningful benefit in at least 70% of cases; a flat room may correctly return no difference.
- Fewer than 10% of high-confidence wins materially reverse on held-out retest.
- Temporal reference drift is less than half the spatial effect being claimed; otherwise recommendation is deferred or labeled time-varying.
- Low-data survey active probe traffic stays within its selected budget, initially targeting <=1 MB for the walking survey, with zero late transfer after cancellation.
- Valid completion is initially >=90% on supported reference devices and >=80% in supported-device alpha use.
- Resource tests show no sustained severe thermal behavior; proposed early target is <=2 percentage-point incremental battery drain for a two-minute session, to be tested with better repeated energy measurements rather than trusted blindly.
- At least 8/10 first-time test users can identify the recommended spot and explain the uncertainty without assistance.
- Alpha crash-free sessions target >=99.5% before broad promotion.

## Initial failure / pivot conditions

Change the product promise rather than protect an attractive animation if any of these persist after reasonable correction:

- time variation overwhelms spatial differences;
- repeated independent scans cannot rank useful regions reliably;
- high-confidence recommendations reverse too often;
- useful results require excessive bandwidth, heat or unsupported devices;
- most real users cannot take an action that yields repeatable value;
- supported-device failure remains high;
- the spatial workflow is consistently beaten by simpler two-spot comparison or diagnostics.

A “no clear better spot” result is not failure. A wrong confident recommendation is.
