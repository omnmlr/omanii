package com.omanii.app.network.provider

import com.omanii.app.model.*
import com.omanii.app.network.epoch.NetworkEpochTracker
import com.omanii.app.network.model.*
import java.util.Collections

fun interface NetworkReadSource { fun read(): NetworkRead }

interface NetworkContextProvider {
    fun captureRadioObservation(): RadioObservation
    fun currentEpoch(): NetworkEpochRef?
    fun transitions(): List<NetworkTransitionEvent>
}

class NetworkContextLogger(
    private val source: NetworkReadSource,
    private val clock: MonotonicClock,
    sessionId: String,
    private val providerToken: String = java.util.UUID.randomUUID().toString(),
) : NetworkContextProvider {
    private val tracker = NetworkEpochTracker(sessionId, providerToken)
    private var count = 0L
    private val receipts = mutableMapOf<String, SourceTimestamp>()

    @Synchronized override fun captureRadioObservation(): RadioObservation {
        val raw = source.read()
        // Snapshot input collections before retaining transition evidence.
        val mapped = raw.cells.value?.map { cell ->
            val stamp = cell.timing.sourceTimestamp
            val key = cell.cellToken.value?.let { "${cell.tech}:$it" }
            val previous = key?.let { receipts[it] }
            val recency = when {
                key == null || stamp == null || previous == null -> SourceRecency.UNKNOWN
                stamp == previous -> SourceRecency.REPEATED_SOURCE_RECEIPT
                stamp.domain == previous.domain && stamp.timestampNs > previous.timestampNs -> SourceRecency.UPDATED_SOURCE_RECEIPT
                else -> SourceRecency.UNKNOWN
            }
            if (key != null && stamp != null) receipts[key] = stamp
            cell.copy(metrics = Collections.unmodifiableMap(LinkedHashMap(cell.metrics)), sourceRecency = recency)
        }
        val read = raw.copy(cells = raw.cells.copy(value = mapped?.let { Collections.unmodifiableList(it) }))
        val now = clock.nowElapsedRealtimeNs()
        require(now >= read.path.timing.receivedAtElapsedRealtimeNs)
        require(read.wifi.value == null || now >= read.wifi.value.timing.receivedAtElapsedRealtimeNs)
        require(mapped == null || mapped.all { now >= it.timing.receivedAtElapsedRealtimeNs })
        tracker.evaluate(read, now)
        return RadioObservation("$providerToken:observation-${++count}", ObservationTiming(now, "NetworkContextLogger.snapshot", null, null),
            requireNotNull(tracker.currentEpoch()), read,
            ValueState(null, Availability.UNKNOWN, "Polling, changed values and source receipt times do not prove statistical independence"))
    }
    override fun currentEpoch() = tracker.currentEpoch()
    override fun transitions() = tracker.transitions()
}
