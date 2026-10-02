package com.omanii.app.pose

import com.omanii.app.model.MonotonicClock
import com.omanii.app.model.SegmentRef
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class PoseSourceException(val reason: PoseReason) : Exception("Pose source unavailable")

interface PoseFrameSource {
    fun resume()
    fun update(): PoseFrame
    fun resetOrigin()
    fun pause()
    fun close(): CompletableFuture<Unit>
    fun prepareGl(textureId: Int, rotation: Int, widthPx: Int, heightPx: Int) = Unit
}

/** Caller drives foreground updates; this adapter never schedules a global scan. */
class PoseProvider(
    private val source: PoseFrameSource,
    private val sessionId: String,
    initialIdentity: PoseFrameIdentity,
    nextIdentity: (PoseReason) -> PoseFrameIdentity,
    clock: MonotonicClock,
    private val emit: (PoseRecord) -> Unit,
    sampleIntervalNs: Long = 100_000_000L,
    private val dispatchCleanup: (() -> Unit) -> Unit = { it() },
) {
    private val lock = Any()
    private val accepting = AtomicBoolean(false)
    private val pendingStop = AtomicReference<PoseReason?>(null)
    private val capture = PoseCapture(sessionId, initialIdentity, nextIdentity, clock, source::resetOrigin, sampleIntervalNs)
    private var started = false
    private var stopped = false
    private var inUpdate = false
    private val pendingSegments = java.util.ArrayDeque<SegmentRef>()
    @Volatile var cleanup: CompletableFuture<Unit>? = null
        private set

    fun start() = synchronized(lock) {
        check(!started && !stopped)
        started = true
        emit(capture.started())
        if (pendingStop.get() != null) return@synchronized
        try {
            source.resume()
            accepting.set(true)
            // stop publishes its reason before closing the gate. Do not reopen a cancelled startup.
            if (pendingStop.get() != null) accepting.set(false)
        } catch (_: SecurityException) {
            requestStop(PoseReason.CAMERA_DENIED)
        } catch (e: PoseSourceException) {
            requestStop(e.reason)
        } catch (_: Exception) {
            requestStop(PoseReason.INITIALIZATION_FAILED)
        }
    }

    /** Run with the owning current GL context. LATEST_CAMERA_IMAGE avoids waiting for a new image. */
    fun update() = synchronized(lock) {
        if (!accepting.get()) return@synchronized
        inUpdate = true
        try {
            val frame = source.update()
            if (!accepting.get()) return@synchronized
            val records = capture.accept(frame)
            // Capture accepted a whole update before cancellation. Control records must remain ordered
            // even if cancellation closes the gate mid-batch; otherwise STOP can lose its frame boundary.
            records.forEach { if (it.kind != PoseRecordKind.SAMPLE || accepting.get()) emit(it) }
            if (frame.cameraTracking == PoseTracking.STOPPED) requestStop(PoseReason.CAMERA_TRACKING_STOPPED)
            // Finish the accepted update batch before emitting caller-driven segment boundaries.
            while (accepting.get() && pendingSegments.isNotEmpty()) {
                capture.changeSegment(pendingSegments.removeFirst())?.let(emit)
            }
        } catch (_: SecurityException) {
            requestStop(PoseReason.CAMERA_DENIED)
        } catch (e: PoseSourceException) {
            requestStop(e.reason)
        } catch (_: IllegalArgumentException) {
            requestStop(PoseReason.INVALID_POSE)
        } catch (_: Exception) {
            requestStop(PoseReason.UPDATE_FAILED)
        } finally {
            pendingSegments.clear()
            inUpdate = false
            pendingStop.get()?.let { reason -> dispatchCleanup { synchronized(lock) { stopLocked(reason) } } }
        }
    }

    fun discontinuity() = synchronized(lock) { if (accepting.get()) capture.discontinuity() }

    /** Requests from update callbacks follow the current batch; cancellation discards pending requests. */
    fun changeSegment(segment: SegmentRef) = synchronized(lock) {
        if (accepting.get()) {
            require(segment.sessionId == sessionId)
            requireToken(segment.segmentId)
            if (inUpdate) pendingSegments.addLast(segment)
            else capture.changeSegment(segment)?.let(emit)
        }
    }

    fun prepareGl(textureId: Int, rotation: Int, widthPx: Int, heightPx: Int) = synchronized(lock) {
        if (accepting.get()) try { source.prepareGl(textureId, rotation, widthPx, heightPx) }
        catch (_: SecurityException) { requestStop(PoseReason.CAMERA_DENIED) }
        catch (_: Exception) { requestStop(PoseReason.UPDATE_FAILED) }
    }

    /** The emission gate closes before waiting for any in-flight update. Call from foreground owner. */
    fun stop(reason: PoseReason = PoseReason.CANCELLED) {
        requestStop(reason)
    }

    private fun requestStop(reason: PoseReason) {
        val firstStop = pendingStop.compareAndSet(null, reason)
        accepting.set(false)
        if (firstStop) {
            // Android owner dispatches to main. Never wait for main while holding the GL/update lock.
            dispatchCleanup { synchronized(lock) { if (!inUpdate) stopLocked(reason) } }
        }
    }

    private fun stopLocked(reason: PoseReason) {
        if (stopped) return
        stopped = true
        accepting.set(false)
        var cleanupFailed = false
        try { source.pause() } catch (_: Exception) { cleanupFailed = true }
        cleanup = try { source.close() } catch (e: Exception) {
            cleanupFailed = true
            CompletableFuture<Unit>().also { it.completeExceptionally(e) }
        }
        capture.stop(reason)?.let(emit)
        if (cleanupFailed) {
            // Preserve the requested stop reason; cleanup failures are separately observable on cleanup.
            if (cleanup?.isCompletedExceptionally != true) {
                cleanup = CompletableFuture<Unit>().also { it.completeExceptionally(IllegalStateException("Pose cleanup failed")) }
            }
        }
    }
}
