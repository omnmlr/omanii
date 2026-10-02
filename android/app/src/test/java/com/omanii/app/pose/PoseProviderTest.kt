package com.omanii.app.pose

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentLinkedQueue
import com.omanii.app.model.SegmentRef
import com.omanii.app.model.CoordinateFrameRef

class PoseProviderTest {
    private class Source : PoseFrameSource {
        var updates = 0; var pauses = 0; var closes = 0; var resets = 0
        var failure: Exception? = null
        var pauseFails = false
        var graphicsFails = false
        var block: (() -> Unit)? = null
        var resumeBlock: (() -> Unit)? = null
        var frameFactory: ((Int) -> PoseFrame)? = null
        override fun resume() { resumeBlock?.invoke(); failure?.let { throw it } }
        override fun update(): PoseFrame {
            updates++; block?.invoke(); failure?.let { throw it }
            frameFactory?.let { return it(updates) }
            val pose = PhysicalPose(PositionM(0.0, 0.0, 0.0), QuaternionXyzw.IDENTITY)
            return PoseFrame(updates.toLong(), PoseTracking.TRACKING, PoseTracking.TRACKING, pose, pose)
        }
        override fun resetOrigin() { resets++ }
        override fun pause() { pauses++; if (pauseFails) throw IllegalStateException() }
        override fun close(): CompletableFuture<Unit> { closes++; return CompletableFuture.completedFuture(Unit) }
        override fun prepareGl(textureId: Int, rotation: Int, widthPx: Int, heightPx: Int) {
            if (graphicsFails) throw IllegalStateException()
        }
    }
    private fun provider(s: Source, events: MutableList<PoseRecord>) = PoseProvider(
        s, "session", PoseFrameIdentity("s0", "f0"), { PoseFrameIdentity("s1", "f1") }, { 10L }, events::add, 0,
    )

    @Test fun cancellationStopsCollectionAndCleansUpExactlyOnce() {
        val source = Source(); val events = mutableListOf<PoseRecord>(); val p = provider(source, events)
        p.start(); p.update(); p.stop(); p.update(); p.stop()
        assertEquals(1, source.updates); assertEquals(1, source.pauses); assertEquals(1, source.closes)
        assertEquals(PoseReason.CANCELLED, events.last().reason); assertTrue(p.cleanup!!.isDone)
    }

    @Test fun foregroundLifecycleStopCannotEmitLaterValidPose() {
        val source = Source(); val events = mutableListOf<PoseRecord>(); val p = provider(source, events)
        p.start(); p.update(); p.stop(PoseReason.BACKGROUNDED)
        val count = events.size; p.update()
        assertEquals(count, events.size); assertEquals(PoseReason.BACKGROUNDED, events.last().reason)
    }

    @Test fun cancellationDuringInflightUpdateClosesEmissionGate() {
        val source = Source(); val events = mutableListOf<PoseRecord>(); val p = provider(source, events)
        val entered = CountDownLatch(1); val release = CountDownLatch(1); val cancelling = CountDownLatch(1)
        source.block = { entered.countDown(); check(release.await(5, TimeUnit.SECONDS)) }
        p.start()
        val update = Thread { p.update() }; update.start()
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        val cancel = Thread { cancelling.countDown(); p.stop() }; cancel.start()
        assertTrue(cancelling.await(5, TimeUnit.SECONDS))
        // Wait for stop to reach the held monitor; accepting is cleared before monitor acquisition.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (cancel.state != Thread.State.BLOCKED && System.nanoTime() < deadline) Thread.yield()
        assertEquals(Thread.State.BLOCKED, cancel.state)
        release.countDown(); update.join(5000); cancel.join(5000)
        assertFalse(update.isAlive); assertFalse(cancel.isAlive)
        assertEquals(listOf(PoseRecordKind.START, PoseRecordKind.STOP), events.map { it.kind })
    }

    @Test fun initializationFailureStillClosesSourceAndRecordsReason() {
        val source = Source().apply { failure = IllegalStateException() }; val events = mutableListOf<PoseRecord>()
        val p = provider(source, events); p.start(); p.update()
        assertEquals(PoseReason.INITIALIZATION_FAILED, events.last().reason)
        assertEquals(1, source.closes); assertEquals(0, source.updates)
    }

