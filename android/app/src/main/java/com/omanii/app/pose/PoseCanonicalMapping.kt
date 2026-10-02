package com.omanii.app.pose

import com.omanii.app.model.Availability
import com.omanii.app.model.ClockDomainKind
import com.omanii.app.model.ClockDomainRef
import com.omanii.app.model.CoordinateFrameRef
import com.omanii.app.model.DevelopmentRecordMetadata
import com.omanii.app.model.ObservationTiming
import com.omanii.app.model.ProfileRef
import com.omanii.app.model.SegmentRef
import com.omanii.app.model.SourceTimeMeaning
import com.omanii.app.model.SourceTimestamp
import com.omanii.app.model.ValueState
import com.omanii.app.model.PositionM as SharedPositionM
import com.omanii.app.model.QuaternionXyzw as SharedQuaternionXyzw

/** Task-local projection onto the approved Wave 1 primitives; not a shared PoseObservation schema. */
data class CanonicalPoseGeometry(val position: SharedPositionM, val orientation: SharedQuaternionXyzw) {
    val horizontal: HorizontalM get() = HorizontalM(position.xM, position.zM)
}

fun PhysicalPose.toCanonicalGeometry() = CanonicalPoseGeometry(
    SharedPositionM(position.xM, position.yM, position.zM),
    SharedQuaternionXyzw(orientation.x, orientation.y, orientation.z, orientation.w),
)

data class PoseDebugMetadata(
    val dataOrigin: PoseDataOrigin,
    val schemaVersion: String = PoseJsonl.SCHEMA,
    val protocolVersion: String = PoseJsonl.PROTOCOL,
    val testProfile: String = "pose_debug_only",
) {
    init {
        require(schemaVersion == PoseJsonl.SCHEMA && protocolVersion == PoseJsonl.PROTOCOL && testProfile == "pose_debug_only")
    }
}

data class PoseMappingContext(
    val sessionId: String,
    val runToken: String,
    val appBuild: String,
    val debug: PoseDebugMetadata,
) {
    init { requireToken(sessionId); requireToken(runToken); require(appBuild.isNotBlank()) }
    // Length prefixes keep distinct allowed session/run pairs distinct, including underscore tokens.
    internal val scopeToken = "${sessionId.length}:$sessionId:${runToken.length}:$runToken"
    // Neither a device identity nor an ARCore/elapsed-realtime clock alignment.
    val sourceClockDomain = ClockDomainRef(ClockDomainKind.ARCORE_FRAME_UNDEFINED, "arcore_$scopeToken")
}

data class PoseSpatialAssociation(
    val segment: SegmentRef,
    val coordinateFrame: ValueState<CoordinateFrameRef>,
    val poseObservationId: ValueState<String>,
    val lastObservedGeometry: ValueState<CanonicalPoseGeometry>,
)

data class CanonicalPoseRecord(
    val metadata: DevelopmentRecordMetadata,
    val timing: ObservationTiming,
    val segment: SegmentRef,
    val coordinateFrame: CoordinateFrameRef,
    val pose: ValueState<CanonicalPoseGeometry>,
    val previousSegment: SegmentRef?,
    val previousCoordinateFrame: CoordinateFrameRef?,
    val association: PoseSpatialAssociation,
    val mappingContext: PoseMappingContext,
    val debugRecord: PoseRecord,
) {
    val debugMetadata: PoseDebugMetadata get() = mappingContext.debug
}

/** Map every control event before down-stream scheduling/UI; tracking loss immediately clears association. */
class PoseCanonicalMapper(val context: PoseMappingContext) {
    private var sequence = 0L
    private var lastReceivedNs: Long? = null
    private var currentIdentity: PoseFrameIdentity? = null
    private var lastSampleId: String? = null
    private var lastGeometry: CanonicalPoseGeometry? = null
    private var unavailableReason = "NO_TRACKED_POSE"
    @Volatile var association: PoseSpatialAssociation? = null
        private set

    @Synchronized fun map(record: PoseRecord): CanonicalPoseRecord = project(record, null)

    @Synchronized internal fun restore(record: PoseRecord, recordId: String): CanonicalPoseRecord = project(record, recordId)

