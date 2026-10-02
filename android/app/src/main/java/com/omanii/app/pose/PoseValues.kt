package com.omanii.app.pose

import kotlin.math.abs
import kotlin.math.sqrt

// Task-local adapter values. Canonical semantics: protocol/README.md; shared mapping is integration-owned.
data class PositionM(val xM: Double, val yM: Double, val zM: Double) {
    init { require(xM.isFinite() && yM.isFinite() && zM.isFinite()) }
    val horizontal: HorizontalM get() = HorizontalM(xM, zM)
    operator fun minus(other: PositionM) = PositionM(xM - other.xM, yM - other.yM, zM - other.zM)
}

data class HorizontalM(val xM: Double, val zM: Double)

data class QuaternionXyzw(val x: Double, val y: Double, val z: Double, val w: Double) {
    init {
        require(listOf(x, y, z, w).all { it.isFinite() })
        require(abs(x * x + y * y + z * z + w * w - 1.0) < 1e-6)
    }
    fun inverse() = QuaternionXyzw(-x, -y, -z, w)
    operator fun times(q: QuaternionXyzw) = normalized(
        w * q.x + x * q.w + y * q.z - z * q.y,
        w * q.y - x * q.z + y * q.w + z * q.x,
        w * q.z + x * q.y - y * q.x + z * q.w,
        w * q.w - x * q.x - y * q.y - z * q.z,
    )
    fun rotate(p: PositionM): PositionM {
        val tx = 2 * (y * p.zM - z * p.yM)
        val ty = 2 * (z * p.xM - x * p.zM)
        val tz = 2 * (x * p.yM - y * p.xM)
        return PositionM(
            p.xM + w * tx + y * tz - z * ty,
            p.yM + w * ty + z * tx - x * tz,
            p.zM + w * tz + x * ty - y * tx,
        )
    }
    companion object {
        val IDENTITY = QuaternionXyzw(0.0, 0.0, 0.0, 1.0)
        fun normalized(x: Double, y: Double, z: Double, w: Double): QuaternionXyzw {
            val scale = maxOf(abs(x), abs(y), abs(z), abs(w))
            require(scale.isFinite() && scale > 0.0)
            val sx = x / scale; val sy = y / scale; val sz = z / scale; val sw = w / scale
            val length = sqrt(sx * sx + sy * sy + sz * sz + sw * sw)
            return QuaternionXyzw(sx / length, sy / length, sz / length, sw / length)
        }
    }
}

data class PhysicalPose(val position: PositionM, val orientation: QuaternionXyzw) {
    // Both values must be obtained in the same ARCore update. The origin anchor preserves the frame.
    fun relativeTo(originInThisFrame: PhysicalPose): PhysicalPose {
        val inverse = originInThisFrame.orientation.inverse()
        return PhysicalPose(inverse.rotate(position - originInThisFrame.position), inverse * orientation)
    }
}

data class PoseFrameIdentity(val segmentId: String, val coordinateFrameId: String) {
    init { requireToken(segmentId); requireToken(coordinateFrameId) }
}

internal fun requireToken(value: String) {
    require(value.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "Use session-scoped tokens" }
}

enum class PoseTracking { STARTING, TRACKING, PAUSED, STOPPED }
enum class PoseRecordKind { START, TRACKING, SAMPLE, BOUNDARY, STOP }
enum class PoseReason {
    STARTED, CAMERA_TRACKING_PAUSED, CAMERA_TRACKING_STOPPED, ORIGIN_TRACKING_UNAVAILABLE,
    TRACKING_RECOVERED, TRACKING_DISCONTINUITY, SOURCE_TIMESTAMP_REGRESSION, EXPLICIT_DISCONTINUITY,
    CANCELLED, BACKGROUNDED, OWNER_DESTROYED, COMPLETED, CAMERA_DENIED, INITIALIZATION_FAILED,
    CAMERA_UNAVAILABLE, UPDATE_FAILED, INVALID_POSE, BUFFER_LIMIT, CLEANUP_FAILED, SEGMENT_CHANGED,
}

enum class PoseSourceClock { ARCORE_FRAME_UNDEFINED }
enum class PoseSourceMeaning { CAMERA_IMAGE_CAPTURE }
data class PoseSourceTimestamp(
    val timestampNs: Long,
    val clockDomain: PoseSourceClock = PoseSourceClock.ARCORE_FRAME_UNDEFINED,
    val eventMeaning: PoseSourceMeaning = PoseSourceMeaning.CAMERA_IMAGE_CAPTURE,
) { init { require(timestampNs >= 0) } }

data class PoseRecord(
    val sessionId: String,
    val identity: PoseFrameIdentity,
    val receivedAtMonotonicNs: Long,
    val kind: PoseRecordKind,
    val tracking: PoseTracking,
    val sourceTimestamp: PoseSourceTimestamp? = null,
    val pose: PhysicalPose? = null,
    val reason: PoseReason? = null,
    val trackingFailure: String? = null,
    val previousIdentity: PoseFrameIdentity? = null,
) {
    init {
        requireToken(sessionId)
        require(receivedAtMonotonicNs >= 0)
        require((kind == PoseRecordKind.SAMPLE) == (pose != null))
        require(pose == null || (tracking == PoseTracking.TRACKING && sourceTimestamp != null && sourceTimestamp.timestampNs > 0))
        require(trackingFailure == null || trackingFailure.matches(Regex("[A-Z_]{1,80}")))
        require((kind == PoseRecordKind.BOUNDARY) == (previousIdentity != null))
    }
    // Frame timestamps cannot supply these values. Cross-provider association uses receive time.
    val measurementAtMonotonicNs: Long? get() = null
    val ageKnown: Boolean get() = false
}

data class PoseFrame(
    val sourceTimestampNs: Long,
    val cameraTracking: PoseTracking,
    val originTracking: PoseTracking,
    val cameraInWorld: PhysicalPose? = null,
    val originInWorld: PhysicalPose? = null,
    val trackingFailure: String? = null,
)
