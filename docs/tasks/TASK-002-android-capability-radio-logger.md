# TASK 002 - Android Capability & Radio Logger

**Status:** READY after Bootstrap Task 001 approval and shared Android build scaffold
**Type:** Implementation / platform evidence

## Objective

Build the smallest native Android foundation that can truthfully report relevant device/network capabilities and record timestamped network/radio-context observations for later scan and measurement work.

This task is instrumentation, not the consumer scanner.

## Product reason

omanii cannot make honest spatial or diagnostic claims until it knows what a specific Android device can actually observe, with what permissions, timing and unavailable states. This task creates the truthful input stream needed by later session, field and score work.

## Dependencies

- Bootstrap Task 001 approved and merged.
- Shared minimal Kotlin/Compose Android build scaffold created once by the integration owner.
- `PRODUCT.md`, `ARCHITECTURE.md`, `protocol/README.md` accepted.
- ADR-001, ADR-002, ADR-004 and ADR-005 accepted.

No dependency on Task 003 or Task 004 for core completion.

## Owned paths

Target paths after scaffold:

- `android/app/src/main/java/**/capability/**`
- `android/app/src/main/java/**/network/**`
- `android/app/src/main/java/**/devexport/**` only for local development export required here
- corresponding unit/instrumented test paths
- `fixtures/network/**`
- `docs/evidence/TASK-002/**`

Shared manifest/build files may be changed only for task-required ordinary network/radio permissions/dependencies and must be coordinated with the integration owner.

## Required context

Read only:

- `PRODUCT.md` - Truth rules, permissions philosophy, V1 exclusions.
- `ARCHITECTURE.md` - Network context, capability, timing, privacy boundaries.
- `AGENTS.md`.
- `protocol/README.md` - `CapabilityReport`, `NetworkContextObservation`, time/availability/version semantics.
- ADR-001, ADR-002, ADR-004, ADR-005.
- This task packet.

When implementing an Android API whose behavior/permission is uncertain, consult current official Android documentation and record the source in evidence. Do not guess signatures or permission behavior.

## Interfaces/contracts used

Conceptual contracts:

- `CapabilityRepository`
- `CapabilityReport`
- `NetworkContextProvider`
- `NetworkContextObservation`
- session-scoped network/path transition token semantics
- monotonic/measurement-time semantics
- availability/null semantics

Implementation may introduce Kotlin types/adapters that faithfully represent these contracts inside owned paths or an integration-owner-approved shared model location.

## Interfaces forbidden to change

This task may not redefine:

- protocol field meaning/version policy;
- score inputs/formulas;
- pose/session coordinate semantics;
- scan candidate/verification semantics;
- privacy defaults;
- forbidden permission list;
- product scope.

If Android reality conflicts with the conceptual contract, stop and escalate with exact official documentation/evidence.

## Implementation requirements

### 1. Capability report

Provide a callable/replayable report that records at least:

- device model/manufacturer as ordinary device capability metadata;
- Android/API level;
- app/build version;
- supported network observation categories;
- current permission states relevant to implemented radio observations;
- supported test-profile capability flags known at this stage;
- explicit unavailable/not-evaluated state for capabilities owned by other tasks, such as AR pose, rather than fabricating support.

Do not request permissions merely to fill the capability report.

### 2. Current network path

Observe the network actually available to the app and record where supported:

- active transport (Wi-Fi, cellular, Ethernet/other, none);
- validated/internet capability state;
- metered state;
- VPN presence and underlying transport/context where defensibly available;
- relevant private-DNS context if the chosen API provides it without broadening scope.

A Wi-Fi transport is not automatically a validated internet connection.

### 3. Connected Wi-Fi context

Where API level/permissions permit, record appropriate connected-network context such as:

- RSSI;
- frequency/band derivation when defensible;
- link speed and separate RX/TX link speed on APIs that expose them;
- Wi-Fi standard where exposed;
- SSID/BSSID only as optional sensitive context needed for transition grouping, with redaction/missing handling and no ordinary analytics/logging.

Do not present link speed as internet throughput.

### 4. Cellular/radio context

Where API level, hardware and permissions support it, record selected useful current cellular signal fields such as LTE/NR metrics. Preserve unsupported/unavailable values explicitly.

Requirements:

- do not map platform unavailable sentinel values into ordinary numeric observations;
- bind observations to the active data context as defensibly as the selected APIs allow;
- record source/API path;
- record callback/read receive time;
- record actual measurement timestamp/age only if the API exposes it;
- `age_known=false` when freshness cannot be established.

This task does not need a dense cellular scanner or spatial cellular mapping.

### 5. Transition handling

Emit a transition/event or update a session-scoped epoch/token when relevant context changes, including at minimum:

- active transport change;
- selected network replacement/loss;
- Wi-Fi AP/band transition when observable and permission allows;
- active data/radio context change that would make comparisons unsafe.

The token is for within-session comparability. Do not create a cross-user stable tracking identifier.

### 6. Permissions

Keep permission handling progressive.

