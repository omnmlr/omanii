# TASK-002 physical execution

Status: **NOT PERFORMED / pending**. Date: 2026-10-02 (Africa/Lagos).

No authorized physical device was operated during this repair. No phone model,
API level, OS build, grant state, radio reading, OEM anomaly or compatibility
result has been verified. The original Redmi Note 12 and generic fallback
execution/PASS narratives were rejected and are not carried forward.

All records under `fixtures/network/` are labeled synthetic. They demonstrate
software behavior only and cannot establish Android/OEM radio behavior.

Required before physical acceptance under TASK-002:

- Integration owner provides the debug route and approved progressive rich-radio
  grant flow. Current shared manifest lacks fine/coarse location declarations.
- Record exact model, manufacturer, API, OS/app build, capability flags, current
  permission states and location toggle; do not infer hardware from model name.
- Cold start without camera/location requests; direct Wi-Fi, cellular (where
  permitted/supported), no active network, permission denial and revocation.
- Wi-Fi compatibility API behavior: RSSI, band, local link rates, standard,
  concealed identity, AP/band roam, network replacement and VPN transport change.
- Cellular cache: multiple registered/neighbor cells and SIMs, per-cell source
  receipt times, unknown measurement age, sentinel fields and active-subscription
  changes. Registered cells must not be labeled active data without evidence.
- Export sanitized samples and transitions with collection/build/session IDs.
  Observe limitations of snapshot polling and changes between reads explicitly.
- Two physical devices, or one device plus an actually exercised documented
  capability-limited fallback case. A fabricated generic profile is not evidence.

Acceptance criterion 10 remains pending. App start, permission flow, real radio
freshness, battery/thermal and network behavior are unverified. No physical PASS,
network improvement, spatial repeatability or physical performance claim is made.