    private fun project(record: PoseRecord, recordId: String?): CanonicalPoseRecord {
        require(record.sessionId == context.sessionId)
        require(lastReceivedNs == null || record.receivedAtMonotonicNs >= lastReceivedNs!!)
        lastReceivedNs = record.receivedAtMonotonicNs
        val segment = SegmentRef(record.sessionId, record.identity.segmentId)
        val frame = CoordinateFrameRef(record.sessionId, record.identity.coordinateFrameId)
        val previousSegment = record.previousIdentity?.let { SegmentRef(record.sessionId, it.segmentId) }
        val previousFrame = record.previousIdentity?.let { CoordinateFrameRef(record.sessionId, it.coordinateFrameId) }
        val prefix = "pose_${context.scopeToken}_"
        if (recordId != null) require(recordId.startsWith(prefix) && recordId.removePrefix(prefix).toLongOrNull()?.let { it >= 0 } == true)
        val metadata = DevelopmentRecordMetadata(
            recordId = recordId ?: "$prefix${sequence++}",
            sessionId = record.sessionId,
            producer = "TASK-003_ARCORE_POSE",
            appBuild = context.appBuild,
            schemaVersion = context.debug.schemaVersion,
            protocolVersion = context.debug.protocolVersion,
            testProfile = ValueState<ProfileRef>(null, Availability.NOT_APPLICABLE, "PASSIVE_POSE_CAPTURE"),
        )
        val source = record.sourceTimestamp?.let {
            SourceTimestamp(it.timestampNs, context.sourceClockDomain, SourceTimeMeaning.FRAME_CAPTURE)
        }
        val timing = ObservationTiming(record.receivedAtMonotonicNs, "ARCORE", source, null)
        val geometry = record.pose?.toCanonicalGeometry()
        val networkOnlyBoundary = record.kind == PoseRecordKind.BOUNDARY && record.reason == PoseReason.SEGMENT_CHANGED
        if (record.kind == PoseRecordKind.BOUNDARY) require(currentIdentity == record.previousIdentity)
        else if (currentIdentity != null) require(currentIdentity == record.identity)
        else require(record.kind == PoseRecordKind.START)
        if (networkOnlyBoundary) {
            require(record.identity.coordinateFrameId == record.previousIdentity!!.coordinateFrameId)
        }
        when {
            record.tracking != PoseTracking.TRACKING -> invalidate(record.reason?.name ?: record.tracking.name)
            record.kind == PoseRecordKind.BOUNDARY && !networkOnlyBoundary -> invalidate("POSE_FRAME_DISCONTINUITY")
            currentIdentity != null && record.identity.coordinateFrameId != currentIdentity!!.coordinateFrameId -> invalidate("POSE_FRAME_CHANGED")
        }
        if (geometry != null) { lastSampleId = metadata.recordId; lastGeometry = geometry }
        currentIdentity = record.identity
        val associationAvailability = if (lastGeometry != null) Availability.AVAILABLE else Availability.TEMPORARILY_UNAVAILABLE
        val state = PoseSpatialAssociation(
            segment,
            ValueState(if (lastGeometry != null) frame else null, associationAvailability, if (lastGeometry == null) unavailableReason else null),
            ValueState(lastSampleId, associationAvailability, if (lastGeometry == null) unavailableReason else null),
            ValueState(lastGeometry, associationAvailability, if (lastGeometry == null) unavailableReason else null),
        )
        association = state
        val poseState = if (geometry != null) ValueState(geometry, Availability.AVAILABLE, null)
            else ValueState<CanonicalPoseGeometry>(null,
                if (record.tracking == PoseTracking.TRACKING) Availability.NOT_APPLICABLE else Availability.TEMPORARILY_UNAVAILABLE,
                if (record.tracking == PoseTracking.TRACKING) "CONTROL_EVENT_NO_NEW_POSE" else unavailableReason)
        return CanonicalPoseRecord(metadata, timing, segment, frame, poseState, previousSegment, previousFrame,
            state, context, record)
    }

    private fun invalidate(reason: String) {
        lastSampleId = null; lastGeometry = null; unavailableReason = reason
    }
}

/** Replay provenance remains explicit; mapping does not relabel synthetic records as live evidence. */
fun PoseReplay.toCanonicalRecords(appBuild: String, runToken: String): List<CanonicalPoseRecord> {
    require(records.isNotEmpty())
    canonicalRecords?.let {
        require(it.first().mappingContext.appBuild == appBuild && it.first().mappingContext.runToken == runToken)
        return it
    }
    val mapper = PoseCanonicalMapper(PoseMappingContext(records.first().sessionId, runToken, appBuild, PoseDebugMetadata(dataOrigin)))
    return records.map(mapper::map)
}
