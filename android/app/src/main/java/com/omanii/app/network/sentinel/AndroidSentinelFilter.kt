package com.omanii.app.network.sentinel

import com.omanii.app.model.*

object AndroidSentinelFilter {
    fun cellInt(raw: Int?, range: IntRange? = null): ValueState<Int> = when {
        raw == null -> ValueState(null, Availability.UNKNOWN, "No value supplied")
        raw == Int.MAX_VALUE -> ValueState(null, Availability.TEMPORARILY_UNAVAILABLE, "Platform UNAVAILABLE; support is not established")
        range != null && raw !in range -> ValueState(null, Availability.UNKNOWN, "Outside documented range")
        else -> ValueState(raw, Availability.AVAILABLE, null)
    }

    fun lteAsu(raw: Int?) = if (raw == 255) ValueState<Int>(null, Availability.UNKNOWN, "ASU unknown") else cellInt(raw, 0..97)
    fun nrAsu(raw: Int?) = lteAsu(raw)
    fun wifiRssi(raw: Int) = if (raw == -127 || raw !in -126..-1) ValueState<Int>(null, Availability.UNKNOWN, "Invalid or unavailable RSSI") else cellInt(raw)
    fun wifiSpeed(raw: Int) = if (raw < 0) ValueState<Int>(null, Availability.TEMPORARILY_UNAVAILABLE, "Link speed unknown") else cellInt(raw)
    fun wifiFrequency(raw: Int) = if (raw <= 0) ValueState<Int>(null, Availability.TEMPORARILY_UNAVAILABLE, "Frequency unknown") else cellInt(raw)

    fun cellTiming(receivedNs: Long, rawNs: Long?, domain: ClockDomainRef): ObservationTiming = ObservationTiming(
        receivedNs, "TelephonyManager.getAllCellInfo/cache",
        rawNs?.takeIf { it >= 0 && it != Long.MAX_VALUE }?.let { SourceTimestamp(it, domain, SourceTimeMeaning.PLATFORM_RECEIPT) },
        null,
    )

    fun millisToNanos(raw: Long): Long? = raw.takeIf { it >= 0 && it <= Long.MAX_VALUE / 1_000_000 }?.times(1_000_000)
}