- Basic transport/validation reporting should work with minimum access.
- Richer Wi-Fi/cell detail must degrade gracefully when permissions/Location state are absent.
- No Accessibility, background location, microphone, contacts, IMEI, VPN, `QUERY_ALL_PACKAGES`, broad storage or screen capture.
- A denied permission is a modeled capability state, not a crash/error loop.

### 7. Timing

Use a monotonic clock for observation ordering/duration.

Store separately when applicable:

- app receive time;
- underlying measurement time;
- age-known flag/source.

A repeated identical value may be cached/quantized/real. Do not mark it as a new independent radio measurement simply because the polling loop ran.

### 8. Local development export

Provide a development-only export path that produces inspectable line-oriented records (JSONL is acceptable for this development export) containing capability/network observations with schema/protocol identifiers.

Export rules:

- no production cloud upload;
- sensitive wireless identifiers redacted or mapped to session-scoped tokens by default;
- no camera data;
- records replayable by tests/tools later;
- export format must be labeled development/research, not mistaken for the permanently frozen wire serialization.

### 9. Minimal debug surface

A minimal debug-only screen/log view may show current capability and observations so Francis can validate real devices. Do not build consumer visual design or score UI.

## Acceptance criteria

Task can enter review only when all of the following are evidenced:

1. App builds and starts on the shared scaffold.
2. Basic capability/current-transport reporting works without requesting camera/location at cold start.
3. Permission denial/revocation paths return explicit unavailable states rather than fabricated values or crashes.
4. Wi-Fi context, when permitted/supported, includes source and timing semantics and does not label link speed as internet throughput.
5. Cellular fields accept unsupported/unavailable values without mapping sentinels to valid “excellent” numbers.
6. Network loss/transport change creates an explicit transition observable by later session code.
7. Repeated polling/rendering is not counted in code/tests as independent measurement evidence.
8. Development export is deterministic enough for fixture/replay and redacts stable sensitive identifiers by default.
9. No forbidden permission or unapproved SDK/dependency has been added.
10. Physical evidence from at least two Android devices or one device plus a clearly documented capability-limited fallback case demonstrates actual observations and denial behavior.

## Verification commands

After the shared Android scaffold exists:

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
git diff --check
```

If instrumented tests are added and a device/emulator is available:

```bash
cd android
./gradlew :app:connectedDebugAndroidTest
```

Record exact executed commands and outputs in `docs/evidence/TASK-002/`.

## Automated tests

At minimum:

- unit conversion/value mapping for each implemented radio metric;
- unavailable/sentinel mapping to explicit unavailable;
- permission denied state;
- redacted SSID/BSSID handling;
- unknown-age observation;
- observation with real measurement timestamp when provided;
- repeated identical read does not automatically increment independent-observation count in any helper introduced by this task;
- transport change/new network epoch;
- Wi-Fi AP/band transition fixture where data exists;
- active network lost/none;
- export redaction and schema/version fields;
- no raw stable wireless identifier in default dev-export fixture.

Use fake providers/test doubles. Do not rely on emulator radio behavior as proof of real-device correctness.

## Physical-device evidence

Create `docs/evidence/TASK-002/physical.md` documenting:

- exact phone model/API/build;
- granted/denied permissions;
- Wi-Fi connected case;
- cellular case where supported;
- transport switch/loss case;
- permission denial/revocation;
- examples of unknown/unavailable fields;
- timestamps/age semantics observed;
- exported sanitized sample;
- any OEM-specific anomaly.

At least one Xiaomi/Redmi-family observation is desirable early because it is founder-accessible, but lack of a second OEM does not justify inventing a compatibility claim.

## Security/privacy considerations

- Do not persist raw SSID/BSSID/cell identity beyond what is necessary for local development evidence.
- Default export uses session-scoped tokenization/redaction.
- No advertising/analytics SDK additions.
- No stable device identifier.
- Logs must not print raw sensitive identifiers by default.
- Any new permission must be visible in the handoff and manifest diff.

## Out of scope

- polished consumer UI;
- AR pose/scanner;
- field interpolation/candidate selection;
- active throughput/latency probe implementation;
- final Omanii/OG score;
- data-usage tracking;
- Boost;
- background monitoring;
- public analytics upload.

## Stop / escalation conditions

Stop and escalate if:

- implementing the requested observation requires a forbidden/sensitive permission not already approved;
- an official Android API behaves differently from the accepted contract in a way that changes semantics;
- the only way to provide a field is to invent freshness/timestamps;
- a dependency/SDK is required that adds unrelated data access or large native risk;
- the task would need to change protocol semantics or another task's owned paths;
- real-device evidence cannot reproduce a claimed radio field.

## Handoff

Report:

- files changed;
- Android APIs used and official references for non-obvious behavior;
- contracts changed: expected `none` unless separately approved;
- permissions/dependencies added;
- commands/results;
- evidence path;
- physical verification completed/remaining;
- device/OEM limitations;
- privacy/security implications;
- unresolved questions;
- commit/PR reference.

## Recommended agent role

Android platform implementer with repository/device-debugging access. Use a different agent/model for review of API semantics, permission behavior and sentinel handling.
