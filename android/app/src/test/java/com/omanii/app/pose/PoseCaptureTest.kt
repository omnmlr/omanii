package com.omanii.app.pose

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class PoseCaptureTest {
    private var time = 1_000_000_000L
    private var sequence = 0
    private var resets = 0
    private val id = PoseFrameIdentity("s0", "f0")
    private fun capture(interval: Long = 0) = PoseCapture("session", id, {
        sequence++; PoseFrameIdentity("s$sequence", "f$sequence")
    }, { time }, { resets++ }, interval)
    private fun pose(x: Double, y: Double, z: Double, q: QuaternionXyzw = QuaternionXyzw.IDENTITY) = PhysicalPose(PositionM(x, y, z), q)
    private fun frame(source: Long = 100, camera: PhysicalPose = pose(4.0, 2.0, -3.0), origin: PhysicalPose = pose(4.0, 2.0, -3.0)) =
        PoseFrame(source, PoseTracking.TRACKING, PoseTracking.TRACKING, camera, origin)

    @Test fun firstUsableFrameIsTranslationOriginAndRetainsCameraOrientation() {
        val c = capture(); c.started()
        val orientation = QuaternionXyzw.normalized(0.0, 1.0, 0.0, 1.0)
        val sample = c.accept(frame(camera = pose(4.0, 2.0, -3.0, orientation))).last()
        assertEquals(PositionM(0.0, 0.0, 0.0), sample.pose!!.position)
        assertEquals(orientation, sample.pose.orientation)
        assertEquals(id, sample.identity)
    }

    @Test fun horizontalProjectionRetainsHeightAndKnownGoldenCoordinates() {
        val c = capture(); c.started(); c.accept(frame())
        time += 1
        val p = c.accept(frame(101, pose(6.0, 3.5, -5.0))).last().pose!!.position
        assertEquals(PositionM(2.0, 1.5, -2.0), p)
        assertEquals(HorizontalM(2.0, -2.0), p.horizontal)
    }

    @Test fun sameFrameAnchorCompensatesWorldTranslationAndRotation() {
        val q = QuaternionXyzw.normalized(0.0, 1.0, 0.0, 1.0)
        val origin = pose(10.0, 0.0, 20.0, q)
        val local = pose(10.0, 2.0, 19.0, q).relativeTo(origin)
        assertEquals(1.0, local.position.xM, 1e-9)
        assertEquals(2.0, local.position.yM, 1e-9)
        assertEquals(0.0, local.position.zM, 1e-9)
        assertEquals(QuaternionXyzw.IDENTITY, local.orientation)
    }

    @Test fun quaternionHamiltonOrderAndNormalizationAreExplicit() {
        val q = QuaternionXyzw.normalized(0.0, 0.0, 2.0, 2.0)
        assertEquals(sqrt(0.5), q.z, 1e-9)
        val rotated = q.rotate(PositionM(1.0, 0.0, 0.0))
        assertEquals(0.0, rotated.xM, 1e-9); assertEquals(1.0, rotated.yM, 1e-9)
    }

    @Test fun startupZeroTimestampPlacesNothing() {
        val c = capture(); c.started()
        assertTrue(c.accept(frame(0)).none { it.pose != null })
    }

    @Test fun receiveAndUndefinedSourceClockAreSeparate() {
        val c = capture(); c.started()
        val sample = c.accept(frame(9_000_000_000_000L)).last()
        assertEquals(time, sample.receivedAtMonotonicNs)
        assertEquals(9_000_000_000_000L, sample.sourceTimestamp!!.timestampNs)
        assertEquals(PoseSourceClock.ARCORE_FRAME_UNDEFINED, sample.sourceTimestamp.clockDomain)
        assertNull(sample.measurementAtMonotonicNs); assertFalse(sample.ageKnown)
    }

    @Test fun receiveOrderingAllowsEqualTimeButRejectsRegression() {
        val c = capture(); c.started(); c.accept(frame())
        c.accept(frame(101))
        time--
        assertThrows(IllegalArgumentException::class.java) { c.accept(frame(102)) }
    }

    @Test fun repeatedCameraFrameIsNotAnotherObservation() {
        val c = capture(); c.started(); c.accept(frame())
        time += 2_000_000_000L
        assertTrue(c.accept(frame()).isEmpty())
    }

    @Test fun downsampleNeverRemovesTrackingTransitions() {
        val c = capture(100); c.started(); c.accept(frame())
        time += 10
        assertTrue(c.accept(frame(101)).isEmpty())
        val paused = c.accept(PoseFrame(101, PoseTracking.PAUSED, PoseTracking.PAUSED, trackingFailure = "INSUFFICIENT_LIGHT"))
        assertEquals(PoseRecordKind.TRACKING, paused.single().kind)
        assertEquals(PoseTracking.PAUSED, paused.single().tracking)
        assertNull(paused.single().pose)
    }

    @Test fun lostTrackingResumesOnlyInNewCallerAssignedSegmentAndOrigin() {
        val c = capture(); c.started(); c.accept(frame())
        time++
        val paused = c.accept(PoseFrame(101, PoseTracking.PAUSED, PoseTracking.PAUSED))
        assertTrue(paused.none { it.pose != null }); assertEquals(1, resets)
        time++
        val resumed = c.accept(frame(102, pose(50.0, 2.0, 10.0), pose(50.0, 2.0, 10.0)))
        val boundary = resumed.single { it.kind == PoseRecordKind.BOUNDARY }
        assertEquals(id, boundary.previousIdentity)
        assertEquals(PoseFrameIdentity("s1", "f1"), boundary.identity)
        assertEquals(PositionM(0.0, 0.0, 0.0), resumed.last().pose!!.position)
    }

    @Test fun untrackedAnchorBlocksPlacementEvenWithTrackedCamera() {
        val c = capture(); c.started(); c.accept(frame())
        val events = c.accept(frame(101).copy(originTracking = PoseTracking.PAUSED))
        assertNull(events.single().pose)
        assertEquals(PoseReason.ORIGIN_TRACKING_UNAVAILABLE, events.single().reason)
    }

    @Test fun explicitDiscontinuityResetsAndCreatesBoundary() {
        val c = capture(); c.started(); c.accept(frame()); c.discontinuity()
        val events = c.accept(frame(101))
        assertEquals(PoseReason.EXPLICIT_DISCONTINUITY, events.first { it.kind == PoseRecordKind.BOUNDARY }.reason)
        assertEquals(1, resets)
    }

    @Test fun sourceRegressionRequiresAnotherUpdateAfterOriginReset() {
        val c = capture(); c.started(); c.accept(frame(500))
        val regressed = c.accept(frame(100))
        assertTrue(regressed.none { it.pose != null }); assertEquals(1, resets)
        assertEquals(PoseReason.SOURCE_TIMESTAMP_REGRESSION, regressed.last().reason)
        assertEquals(PoseReason.SOURCE_TIMESTAMP_REGRESSION, c.accept(frame(101)).first { it.kind == PoseRecordKind.BOUNDARY }.reason)
    }

    @Test fun stopRecordsReasonAndRejectsLaterFrames() {
        val c = capture(); c.started(); c.accept(frame())
        assertEquals(PoseReason.BACKGROUNDED, c.stop(PoseReason.BACKGROUNDED)!!.reason)
        assertTrue(c.accept(frame(101)).isEmpty()); assertNull(c.stop(PoseReason.CANCELLED))
    }

    @Test fun callerCannotReusePreviousSegmentOrFrame() {
        val c = PoseCapture("session", id, { id }, { time }, {})
        c.started(); c.accept(frame()); c.discontinuity()
        assertThrows(IllegalArgumentException::class.java) { c.accept(frame(101)) }
    }

    @Test fun invalidCoordinatesOrQuaternionAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { PositionM(Double.NaN, 0.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { QuaternionXyzw.normalized(0.0, 0.0, 0.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { QuaternionXyzw(0.0, 0.0, 0.0, 2.0) }
    }
}
