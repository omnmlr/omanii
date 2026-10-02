package com.omanii.app.session

import com.omanii.app.model.Availability
import com.omanii.app.model.CoordinateFrameRef
import com.omanii.app.model.NetworkEpochRef
import com.omanii.app.model.SegmentRef
import com.omanii.app.model.ValueState
import java.util.Collections

data class ContextSnapshot(
    val atElapsedRealtimeNs: Long,
    val sessionId: String,
    val segment: ValueState<SegmentRef>,
    val networkEpoch: ValueState<NetworkEpochRef>,
    val coordinateFrame: ValueState<CoordinateFrameRef>,
    val poseObservationId: ValueState<String>,
) {
    init {
        require(atElapsedRealtimeNs >= 0 && sessionId.isNotBlank())
        require(segment.value == null || segment.value.sessionId == sessionId)
        require(networkEpoch.value == null || networkEpoch.value.sessionId == sessionId)
        require(coordinateFrame.value == null || coordinateFrame.value.sessionId == sessionId)
        require(poseObservationId.value == null || poseObservationId.value.isNotBlank())
    }
}

enum class ContextContinuity { CONTINUOUS, BROKEN, UNKNOWN, NOT_EVALUATED }

data class ContextBoundary(
    val atElapsedRealtimeNs: Long,
    val reason: String,
    val before: ContextSnapshot,
    val after: ContextSnapshot,
) {
    init {
        require(before.sessionId == after.sessionId && reason.isNotBlank())
        require(atElapsedRealtimeNs in before.atElapsedRealtimeNs..after.atElapsedRealtimeNs)
    }
}

/** Canonical meaning: protocol/README.md, ProbeObservation interval/context semantics. */
class ProbeIntervalContext(
    val start: ContextSnapshot,
    val end: ContextSnapshot,
    boundaries: List<ContextBoundary>,
    val continuity: ContextContinuity,
    val movementSpanM: ValueState<Double>,
) {
    // Own the validated journal; neither the source list nor a consumer can mutate it.
    val boundaries: List<ContextBoundary> = Collections.unmodifiableList(ArrayList(boundaries))

    init {
        require(start.sessionId == end.sessionId && end.atElapsedRealtimeNs >= start.atElapsedRealtimeNs)
        require(this.boundaries.all {
            it.before.sessionId == start.sessionId &&
                it.atElapsedRealtimeNs in start.atElapsedRealtimeNs..end.atElapsedRealtimeNs
        })
        require(this.boundaries.zipWithNext().all { (a, b) -> a.atElapsedRealtimeNs <= b.atElapsedRealtimeNs })
        require(this.boundaries.isEmpty() || continuity == ContextContinuity.BROKEN)
        require(movementSpanM.value == null || (movementSpanM.value.isFinite() && movementSpanM.value >= 0))
        if (continuity == ContextContinuity.CONTINUOUS) {
            require(start.segment.availability == Availability.AVAILABLE)
            require(start.networkEpoch.availability == Availability.AVAILABLE)
            require(start.segment.value == end.segment.value && start.networkEpoch.value == end.networkEpoch.value)
            require(start.coordinateFrame == end.coordinateFrame)
            require(start.coordinateFrame.availability in setOf(Availability.AVAILABLE, Availability.NOT_APPLICABLE))
        }
    }

    // Preserve value semantics while routing every copy through snapshotting and validation.
    fun copy(
        start: ContextSnapshot = this.start,
        end: ContextSnapshot = this.end,
        boundaries: List<ContextBoundary> = this.boundaries,
        continuity: ContextContinuity = this.continuity,
        movementSpanM: ValueState<Double> = this.movementSpanM,
    ): ProbeIntervalContext = ProbeIntervalContext(start, end, boundaries, continuity, movementSpanM)

    operator fun component1(): ContextSnapshot = start
    operator fun component2(): ContextSnapshot = end
    operator fun component3(): List<ContextBoundary> = boundaries
    operator fun component4(): ContextContinuity = continuity
    operator fun component5(): ValueState<Double> = movementSpanM

    override fun equals(other: Any?): Boolean = this === other ||
        (other is ProbeIntervalContext && start == other.start && end == other.end &&
            boundaries == other.boundaries && continuity == other.continuity && movementSpanM == other.movementSpanM)

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + boundaries.hashCode()
        result = 31 * result + continuity.hashCode()
        return 31 * result + movementSpanM.hashCode()
    }

    override fun toString(): String = "ProbeIntervalContext(start=$start, end=$end, boundaries=$boundaries, " +
        "continuity=$continuity, movementSpanM=$movementSpanM)"
}

interface ProbeContextHooks {
    fun begin(probeId: String, startAtElapsedRealtimeNs: Long): ContextSnapshot
    fun end(probeId: String, endAtElapsedRealtimeNs: Long): ProbeIntervalContext
}
