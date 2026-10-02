# TASK-002 repair handoff

Status: **software repair candidate; physical/integration acceptance pending**.
Date: 2026-10-02 (Africa/Lagos).
Worktree: `C:/Users/USER/Desktop/omanii-task-002-repair`.
Branch: `repair/task-002-capability-radio-v2`.
Unchanged HEAD/base: `4be02dce58b2b070de6bff669309ead2bbe384e9`.
No commit, push or PR. Original TASK-002 worktree was used read-only.
Canonical meaning remains in TASK-002, `protocol/README.md`, PRODUCT/ARCHITECTURE
and ADR-001/002/004/005. No shared contract, schema owner file, manifest, build,
app entry point or canonical document was modified.

## A. Salvage and reimplementation

Exact byte-for-byte salvage from the original `android/app/src/` tree:

- `main/java/com/omanii/app/capability/DeviceMetadata.kt`
- `main/java/com/omanii/app/network/model/CellularTech.kt`
- `main/java/com/omanii/app/network/model/NetworkTransport.kt`
- `main/java/com/omanii/app/network/model/WifiBand.kt`
- `main/java/com/omanii/app/network/model/WifiStandard.kt`
- `test/java/com/omanii/app/network/WifiMetricsMappingTest.kt`

Reimplemented in owned paths:

- capability: `CapabilityRepository.kt` (report/gate), `AndroidCapabilityRepository.kt`.
- network model: `NetworkObservation.kt` (path/Wi-Fi/cell/read/observation/transition
  DTOs, canonical timing/availability/epoch imports and immutable snapshot helper).
- network provider: `NetworkContextProvider.kt` (source interface, logger/journal),
  `AndroidNetworkReadSource.kt` (Android adapter), `RadioAccessGate.kt` (fine-location
  and runtime-access gate).
- network epoch: `NetworkEpochTracker.kt`.
- network sentinel: `AndroidSentinelFilter.kt`, `CellularMetricMapper.kt`.
- network token: `IdentifierAnonymizer.kt` (old pseudonymization/placeholder cases
  retained as intent; namespace/domain separation and full token digest rebuilt).
- development export: `DevExportJsonSerializer.kt`, `DevDebugStateProvider.kt`.
- tests: `CapabilityGateTest.kt`, `RadioSemanticsTest.kt`, `NetworkEpochTrackerTest.kt`,
  `IdentifierAnonymizerTest.kt`, `RepairTestData.kt`, `NetworkFixturesReplayTest.kt`,
  `DevExportTest.kt`.
- fixtures: `README.md`, `repair-cases.tsv`, `repair-golden.jsonl`. Old repeated,
  unknown-age, transport, AP/band and unvalidated fixture scenarios were salvaged
  as **synthetic cases**, not copied bytes or physical observations.

The source hash manifest supplies exact full paths and SHA-256 values for the
reviewed source/test/fixture candidate and its shared dependencies.
`files.json` lists all 28 owned code/test/fixture deliverables and verifies the six
exact salvages; `old-source-disposition.json` exhaustively names/hashes the original
owned files and states whether bytes were salvaged or deliberately rejected.

## B. Old code deliberately rejected

- `capability/AvailabilityReason.kt`, `ValueWithAvailability.kt` and
  `network/time/MonotonicClock.kt`: duplicate shared types and AVAILABLE-null loophole.
- `network/filter/IndependentSampleFilter.kt` and its tests: first-read, changed-value,
  transport-change and advancing-receipt independence assumptions.
- Old `AndroidNetworkContextProvider.kt`: cached all-cell reads treated as active fresh
  measurements; coarse-or-fine gate; one timestamp assigned across cells; unsupported
  metric ranges; raw Network hash token; missing services mapped to NONE/false;
  private-DNS absence labeled not configured.
- Old `NetworkEpochTracker.kt`: omitted selected network, serving identity, active
  subscription, VPN/path and observability boundaries; first-match-only event reason.
- Old path, VPN, radio, cell and Wi-Fi DTOs: task-local availability/timing fields,
  Boolean independence defaults and incomplete session identity.
- Old `AndroidCapabilityRepository.kt`: named later measurement profiles advertised
  supported, hardware-only claims and unsupported active-data attribution.
- Old `DevExportRecord.kt`, `DevExportWriter.kt`, serializer and debug provider:
  incomplete protocol/session metadata, swallowed errors, insufficient escaping,
  provider-lifetime ambiguity. Replaced by canonical metadata plus scoped local export.
