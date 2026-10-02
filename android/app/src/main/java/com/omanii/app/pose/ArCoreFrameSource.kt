package com.omanii.app.pose

import android.content.Context
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import java.util.concurrent.CompletableFuture

/** No image acquisition/recording, planes, depth, geospatial, cloud anchors or network observations. */
class ArCoreFrameSource private constructor(private val session: Session) : PoseFrameSource {
    private var anchor: Anchor? = null
    private var resetPending = false
    private var resumed = false
    private var closed = false

    override fun resume() {
        check(!closed)
        try { session.resume(); resumed = true }
        catch (_: CameraNotAvailableException) { throw PoseSourceException(PoseReason.CAMERA_UNAVAILABLE) }
    }
    override fun prepareGl(textureId: Int, rotation: Int, widthPx: Int, heightPx: Int) {
        check(!closed)
        session.setCameraTextureName(textureId)
        if (widthPx > 0 && heightPx > 0) session.setDisplayGeometry(rotation, widthPx, heightPx)
    }

    override fun update(): PoseFrame {
        check(resumed && !closed)
        val frame = try { session.update() }
        catch (_: CameraNotAvailableException) { throw PoseSourceException(PoseReason.CAMERA_UNAVAILABLE) }
        val camera = frame.camera
        val cameraState = camera.trackingState.toPoseTracking()
        if (resetPending) { anchor?.detach(); anchor = null; resetPending = false }
        val cameraPose = if (cameraState == PoseTracking.TRACKING && frame.timestamp != 0L) camera.pose else null
        if (anchor == null && cameraPose != null) {
            // Translation-only origin: retains world +Y up, rather than aligning axes to a tilted phone.
            anchor = session.createAnchor(Pose(cameraPose.translation, floatArrayOf(0f, 0f, 0f, 1f)))
        }
        val reference = anchor
        val referenceState = reference?.trackingState?.toPoseTracking() ?: PoseTracking.STARTING
        return PoseFrame(
            sourceTimestampNs = frame.timestamp,
            cameraTracking = cameraState,
            originTracking = referenceState,
            cameraInWorld = cameraPose?.toPhysicalPose(),
            originInWorld = if (referenceState == PoseTracking.TRACKING) reference?.pose?.toPhysicalPose() else null,
            trackingFailure = if (cameraState == PoseTracking.PAUSED) camera.trackingFailureReason.name else null,
        )
    }

    override fun resetOrigin() { resetPending = true }
    override fun pause() {
        if (resumed && !closed) { session.pause(); resumed = false }
    }
    override fun close(): CompletableFuture<Unit> {
        check(!closed)
        closed = true
        val completion = CompletableFuture<Unit>()
        val origin = anchor
        anchor = null
        // Session reference is now inaccessible to capture; close can block and belongs off the UI thread.
        Thread({
            var failed = false
            try { origin?.detach() } catch (_: Exception) { failed = true }
            try { session.close() } catch (_: Exception) { failed = true }
            if (failed) completion.completeExceptionally(IllegalStateException("ARCore cleanup failed"))
            else completion.complete(Unit)
        }, "omanii-pose-close").start()
        return completion
    }

    companion object {
        /** Caller must verify AVAILABLE runtime, CAMERA grant and resumed foreground ownership first. */
        fun create(context: Context): ArCoreFrameSource {
            val session = Session(context)
            try {
                session.configure(Config(session).apply {
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    planeFindingMode = Config.PlaneFindingMode.DISABLED
                    lightEstimationMode = Config.LightEstimationMode.DISABLED
                    depthMode = Config.DepthMode.DISABLED
                    geospatialMode = Config.GeospatialMode.DISABLED
                })
                return ArCoreFrameSource(session)
            } catch (e: Exception) {
                val completion = CompletableFuture<Unit>()
                var pauseFailed = false
                try { session.pause() } catch (_: Exception) { pauseFailed = true }
                val initialPauseFailed = pauseFailed
                Thread({
                    try {
                        session.close()
                        if (initialPauseFailed) completion.completeExceptionally(IllegalStateException("ARCore initialization cleanup failed"))
                        else completion.complete(Unit)
                    } catch (_: Exception) {
                        completion.completeExceptionally(IllegalStateException("ARCore initialization cleanup failed"))
                    }
                }, "omanii-pose-init-close").start()
                throw ArCoreInitializationException(e, completion)
            }
        }
    }
}

class ArCoreInitializationException(val initializationFailure: Exception, val cleanup: CompletableFuture<Unit>) :
    Exception("ARCore initialization failed")

private fun TrackingState.toPoseTracking(): PoseTracking = when (this) {
    TrackingState.TRACKING -> PoseTracking.TRACKING
    TrackingState.PAUSED -> PoseTracking.PAUSED
    TrackingState.STOPPED -> PoseTracking.STOPPED
}

private fun Pose.toPhysicalPose(): PhysicalPose {
    val translation = translation
    val rotation = rotationQuaternion
    return PhysicalPose(
        PositionM(translation[0].toDouble(), translation[1].toDouble(), translation[2].toDouble()),
        QuaternionXyzw.normalized(rotation[0].toDouble(), rotation[1].toDouble(), rotation[2].toDouble(), rotation[3].toDouble()),
    )
}