    @Test fun revokedPermissionStopsAndReleases() {
        val source = Source(); val events = mutableListOf<PoseRecord>(); val p = provider(source, events)
        p.start(); source.failure = SecurityException(); p.update()
        assertEquals(PoseReason.CAMERA_DENIED, events.last().reason); assertEquals(1, source.closes)
    }

    @Test fun pauseFailureDoesNotPreventCloseAndIsObservable() {
        val source = Source().apply { pauseFails = true }; val events = mutableListOf<PoseRecord>(); val p = provider(source, events)
        p.start(); p.stop()
        assertEquals(1, source.closes); assertTrue(p.cleanup!!.isCompletedExceptionally)
        assertEquals(PoseReason.CANCELLED, events.last().reason)
    }

    @Test fun resumePermissionRevocationRecordsCameraDenied() {
        val source = Source().apply { failure = SecurityException() }; val events = mutableListOf<PoseRecord>()
        val p = provider(source, events); p.start(); p.update()
        assertEquals(PoseReason.CAMERA_DENIED, events.last().reason)
        assertEquals(1, source.closes); assertEquals(0, source.updates)
    }

    @Test fun graphicsFailureClosesEmissionGateBeforeOwnerCleanup() {
        val source = Source().apply { graphicsFails = true }; val events = mutableListOf<PoseRecord>()
        val ownerQueue = mutableListOf<() -> Unit>()
        val p = PoseProvider(source, "session", PoseFrameIdentity("s0", "f0"), { PoseFrameIdentity("s1", "f1") },
            { 10L }, events::add, dispatchCleanup = { ownerQueue.add(it) })
        p.start(); p.prepareGl(1, 0, 1, 1); p.update()
        assertEquals(0, source.updates); assertEquals(0, source.pauses); assertEquals(0, source.closes)
        ownerQueue.toList().forEach { it() }
        assertEquals(1, source.pauses); assertEquals(1, source.closes)
        assertEquals(PoseReason.UPDATE_FAILED, events.last().reason)
    }

    @Test fun updateFailureDispatchesCleanupToOwnerAndNeverWaitsForOwnerUnderLock() {
        val source = Source(); val events = mutableListOf<PoseRecord>()
        val ownerQueue = mutableListOf<() -> Unit>()
        val p = PoseProvider(source, "session", PoseFrameIdentity("s0", "f0"), { PoseFrameIdentity("s1", "f1") },
            { 10L }, events::add, dispatchCleanup = { ownerQueue.add(it) })
        p.start(); source.failure = PoseSourceException(PoseReason.CAMERA_UNAVAILABLE); p.update(); p.update()
        assertEquals(1, source.updates); assertEquals(0, source.closes)
        assertTrue(ownerQueue.isNotEmpty())
        ownerQueue.toList().forEach { it() }
        assertEquals(1, source.pauses); assertEquals(1, source.closes)
        assertEquals(PoseReason.CAMERA_UNAVAILABLE, events.last().reason)
    }

    @Test fun cancellationDuringRecoveryPreservesBoundaryBeforeStopWithoutLateSample() {
        val source = Source(); val events = mutableListOf<PoseRecord>()
        val pose = PhysicalPose(PositionM(0.0, 0.0, 0.0), QuaternionXyzw.IDENTITY)
        source.frameFactory = { n -> if (n == 2) PoseFrame(2, PoseTracking.PAUSED, PoseTracking.PAUSED)
            else PoseFrame(n.toLong(), PoseTracking.TRACKING, PoseTracking.TRACKING, pose, pose) }
        lateinit var p: PoseProvider
        var cancelOnRecovery = false
        p = PoseProvider(source, "session", PoseFrameIdentity("s0", "f0"), { PoseFrameIdentity("s1", "f1") }, { 10L }, {
            events += it
            if (cancelOnRecovery && it.kind == PoseRecordKind.TRACKING && it.tracking == PoseTracking.TRACKING) p.stop()
        }, 0)
        p.start(); p.update(); p.update(); cancelOnRecovery = true; p.update()
        assertEquals(listOf(PoseRecordKind.TRACKING, PoseRecordKind.BOUNDARY, PoseRecordKind.STOP), events.takeLast(3).map { it.kind })
        assertEquals(PoseFrameIdentity("s1", "f1"), events.last().identity)
        assertEquals(events, PoseJsonl.decode(PoseJsonl.encode(events, PoseDataOrigin.SYNTHETIC_FIXTURE)).records)
        assertEquals(1, source.closes)
    }

