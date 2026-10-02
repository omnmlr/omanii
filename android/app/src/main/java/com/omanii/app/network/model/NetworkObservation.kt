package com.omanii.app.network.model

import com.omanii.app.model.*
import java.util.Collections

data class NetworkPathSnapshot(
    val timing: ObservationTiming,
    val transport: ValueState<NetworkTransport>,
    val networkToken: ValueState<String>,
    val internet: ValueState<Boolean>,
    val validated: ValueState<Boolean>,
    val metered: ValueState<Boolean>,
    val vpn: ValueState<Boolean>,
    val underlyingTransports: ValueState<String>,
    val privateDnsActive: ValueState<Boolean>,
    val privateDnsMode: ValueState<String>,
    val pathToken: ValueState<String>,
)

data class WifiConnectedMetrics(
    val timing: ObservationTiming,
    val rssiDbm: ValueState<Int>,
    val frequencyMhz: ValueState<Int>,
    val band: ValueState<WifiBand>,
    val linkSpeedMbps: ValueState<Int>,
    val rxLinkSpeedMbps: ValueState<Int>,
    val txLinkSpeedMbps: ValueState<Int>,
    val standard: ValueState<WifiStandard>,
    val ssidToken: ValueState<String>,
    val bssidToken: ValueState<String>,
)

enum class SourceRecency { UNKNOWN, REPEATED_SOURCE_RECEIPT, UPDATED_SOURCE_RECEIPT }

data class CellularSignalMetrics(
    val timing: ObservationTiming,
    val tech: CellularTech,
    val registered: Boolean,
    val cellToken: ValueState<String>,
    val activeDataAttribution: ValueState<Boolean>,
    val metrics: Map<String, ValueState<Int>>,
    val sourceRecency: SourceRecency = SourceRecency.UNKNOWN,
)

data class NetworkRead(
    val path: NetworkPathSnapshot,
    val wifi: ValueState<WifiConnectedMetrics>,
    val cells: ValueState<List<CellularSignalMetrics>>,
    val activeDataSubscriptionToken: ValueState<String>,
)

internal fun NetworkRead.snapshot(): NetworkRead = copy(cells = cells.copy(value = cells.value?.let { list ->
    Collections.unmodifiableList(list.map { it.copy(metrics = Collections.unmodifiableMap(LinkedHashMap(it.metrics))) })
}))

data class RadioObservation(
    val observationId: String,
    val timing: ObservationTiming,
    val epoch: NetworkEpochRef,
    val read: NetworkRead,
    val independence: ValueState<Boolean>,
)

enum class TransitionReason {
    INITIAL_EPOCH, NETWORK_LOST, NETWORK_RESTORED, TRANSPORT_CHANGED,
    NETWORK_REPLACED, NETWORK_VALIDATION_CHANGED, INTERNET_CAPABILITY_CHANGED,
    METERED_CHANGED, VPN_CHANGED, PATH_CHANGED, PRIVATE_DNS_CHANGED,
    WIFI_AP_CHANGED, WIFI_BAND_CHANGED, CELLULAR_TECH_CHANGED,
    SERVING_CELL_CHANGED, CELLULAR_SUBSCRIPTION_CHANGED, OBSERVABILITY_CHANGED,
}

data class NetworkTransitionEvent(
    val atElapsedRealtimeNs: Long,
    val previous: NetworkEpochRef?,
    val next: NetworkEpochRef,
    val reasons: List<TransitionReason>,
    val before: NetworkRead?,
    val after: NetworkRead,
)