- Old `NetworkFixturesValidationTest.kt`: substring-only schema/privacy validation.
  Remaining old tests were reimplemented rather than porting their assertions.
- All seven old fixture files were rejected as export bytes; none is physical evidence.
- Old `REPORT.md`, `physical.md`, `proposed_manifest_patch.md`: unsupported device
  PASS narratives and deferred permission/debug changes are not ported.

## C. Independent review blocker resolution

| Original blocker | Software resolution and evidence |
| --- | --- |
| 1. Unsupported physical evidence | Original physical narratives omitted; physical.md is NOT PERFORMED; every replay export is synthetic. |
| 2. Shared duplication | Only canonical MonotonicClock, ValueState, Availability imported; shared constructor rejects AVAILABLE null. |
| 3. Epoch correctness | Selected network, registered-cell identity/technology sets, active subscription, VPN/reported transport set, link/path token, internet/validation/metering, AP/band and availability changes create boundaries; all reasons and before/after evidence retained. CELLULAR_SUBSCRIPTION_CHANGED regression passes. |
| 4. Cell timing/attribution | Cache source labeled; each cell retains its own PLATFORM_RECEIPT SourceTimestamp; measurement time remains null/age false; all-radio cells have UNKNOWN active-data attribution. |
| 5. Independent samples | Every polling observation has canonical UNKNOWN independence. Per-cell repeated/updated source-receipt recency is separate and never proves independent measurement. |
| 6. Availability | Canonical states preserved; service absence, permissions, redaction, unsupported API, unknown state and not-applicable differ. Fine location required for cell cache; coarse alone cannot pass. Private DNS reports network use/context, never global configuration. |
| 7. Metrics | LTE CQI 0..15, RSRP -140..-43, RSSNR -20..30 dB, TA 0..1282; NR RSRP -156..-31, SS-RSRQ -43..20, CSI-RSRQ -20..-3, CSI-SINR -23..23/SS-SINR -23..40. Generic UNAVAILABLE maps temporary unavailability. ASU uses LTE/NR rules. Public LTE RSRQ/dbm getters lack an enforceable range so nonsentinel values are retained. |
| 8. Capability | API/hardware/service/grants gated; cell cache additionally gates location state. Later profiles and AR pose stay NOT_EVALUATED. No runtime success or physical support inferred from names. |
| 9. Shared integration | Canonical clock/time/source/availability/epoch/metadata consumed; segment IDs remain session-owned; no controller/TASK-005/score/heatmap. |
| 10. Export/evidence | Canonical metadata, app build, schema/collection/protocol/session identities, explicit profile applicability, full token digest and strict token validation. Production logger replay plus exact golden export and recursive JSON semantic validation. Actual final gate outputs and source hashes retained. |

Independent reviewer found two additional Android adapter defects during this repair:
synchronous capabilities redact location-sensitive Wi-Fi identity; and discarding
VPN-reported non-VPN transports misses an underlying switch on the same VPN network.
Bounded review repair pass **1 of 2** uses documented WifiManager compatibility reads
only on a direct default Wi-Fi path, labels source and unknown timing, and preserves
reported non-VPN transport flags without claiming exact physical attribution.
No permissions/dependencies/shared semantics were expanded. Re-review is retained
in REVIEW.md. Self-certification is not task acceptance.

Bounded final audit pass **2 of 2** corrected one additional epoch edge case:
an UNKNOWN/permission-inaccessible prior transport followed by observed NONE
does not establish NETWORK_LOST. It still creates an observability boundary.
The denial/loss regression now checks this sequence. No further autonomous repair
pass is available under the default circuit breaker.

## D. Commands and automated gates