    @Test fun stoppedCameraReleasesOnOwnerBeforeTerminalEmission() {
        val source = Source().apply { frameFactory = { PoseFrame(1, PoseTracking.STOPPED, PoseTracking.STOPPED) } }
        val events = mutableListOf<PoseRecord>(); val p = provider(source, events)
        p.start(); p.update(); p.update()
        assertEquals(PoseReason.CAMERA_TRACKING_STOPPED, events.last().reason)
        assertEquals(1, events.count { it.kind == PoseRecordKind.STOP }); assertEquals(1, source.closes)
    }

    @Test fun cancellationDuringBlockedResumeCannotReopenEmissionBeforeOwnerCleanup() {
        val source = Source(); val events = mutableListOf<PoseRecord>()
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val ownerQueue = ConcurrentLinkedQueue<() -> Unit>()
        source.resumeBlock = { entered.countDown(); check(release.await(5, TimeUnit.SECONDS)) }
        val p = PoseProvider(source, "session", PoseFrameIdentity("s0", "f0"), { PoseFrameIdentity("s1", "f1") },
            { 10L }, events::add, dispatchCleanup = { ownerQueue.add(it) })
        val startup = Thread { p.start() }; startup.start()
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        p.stop(PoseReason.CANCELLED)
        assertEquals(0, source.pauses)
        release.countDown(); startup.join(5000); assertFalse(startup.isAlive)
        p.update()
        assertEquals(0, source.updates)
        assertEquals(listOf(PoseRecordKind.START), events.map { it.kind })
        while (true) { val action = ownerQueue.poll() ?: break; action() }
        assertEquals(1, source.pauses); assertEquals(1, source.closes)
        assertEquals(PoseReason.CANCELLED, events.last().reason)
    }

    @Test fun externalSegmentChangeNeverResetsPoseSourceOrKeepsEmittingAfterStop() {
        val source = Source(); val events = mutableListOf<PoseRecord>(); val p = provider(source, events)
        p.start(); p.update(); p.changeSegment(SegmentRef("session", "network_segment")); p.update()
        assertEquals(0, source.resets)
        assertEquals("f0", events.last().identity.coordinateFrameId)
        assertEquals("network_segment", events.last().identity.segmentId)
        p.stop(); val count = events.size; p.changeSegment(SegmentRef("session", "later_segment"))
        assertEquals(count, events.size)
        assertEquals(events, PoseJsonl.decode(PoseJsonl.encode(events, PoseDataOrigin.SYNTHETIC_FIXTURE)).records)
    }

    @Test fun reentrantSegmentChangeDuringRecoveryPreservesCanonicalAndReplayOrdering() {
        val source = Source(); val events = mutableListOf<PoseRecord>()
        val mapped = mutableListOf<CanonicalPoseRecord>()
        val mapper = PoseCanonicalMapper(PoseMappingContext("session", "run", "test-build",
            PoseDebugMetadata(PoseDataOrigin.SYNTHETIC_FIXTURE)))
        val pose = PhysicalPose(PositionM(0.0, 0.0, 0.0), QuaternionXyzw.IDENTITY)
        source.frameFactory = { n -> if (n == 2) PoseFrame(2, PoseTracking.PAUSED, PoseTracking.PAUSED)
            else PoseFrame(n.toLong(), PoseTracking.TRACKING, PoseTracking.TRACKING, pose, pose) }
        lateinit var p: PoseProvider
        var changeOnRecovery = false
        p = PoseProvider(source, "session", PoseFrameIdentity("s0", "f0"), { PoseFrameIdentity("s1", "f1") }, { 10L }, {
            events += it
            mapped += mapper.map(it)
            if (changeOnRecovery && it.kind == PoseRecordKind.TRACKING && it.tracking == PoseTracking.TRACKING) {
                changeOnRecovery = false
                p.changeSegment(SegmentRef("session", "network_segment"))
            }
        }, 0)
        p.start(); p.update(); p.update(); changeOnRecovery = true; p.update()
        assertEquals(listOf(PoseRecordKind.TRACKING, PoseRecordKind.BOUNDARY, PoseRecordKind.SAMPLE, PoseRecordKind.BOUNDARY),
            events.takeLast(4).map { it.kind })
        assertEquals(listOf(PoseReason.TRACKING_DISCONTINUITY, PoseReason.SEGMENT_CHANGED),
            events.filter { it.kind == PoseRecordKind.BOUNDARY }.map { it.reason })
        assertEquals(PoseFrameIdentity("network_segment", "f1"), events.last().identity)
        assertEquals(CoordinateFrameRef("session", "f1"), mapped.last().association.coordinateFrame.value)
        assertEquals(1, source.resets)
        p.stop()
        assertEquals(events, PoseJsonl.decode(PoseJsonl.encode(events, PoseDataOrigin.SYNTHETIC_FIXTURE)).records)
        assertEquals(mapped, PoseJsonl.decode(PoseJsonl.encodeCanonical(mapped)).canonicalRecords)
    }

