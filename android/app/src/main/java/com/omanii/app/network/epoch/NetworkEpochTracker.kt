package com.omanii.app.network.epoch

import com.omanii.app.model.*
import com.omanii.app.network.model.*

class NetworkEpochTracker(private val sessionId: String, private val providerToken: String = java.util.UUID.randomUUID().toString()) {
    init { require(sessionId.isNotBlank() && providerToken.isNotBlank()) }
    private var last: NetworkRead? = null
    private var counter = 0
    private var epoch: NetworkEpochRef? = null
    private var lastAt = -1L
    private val journal = mutableListOf<NetworkTransitionEvent>()

    @Synchronized fun currentEpoch(): NetworkEpochRef? = epoch
    @Synchronized fun transitions(): List<NetworkTransitionEvent> = journal.toList()

    @Synchronized fun evaluate(input: NetworkRead, atNs: Long): NetworkTransitionEvent? {
        require(atNs >= lastAt && atNs >= 0)
        val read = input.snapshot()
        val before = last
        val reasons = mutableListOf<TransitionReason>()
        fun <T : Any> changed(a: ValueState<T>, b: ValueState<T>, reason: TransitionReason) {
            if (a.value != b.value || a.availability != b.availability) {
                reasons += reason
                if (a.availability != b.availability) reasons += TransitionReason.OBSERVABILITY_CHANGED
            }
        }
        if (before == null) reasons += TransitionReason.INITIAL_EPOCH else {
            val a = before.path
            val b = read.path
            changed(a.transport, b.transport, TransitionReason.TRANSPORT_CHANGED)
            if (a.transport.value != null && a.transport.value != NetworkTransport.NONE && b.transport.value == NetworkTransport.NONE) reasons += TransitionReason.NETWORK_LOST
            if (a.transport.value == NetworkTransport.NONE && b.transport.value != null && b.transport.value != NetworkTransport.NONE) reasons += TransitionReason.NETWORK_RESTORED
            changed(a.networkToken, b.networkToken, TransitionReason.NETWORK_REPLACED)
            changed(a.validated, b.validated, TransitionReason.NETWORK_VALIDATION_CHANGED)
            changed(a.internet, b.internet, TransitionReason.INTERNET_CAPABILITY_CHANGED)
            changed(a.metered, b.metered, TransitionReason.METERED_CHANGED)
            changed(a.vpn, b.vpn, TransitionReason.VPN_CHANGED)
            changed(a.underlyingTransports, b.underlyingTransports, TransitionReason.PATH_CHANGED)
            changed(a.pathToken, b.pathToken, TransitionReason.PATH_CHANGED)
            changed(a.privateDnsActive, b.privateDnsActive, TransitionReason.PRIVATE_DNS_CHANGED)
            changed(a.privateDnsMode, b.privateDnsMode, TransitionReason.PRIVATE_DNS_CHANGED)
            changed(before.activeDataSubscriptionToken, read.activeDataSubscriptionToken, TransitionReason.CELLULAR_SUBSCRIPTION_CHANGED)
            if (before.wifi.availability != read.wifi.availability) reasons += TransitionReason.OBSERVABILITY_CHANGED
            if (before.wifi.value?.bssidToken != read.wifi.value?.bssidToken) reasons += TransitionReason.WIFI_AP_CHANGED
            if (before.wifi.value?.band != read.wifi.value?.band) reasons += TransitionReason.WIFI_BAND_CHANGED
            if (before.cells.availability != read.cells.availability) reasons += TransitionReason.OBSERVABILITY_CHANGED
            val oldServing = before.cells.value?.filter { it.registered }
            val newServing = read.cells.value?.filter { it.registered }
            if (oldServing?.map { it.cellToken }?.toSet() != newServing?.map { it.cellToken }?.toSet()) reasons += TransitionReason.SERVING_CELL_CHANGED
            if (oldServing?.map { it.tech }?.toSet() != newServing?.map { it.tech }?.toSet()) reasons += TransitionReason.CELLULAR_TECH_CHANGED
        }
        last = read
        lastAt = atNs
        if (reasons.isEmpty()) return null
        val next = NetworkEpochRef(sessionId, "$providerToken:epoch-${++counter}")
        val event = NetworkTransitionEvent(atNs, epoch, next, java.util.Collections.unmodifiableList(reasons.distinct()), before, read)
        epoch = next
        journal += event
        return event
    }
}
