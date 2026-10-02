package com.omanii.app.pose

import com.omanii.app.model.Availability
import com.omanii.app.model.ClockDomainKind
import com.omanii.app.model.CoordinateFrameRef
import com.omanii.app.model.MonotonicClock
import com.omanii.app.model.SegmentRef
import com.omanii.app.model.SourceTimeMeaning
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PoseCanonicalMappingTest {
    private val id = PoseFrameIdentity("s0", "f0")
    private val localGeometry = PhysicalPose(PositionM(2.0, 1.5, -3.0), QuaternionXyzw.normalized(0.0, 1.0, 0.0, 1.0))
    private fun context(session: String = "session", run: String = "run_a", origin: PoseDataOrigin = PoseDataOrigin.LIVE_ARCORE) =
        PoseMappingContext(session, run, "test-build-1", PoseDebugMetadata(origin))
    private fun start(session: String = "session") = PoseRecord(session, id, 10, PoseRecordKind.START, PoseTracking.STARTING, reason = PoseReason.STARTED)
    private fun tracked(time: Long = 11) = PoseRecord("session", id, time, PoseRecordKind.TRACKING, PoseTracking.TRACKING,
        PoseSourceTimestamp(Long.MAX_VALUE - 1), reason = PoseReason.TRACKING_RECOVERED)
    private fun sample(time: Long = 12) = PoseRecord("session", id, time, PoseRecordKind.SAMPLE, PoseTracking.TRACKING,
        PoseSourceTimestamp(Long.MAX_VALUE), localGeometry)
    private fun readyMapper(): PoseCanonicalMapper = PoseCanonicalMapper(context()).also { it.map(start()); it.map(tracked()) }
    private fun fixture(name: String = "tracking-loss.jsonl"): PoseReplay {
        val root = generateSequence(File(".").canonicalFile) { it.parentFile }.take(5)
            .first { File(it, "fixtures/pose/$name").isFile }
        return PoseJsonl.decode(File(root, "fixtures/pose/$name").readText())
    }

    @Test fun sharedClockControlsReceiveOrderingWithoutSourceClockConversion() {
        var time = 100L
        var calls = 0
        val clock = MonotonicClock { calls++; time }
        val c = PoseCapture("session", id, { PoseFrameIdentity("s1", "f1") }, clock, {})
        val mapper = PoseCanonicalMapper(context())
        mapper.map(c.started())
        time = 200
        val pose = PhysicalPose(PositionM(0.0, 0.0, 0.0), QuaternionXyzw.IDENTITY)
        val mapped = c.accept(PoseFrame(Long.MAX_VALUE, PoseTracking.TRACKING, PoseTracking.TRACKING, pose, pose)).map(mapper::map).last()
        assertEquals(2, calls)
        assertEquals(200L, mapped.timing.receivedAtElapsedRealtimeNs)
        assertEquals(Long.MAX_VALUE, mapped.timing.sourceTimestamp!!.timestampNs)
        assertNull(mapped.timing.measurementAtElapsedRealtimeNs)
        assertFalse(mapped.timing.ageKnown)
    }

    @Test fun sharedGeometryPreservesMetresHeightXzAndHamiltonXyzwExactly() {
        val mapped = readyMapper().map(sample())
        assertEquals(com.omanii.app.model.PositionM(2.0, 1.5, -3.0), mapped.pose.value!!.position)
        assertEquals(localGeometry.orientation.x, mapped.pose.value!!.orientation.x, 0.0)
        assertEquals(localGeometry.orientation.y, mapped.pose.value!!.orientation.y, 0.0)
        assertEquals(localGeometry.orientation.z, mapped.pose.value!!.orientation.z, 0.0)
        assertEquals(localGeometry.orientation.w, mapped.pose.value!!.orientation.w, 0.0)
        assertEquals(HorizontalM(2.0, -3.0), mapped.pose.value!!.horizontal)
        assertEquals(localGeometry, mapped.debugRecord.pose)
    }

    @Test fun sourceTimestampsUseUndefinedDomainFrameCaptureAndSessionRunToken() {
        val a = readyMapper().map(sample())
        val bMapper = PoseCanonicalMapper(context(run = "run_b")); bMapper.map(start()); bMapper.map(tracked())
        val b = bMapper.map(sample())
        val otherSessionMapper = PoseCanonicalMapper(context(session = "other_session"))
        otherSessionMapper.map(start("other_session")); otherSessionMapper.map(tracked().copy(sessionId = "other_session"))
        val other = otherSessionMapper.map(sample().copy(sessionId = "other_session"))
        assertEquals(ClockDomainKind.ARCORE_FRAME_UNDEFINED, a.timing.sourceTimestamp!!.domain.kind)
        assertEquals(SourceTimeMeaning.FRAME_CAPTURE, a.timing.sourceTimestamp!!.meaning)
        assertNotEquals(a.timing.sourceTimestamp!!.domain, b.timing.sourceTimestamp!!.domain)
        assertNotEquals(a.timing.sourceTimestamp!!.domain, other.timing.sourceTimestamp!!.domain)
        assertEquals(a.timing.sourceTimestamp!!.timestampNs, b.timing.sourceTimestamp!!.timestampNs)
        assertThrows(IllegalArgumentException::class.java) { a.timing.copy(measurementAtElapsedRealtimeNs = 12) }
    }

    @Test fun segmentAndFrameReferencesAreSessionScopedAndSeparate() {
        val mapped = readyMapper().map(sample())
        assertEquals(SegmentRef("session", "s0"), mapped.segment)
        assertEquals(CoordinateFrameRef("session", "f0"), mapped.coordinateFrame)
        assertNotEquals(CoordinateFrameRef("other_session", "f0"), mapped.coordinateFrame)
        assertThrows(IllegalArgumentException::class.java) { readyMapper().map(sample().copy(sessionId = "other_session")) }
    }

    @Test fun trackingLossImmediatelyInvalidatesAssociationBeforeAnyBoundary() {
        val mapper = readyMapper(); mapper.map(sample())
        assertEquals(Availability.AVAILABLE, mapper.association!!.lastObservedGeometry.availability)
        val loss = mapper.map(PoseRecord("session", id, 13, PoseRecordKind.TRACKING, PoseTracking.PAUSED,
            PoseSourceTimestamp(1), reason = PoseReason.CAMERA_TRACKING_PAUSED, trackingFailure = "INSUFFICIENT_LIGHT"))
        assertNull(loss.previousCoordinateFrame)
        assertNull(mapper.association!!.lastObservedGeometry.value)
        assertNull(mapper.association!!.poseObservationId.value)
        assertNull(mapper.association!!.coordinateFrame.value)
        assertEquals(Availability.TEMPORARILY_UNAVAILABLE, loss.association.lastObservedGeometry.availability)
        assertEquals("CAMERA_TRACKING_PAUSED", loss.association.lastObservedGeometry.reason)
    }

    @Test fun recoveryDoesNotRestoreAssociationUntilActualNewFrameSample() {
        val replay = fixture()
        val events = replay.toCanonicalRecords("test-build-1", "replay_run")
        val recovered = events.last { it.debugRecord.kind == PoseRecordKind.TRACKING }
        assertNull(recovered.association.lastObservedGeometry.value)
        val boundary = events.single { it.debugRecord.kind == PoseRecordKind.BOUNDARY }
        assertNull(boundary.association.lastObservedGeometry.value)
        val sampleAfter = events.first { it.debugRecord.identity.coordinateFrameId == "f1" && it.debugRecord.kind == PoseRecordKind.SAMPLE }
        assertEquals(Availability.AVAILABLE, sampleAfter.association.lastObservedGeometry.availability)
        assertEquals(CoordinateFrameRef("synthetic_route", "f0"), boundary.previousCoordinateFrame)
        assertEquals(CoordinateFrameRef("synthetic_route", "f1"), boundary.coordinateFrame)
    }

    @Test fun networkOnlySegmentBoundaryPreservesFrameAndPriorObservationWithoutInventingSample() {
        val mapper = readyMapper(); val last = mapper.map(sample())
        val changed = mapper.map(PoseRecord("session", PoseFrameIdentity("s1", "f0"), 13, PoseRecordKind.BOUNDARY,
            PoseTracking.TRACKING, reason = PoseReason.SEGMENT_CHANGED, previousIdentity = id))
        assertEquals(SegmentRef("session", "s1"), changed.segment)
        assertEquals(last.coordinateFrame, changed.coordinateFrame)
        assertEquals(last.association.lastObservedGeometry, changed.association.lastObservedGeometry)
        assertEquals(last.metadata.recordId, changed.association.poseObservationId.value)
        assertNull(changed.pose.value)
        assertEquals(Availability.NOT_APPLICABLE, changed.pose.availability)
    }

    @Test fun canonicalDevelopmentMetadataKeepsDebugAndSyntheticLiveProvenance() {
        val live = readyMapper().map(sample())
        assertEquals("session", live.metadata.sessionId)
        assertEquals("test-build-1", live.metadata.appBuild)
        assertEquals("TASK-003_ARCORE_POSE", live.metadata.producer)
        assertEquals(PoseJsonl.SCHEMA, live.metadata.schemaVersion)
        assertEquals(PoseJsonl.PROTOCOL, live.metadata.protocolVersion)
        assertEquals(Availability.NOT_APPLICABLE, live.metadata.testProfile.availability)
        assertEquals("PASSIVE_POSE_CAPTURE", live.metadata.testProfile.reason)
        assertEquals(PoseDataOrigin.LIVE_ARCORE, live.debugMetadata.dataOrigin)
        assertEquals("pose_debug_only", live.debugMetadata.testProfile)
        assertTrue(fixture().toCanonicalRecords("test-build-1", "replay_run").all { it.debugMetadata.dataOrigin == PoseDataOrigin.SYNTHETIC_FIXTURE })
    }

    @Test fun enrichedExportReplayPreservesOriginalRecordsScopedClockAndCanonicalMetadata() {
        val replay = fixture()
        val mapped = replay.toCanonicalRecords("test-build-1", "replay_run")
        val enriched = PoseJsonl.encodeCanonical(mapped)
        val decoded = PoseJsonl.decode(enriched)
        assertEquals(replay.records, decoded.records)
        assertEquals(replay.dataOrigin, decoded.dataOrigin)
        assertEquals(mapped, decoded.canonicalRecords)
        assertEquals(mapped, decoded.toCanonicalRecords("test-build-1", "replay_run"))
        assertTrue(enriched.contains("FRAME_CAPTURE"))
        assertTrue(enriched.contains("arcore_15:synthetic_route:10:replay_run"))
        assertThrows(IllegalArgumentException::class.java) { decoded.toCanonicalRecords("wrong_build", "new_run") }
    }

    @Test fun enrichedReplayRejectsDomainProvenanceScopeAndMetadataTampering() {
        val json = PoseJsonl.encodeCanonical(fixture().toCanonicalRecords("test-build-1", "replay_run"))
        listOf(
            json.replace("arcore_15:synthetic_route:10:replay_run", "arcore_unrelated_run"),
            json.replace("\"canonical_source_time_meaning\":\"FRAME_CAPTURE\"", "\"canonical_source_time_meaning\":\"MEASUREMENT\""),
            json.replace("\"canonical_segment_session_id\":\"synthetic_route\"", "\"canonical_segment_session_id\":\"other_session\""),
            json.replace("\"canonical_profile_availability\":\"NOT_APPLICABLE\"", "\"canonical_profile_availability\":\"AVAILABLE\""),
            json.replace("\"canonical_producer\":\"TASK-003_ARCORE_POSE\"", "\"canonical_producer\":\"unexpected_producer\""),
        ).forEach { assertThrows(IllegalArgumentException::class.java) { PoseJsonl.decode(it) } }
    }

    @Test fun exportIdGapsFromSuppressedSamplesDoNotCreateMissingAssociationReferences() {
        val mapped = fixture().toCanonicalRecords("test-build-1", "replay_run")
        val retained = mapped.filterNot { it.debugRecord.pose?.position?.xM == 2.0 }
        val decoded = PoseJsonl.decode(PoseJsonl.encodeCanonical(retained))
        assertEquals(retained.map { it.metadata.recordId }, decoded.canonicalRecords!!.map { it.metadata.recordId })
        val ids = decoded.canonicalRecords!!.map { it.metadata.recordId }.toSet()
        assertTrue(decoded.canonicalRecords!!.all { it.association.poseObservationId.value == null || it.association.poseObservationId.value in ids })
    }

    @Test fun rawFixturesRemainReadableWithoutCanonicalMetadata() {
        val replay = fixture()
        assertNull(replay.canonicalRecords)
        assertEquals(replay, PoseJsonl.decode(PoseJsonl.encode(replay.records, replay.dataOrigin)))
    }

    @Test fun networkOnlyFixtureReplaysWithSameScopedFrameAcrossSegmentBoundary() {
        val replay = fixture("network-only-segment.jsonl")
        val mapped = replay.toCanonicalRecords("test-build-1", "replay_run")
        val boundary = mapped.single { it.debugRecord.kind == PoseRecordKind.BOUNDARY }
        assertEquals(boundary.previousCoordinateFrame, boundary.coordinateFrame)
        assertNotEquals(boundary.previousSegment, boundary.segment)
        assertEquals(Availability.AVAILABLE, boundary.association.coordinateFrame.availability)
        assertNull(boundary.pose.value)
        val next = mapped.last { it.debugRecord.kind == PoseRecordKind.SAMPLE }
        assertEquals(com.omanii.app.model.PositionM(2.0, 1.0, -1.0), next.pose.value!!.position)
        assertEquals(mapped, PoseJsonl.decode(PoseJsonl.encodeCanonical(mapped)).canonicalRecords)
    }

    @Test fun distinctSessionRunPairsCannotAliasClockInstancesOrRecordIds() {
        val pairs = listOf(
            ("a_b" to "c") to ("a" to "b_c"),
            ("a_" to "b") to ("a" to "_b"),
            ("a-b_1" to "c_d") to ("a-b" to "1_c_d"),
        )
        pairs.forEach { (first, second) ->
            fun mapped(pair: Pair<String, String>): CanonicalPoseRecord {
                val mapper = PoseCanonicalMapper(context(session = pair.first, run = pair.second))
                mapper.map(start(pair.first))
                mapper.map(tracked().copy(sessionId = pair.first))
                return mapper.map(sample().copy(sessionId = pair.first))
            }
            val a = mapped(first)
            val b = mapped(second)
            val aSource = requireNotNull(a.timing.sourceTimestamp)
            val bSource = requireNotNull(b.timing.sourceTimestamp)
            assertNotEquals(aSource.domain, bSource.domain)
            assertNotEquals(a.metadata.recordId, b.metadata.recordId)
            assertEquals(aSource.timestampNs, bSource.timestampNs)
            assertEquals(SourceTimeMeaning.FRAME_CAPTURE, aSource.meaning)
            assertNull(a.timing.measurementAtElapsedRealtimeNs)
            assertNull(b.timing.measurementAtElapsedRealtimeNs)
        }
    }
}
