# Android API provenance checked for the repair

Checked 2026-10-02 against official Android sources. These references justify
software mappings/gates; they do not prove physical/OEM behavior.

- [TelephonyManager.getAllCellInfo](https://developer.android.com/reference/android/telephony/TelephonyManager#getAllCellInfo()):
  cached all-radio records for modern targets; fine location and radio-access feature
  required; no active-data attribution inferred. Null/cache-empty differs from no cell.
- [CellInfo time/registration](https://developer.android.com/reference/android/telephony/CellInfo):
  per-cell source time represents approximate modem receipt. The adapter preserves
  this provenance as PLATFORM_RECEIPT and never derives measurement time/age.
- [LTE signal getters](https://developer.android.com/reference/android/telephony/CellSignalStrengthLte):
  CQI includes zero; RSRP includes -43 dBm; RSSNR is dB, -20..30; timing advance
  is a raw index 0..1282. Public RSRQ/dbm getters do not provide a range that justifies
  rejecting nonsentinel values. ASU is 0..97 with unavailable/unknown sentinels.
- [NR signal getters](https://developer.android.com/reference/android/telephony/CellSignalStrengthNr):
  use each named field's published range, including RSRP -156..-31 and SS-RSRQ
  -43..20. CSI and SS quality/SINR ranges differ. dbm is retained without applying
  a contradictory narrower range; no NR timing-advance physical unit claim is added.
- [SubscriptionManager.getActiveDataSubscriptionId](https://developer.android.com/reference/android/telephony/SubscriptionManager#getActiveDataSubscriptionId()):
  API 30 active-data context may differ from default. No subscription-list/phone-state
  API is used to infer active signal attribution.
- [LinkProperties](https://developer.android.com/reference/android/net/LinkProperties):
  private DNS active refers to network use; strict name versus active unnamed
  opportunistic context does not establish global device configuration.
- [WifiInfo](https://developer.android.com/reference/android/net/wifi/WifiInfo):
  local link speeds/standard and identity redaction are field-specific; link speeds
  are never end-to-end throughput. Unknown link speed is -1, so zero is retained.
- [WifiManager.getConnectionInfo compatibility notes](https://developer.android.com/reference/android/net/wifi/WifiManager#getConnectionInfo()):
  deprecated API remains supported for internet-providing connection except apps
  creating concurrent peer networks. TASK-002 creates none. Used on direct default
  Wi-Fi only with before/after selected-network checks; age remains unknown.
- [ConnectivityManager](https://developer.android.com/reference/android/net/ConnectivityManager#getNetworkCapabilities(android.net.Network)):
  on-demand capabilities can remove location-sensitive data. This is why identity
  retrieval is not based on synchronous transportInfo alone.
- [NetworkCapabilities](https://developer.android.com/reference/android/net/NetworkCapabilities):
  capability/transport flags are platform context. New transport constants are
  API-gated, including Thread at 34 and satellite at 35.
- [Read network state](https://developer.android.com/develop/connectivity/network-ops/reading-network-state):
  VPN can retain its network/VPN flag while reported underlying transports change.
  Snapshot polling can miss transitions between calls; this task does not claim
  continuous callback coverage or comparison validity from a stable epoch alone.

WifiInfo has no documented measurement timestamp for these properties. Raw route,
interface, proxy, DNS and wireless identifiers are converted to provider-scoped
tokens and omitted from exports. Physical validation and progressive grant/debug
integration remain pending as described in physical.md and REPORT.md.