Final exact command from the repair worktree root (Gradle project `android/`):

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
$env:ANDROID_HOME = 'C:/Users/USER/AppData/Local/Android/Sdk'
$env:GRADLE_USER_HOME = 'C:/Users/USER/.gradle'
& ./android/gradlew.bat -p android :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --no-daemon --console=plain
```

Final results are recorded in `gate-summary.json`, `android-gates.log`,
`unit-results.json`, `lint-results-debug.txt`, `replay-validation.log` and
`hygiene.json`. Source SHA-256 manifest is captured against this uncommitted HEAD;
the manifest/dependency/entry-point diff is empty.

| Final gate | Result |
| --- | --- |
| Compile / assembleDebug | PASS, exit 0; BUILD SUCCESSFUL in 2m20s; APK 10,063,641 bytes; digest in gate-summary.json |
| Full testDebugUnitTest | PASS: 63 tests across 12 suites; 0 failures, errors or skipped tests (32 shared/scaffold and 31 TASK-002) |
| lintDebug | PASS: 0 errors, 3 existing scaffold warnings (OldTargetApi, DataExtractionRules, MissingApplicationIcon); no new suppression of those warnings |
| Production logger / golden replay | PASS: 21 synthetic observations across 6 sessions; exact export matches refreshed fixture |
| Recursive JSON semantic validation | PASS: availability/timing/metadata/epoch/attribution/independence/privacy; 3 negative semantic cases rejected |
| Source identity | PASS: 54 source/contract/build/fixture SHA-256 values unchanged during final gates |
| Hygiene/scope | PASS: git diff --check plus explicit untracked-file ownership/whitespace/conflict scan; shared manifest/build/model/time/session/MainActivity diff empty |
| Physical / app start / instrumented tests | NOT PERFORMED / pending |

First sandbox compile attempts stopped at unavailable Gradle-cache lock writes
(one default-home path, then the configured cache); approved execution used the
existing SDK/cache offline. No approval rejection occurred.
Initial tests: 60 tests, one expected missing-golden failure while creating the
new synthetic fixture; a targeted seed invocation likewise failed only because
the golden did not exist. Logs are retained. After inspection the candidate was
used to create the synthetic golden. Later fixture correction made NONE fields
NOT_APPLICABLE and denied fields unavailable; the parser correctly rejected the
older fixture. Its refreshed candidate is revalidated before final gates. No test
was deleted or loosened to obtain passing results. Interim successful gates are
not cited as final source acceptance.

No instrumented test/device/emulator/start/permission execution was performed.
No active traffic/probe scheduler exists in this task; byte-budget and late-traffic
physical gates are not claimed.

## E. Remaining limits and privacy/security

Snapshot polling logs only transitions actually observed at reads; an A-B-A change
entirely between reads can be missed. An unchanged epoch never proves continuous
network context or independent sampling. VPN reported flags are partial platform
context; hidden path/AP changes remain unknown. Cell cache spans radios, can be stale,
and does not identify active-data signal samples. Technology adapters implement LTE/NR
only; other cells remain NOT_EVALUATED. Unknown identities limit transition detection.
Wi-Fi compatibility retrieval remains deprecated and requires physical/OEM validation;
this task never creates concurrent local-only networks. No captured frames or geographic
coordinates, subscriber identifiers, cloud uploads, analytics or logs are introduced.

The injected session ID belongs to the session/caller. One provider owns one run
and one ephemeral identifier salt/domain token; a new provider creates fresh token
material. Epoch and observation IDs also include a random provider namespace so
recreating a provider cannot reuse identities in the same session. Domain instance
tokens scope one provider run in the elapsed-realtime domain, not cross-boot alignment.
Salt/raw wireless/path identities exist transiently during mapping and are never
persisted/exported. Hashes remain sensitive pseudonyms within the run, not anonymous
data. Development files are app-private and only an existing debuggable build can
construct the debug export adapter; exported snapshots overwrite its single local
file and errors propagate. No manifest/privacy/retention/dependency default changed.

## F. Physical and integration work still required

`physical.md` lists the pending real-device procedure. Shared manifest has ordinary
network/Wi-Fi state grants only for TASK-002; fine/coarse location, phone-state and
debug entry wiring remain integration-owner deferred. TASK-002 provides callable
debug/report/export adapters, not an operational shared debug route or permission UI.
Rich cell/identity paths truthfully deny/degrade on the supplied base. App start and
device behavior cannot be inferred from APK assembly. Acceptance 1/2/3/4/5/6 have
software evidence with runtime behavior still pending; acceptance 10 is NOT PERFORMED.
Acceptance 7/8/9 have synthetic/static evidence. No full TASK-002 physical PASS.

## G. Review readiness and deviations

Candidate is ready for independent software review; final gates/hash/evidence
are retained. Independent source verdict is PASS WITH FOLLOW-UP after the two
bounded passes; final evidence recheck is recorded in REVIEW.md. Task completion/merge
readiness remains pending physical and shared debug/permission integration.
No unresolved shared-contract conflict. Remaining questions: integration owner's
approved debug/progressive-grant flow and the actual physical/OEM results.
Deviation: required physical execution and debug entry are pending, explicitly retained
as limits under the requested repair scope. No architecture/scope/permission expansion.
Commit/PR reference: **none, as instructed**.
