package com.omanii.app.pose

/** Pose-side capture only. The caller supplies session/segment/frame IDs and the shared receive clock. */
class PoseCapture(
    private val sessionId: String,
    initialIdentity: PoseFrameIdentity,
    private val nextIdentity: (PoseReason) -> PoseFrameIdentity,
    private val nowNs: () -> Long,
    private val resetOrigin: () -> Unit,
    private val sampleIntervalNs: Long = 100_000_000L,
) {
    init { requireToken(sessionId); require(sampleIntervalNs >= 0) }
    private var identity = initialIdentity
    private val usedSegments = mutableSetOf(initialIdentity.segmentId)
    private val usedFrames = mutableSetOf(initialIdentity.coordinateFrameId)
    private var active = true
    private var tracking = PoseTracking.STARTING
    private var lastFailure: String? = null
    private var lastReceiveNs: Long? = null
    private var lastSourceNs: Long? = null
    private var lastSampleNs: Long? = null
    private var hasOrigin = false
    private var pendingBoundary: PoseReason? = null

    fun started(): PoseRecord = record(PoseRecordKind.START, readTime(), reason = PoseReason.STARTED)

    fun discontinuity(reason: PoseReason = PoseReason.EXPLICIT_DISCONTINUITY) {
        if (!active) return
        require(reason in setOf(PoseReason.EXPLICIT_DISCONTINUITY, PoseReason.TRACKING_DISCONTINUITY))
        if (hasOrigin) pendingBoundary = reason
        resetOrigin()
        lastSampleNs = null
    }

    fun accept(frame: PoseFrame): List<PoseRecord> {
        if (!active) return emptyList()
        val receivedNs = readTime()
        val source = PoseSourceTimestamp(frame.sourceTimestampNs)
        val events = mutableListOf<PoseRecord>()
        val state = when {
            frame.cameraTracking != PoseTracking.TRACKING -> frame.cameraTracking
            frame.sourceTimestampNs == 0L -> PoseTracking.STARTING
            frame.originTracking != PoseTracking.TRACKING -> PoseTracking.PAUSED
            frame.cameraInWorld == null || frame.originInWorld == null -> PoseTracking.PAUSED
            else -> PoseTracking.TRACKING
        }
        if (state != tracking || frame.trackingFailure != lastFailure) {
            tracking = state
            lastFailure = frame.trackingFailure
            val reason = when (state) {
                PoseTracking.TRACKING -> PoseReason.TRACKING_RECOVERED
                PoseTracking.STOPPED -> PoseReason.CAMERA_TRACKING_STOPPED
                else -> if (frame.cameraTracking == PoseTracking.TRACKING) PoseReason.ORIGIN_TRACKING_UNAVAILABLE
                    else PoseReason.CAMERA_TRACKING_PAUSED
            }
            events += record(PoseRecordKind.TRACKING, receivedNs, source, reason = reason, failure = lastFailure)
        }
        if (state != PoseTracking.TRACKING) {
            if (hasOrigin && pendingBoundary == null) {
                pendingBoundary = PoseReason.TRACKING_DISCONTINUITY
                resetOrigin()
            }
            return events
        }
        val previousSource = lastSourceNs
        if (previousSource != null && frame.sourceTimestampNs < previousSource && pendingBoundary == null) {
            pendingBoundary = PoseReason.SOURCE_TIMESTAMP_REGRESSION
            resetOrigin()
            // This update was made in the old anchor frame. Wait for a fresh update after reset.
            lastSourceNs = null
            tracking = PoseTracking.PAUSED
            events += record(PoseRecordKind.TRACKING, receivedNs, source, reason = pendingBoundary)
            return events
        }
        if (frame.sourceTimestampNs == previousSource) return events
        lastSourceNs = frame.sourceTimestampNs
        pendingBoundary?.let { reason ->
            val previous = identity
            val next = nextIdentity(reason)
            require(next.segmentId !in usedSegments && next.coordinateFrameId !in usedFrames)
            usedSegments += next.segmentId; usedFrames += next.coordinateFrameId
            identity = next
            events += record(PoseRecordKind.BOUNDARY, receivedNs, source, reason = reason, previous = previous)
            pendingBoundary = null
            lastSampleNs = null
        }
        hasOrigin = true
        val lastSample = lastSampleNs
        if (lastSample == null || receivedNs - lastSample >= sampleIntervalNs) {
            val local = frame.cameraInWorld!!.relativeTo(frame.originInWorld!!)
            events += record(PoseRecordKind.SAMPLE, receivedNs, source, pose = local)
            lastSampleNs = receivedNs
        }
        return events
    }

    fun stop(reason: PoseReason): PoseRecord? {
        if (!active) return null
        val time = readTime()
        active = false
        tracking = PoseTracking.STOPPED
        return record(PoseRecordKind.STOP, time, reason = reason)
    }

    private fun readTime(): Long {
        val time = nowNs()
        require(time >= 0 && (lastReceiveNs == null || time >= lastReceiveNs!!)) { "Receive clock regressed" }
        lastReceiveNs = time
        return time
    }

    private fun record(
        kind: PoseRecordKind, time: Long, source: PoseSourceTimestamp? = null,
        pose: PhysicalPose? = null, reason: PoseReason? = null, failure: String? = null,
        previous: PoseFrameIdentity? = null,
    ) = PoseRecord(sessionId, identity, time, kind, tracking, source, pose, reason, failure, previous)
}