    @Test fun queuedReentrantSegmentChangeCannotSurviveCancellation() {
        val source = Source(); val events = mutableListOf<PoseRecord>()
        val pose = PhysicalPose(PositionM(0.0, 0.0, 0.0), QuaternionXyzw.IDENTITY)
        source.frameFactory = { n -> if (n == 2) PoseFrame(2, PoseTracking.PAUSED, PoseTracking.PAUSED)
            else PoseFrame(n.toLong(), PoseTracking.TRACKING, PoseTracking.TRACKING, pose, pose) }
        lateinit var p: PoseProvider
        var cancelOnRecovery = false
        p = PoseProvider(source, "session", PoseFrameIdentity("s0", "f0"), { PoseFrameIdentity("s1", "f1") }, { 10L }, {
            events += it
            if (cancelOnRecovery && it.kind == PoseRecordKind.TRACKING && it.tracking == PoseTracking.TRACKING) {
                cancelOnRecovery = false
                p.changeSegment(SegmentRef("session", "network_segment"))
                p.stop()
            }
        }, 0)
        p.start(); p.update(); p.update(); cancelOnRecovery = true; p.update()
        assertEquals(listOf(PoseRecordKind.TRACKING, PoseRecordKind.BOUNDARY, PoseRecordKind.STOP),
            events.takeLast(3).map { it.kind })
        assertFalse(events.any { it.reason == PoseReason.SEGMENT_CHANGED })
        assertEquals(PoseFrameIdentity("s1", "f1"), events.last().identity)
        assertEquals(events, PoseJsonl.decode(PoseJsonl.encode(events, PoseDataOrigin.SYNTHETIC_FIXTURE)).records)
    }

    @Test fun reentrantSegmentRequestsStayFifoWhenBoundaryCallbackAddsAnother() {
        val source = Source(); val events = mutableListOf<PoseRecord>()
        val mapped = mutableListOf<CanonicalPoseRecord>()
        val mapper = PoseCanonicalMapper(PoseMappingContext("session", "run", "test-build",
            PoseDebugMetadata(PoseDataOrigin.SYNTHETIC_FIXTURE)))
        lateinit var p: PoseProvider
        p = PoseProvider(source, "session", PoseFrameIdentity("s0", "f0"), { PoseFrameIdentity("s1", "f1") }, { 10L }, {
            events += it
            mapped += mapper.map(it)
            if (it.kind == PoseRecordKind.TRACKING && it.tracking == PoseTracking.TRACKING) {
                p.changeSegment(SegmentRef("session", "network_a"))
                p.changeSegment(SegmentRef("session", "network_b"))
            }
            if (it.kind == PoseRecordKind.BOUNDARY && it.identity.segmentId == "network_a") {
                p.changeSegment(SegmentRef("session", "network_c"))
            }
        }, 0)
        p.start(); p.update(); p.stop()
        val boundaries = events.filter { it.kind == PoseRecordKind.BOUNDARY }
        assertEquals(listOf("network_a", "network_b", "network_c"), boundaries.map { it.identity.segmentId })
        assertTrue(boundaries.all { it.identity.coordinateFrameId == "f0" && it.reason == PoseReason.SEGMENT_CHANGED })
        assertEquals("s0", events.single { it.kind == PoseRecordKind.SAMPLE }.identity.segmentId)
        assertEquals(0, source.resets)
        assertEquals(events, PoseJsonl.decode(PoseJsonl.encode(events, PoseDataOrigin.SYNTHETIC_FIXTURE)).records)
        assertEquals(mapped, PoseJsonl.decode(PoseJsonl.encodeCanonical(mapped)).canonicalRecords)
    }
}
