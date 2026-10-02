package com.omanii.app.pose

import com.omanii.app.spatial.poseDebugTraces
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class PoseReplayTest {
    private fun fixture(name: String): String {
        val root = generateSequence(File(".").canonicalFile) { it.parentFile }.take(5)
            .firstOrNull { File(it, "fixtures/pose/$name").isFile } ?: error("Pose fixture not found")
        return File(root, "fixtures/pose/$name").readText()
    }

    @Test fun fixtureGoldenCoordinatesAndTrackingTimeline() {
        val replay = PoseJsonl.decode(fixture("tracking-loss.jsonl"))
        assertEquals(PoseDataOrigin.SYNTHETIC_FIXTURE, replay.dataOrigin)
        assertEquals(listOf(HorizontalM(0.0, 0.0), HorizontalM(2.0, -1.0), HorizontalM(0.0, 0.0), HorizontalM(-1.0, 2.0)),
            replay.records.mapNotNull { it.pose?.position?.horizontal })
        assertEquals(listOf(PoseTracking.TRACKING, PoseTracking.PAUSED, PoseTracking.TRACKING),
            replay.records.filter { it.kind == PoseRecordKind.TRACKING }.map { it.tracking })
        assertEquals(1.0, replay.records.first { it.pose?.position?.xM == 2.0 }.pose!!.position.yM, 0.0)
    }

    @Test fun replayRoundTripPreservesAllSegmentBoundariesAndSourceDomains() {
        listOf("tracking-loss.jsonl", "explicit-break.jsonl").forEach { name ->
            val replay = PoseJsonl.decode(fixture(name))
            assertEquals(replay, PoseJsonl.decode(PoseJsonl.encode(replay.records, replay.dataOrigin)))
            val boundary = replay.records.single { it.kind == PoseRecordKind.BOUNDARY }
            assertEquals(PoseFrameIdentity("s0", "f0"), boundary.previousIdentity)
            assertEquals(PoseFrameIdentity("s1", "f1"), boundary.identity)
        }
    }

    @Test fun exportContainsContractMetadataAndNoImageryOrGeography() {
        val replay = PoseJsonl.decode(fixture("tracking-loss.jsonl"))
        val json = PoseJsonl.encode(replay.records, replay.dataOrigin)
        assertTrue(json.contains("\"schema_version\":\"pose-debug-v1\""))
        assertTrue(json.contains("\"protocol_version\":\"pose-capture-v1\""))
        assertTrue(json.contains("\"coordinate_frame_id\":\"f1\""))
        assertTrue(json.contains("\"tracking\":\"PAUSED\""))
        assertTrue(json.contains("\"source_clock_domain\":\"ARCORE_FRAME_UNDEFINED\""))
        assertTrue(json.contains("\"measurement_at_monotonic_ns\":null"))
        assertTrue(json.contains("\"age_known\":false"))
        listOf("latitude", "longitude", "image_bytes", "bssid", "device_id").forEach { assertFalse(json.contains(it)) }
    }

    @Test fun longNanosecondsRemainExactAboveDoubleIntegerPrecision() {
        val replay = PoseJsonl.decode(fixture("explicit-break.jsonl"))
        val records = replay.records.map { it.copy(receivedAtMonotonicNs = 9_007_199_254_740_993L + it.receivedAtMonotonicNs,
            sourceTimestamp = it.sourceTimestamp?.copy(timestampNs = Long.MAX_VALUE - 10 + it.sourceTimestamp.timestampNs)) }
        assertEquals(records, PoseJsonl.decode(PoseJsonl.encode(records, replay.dataOrigin)).records)
    }

    @Test fun routeNeverBridgesPausedTrackingOrDifferentFrames() {
        val replay = PoseJsonl.decode(fixture("tracking-loss.jsonl"))
        val routes = poseDebugTraces(replay.records)
        assertEquals(2, routes.size)
        assertEquals(listOf("f0", "f1"), routes.map { it.identity.coordinateFrameId })
        assertEquals(listOf(2, 2), routes.map { it.observedPoints.size })
    }

    @Test fun parserRejectsWrongVersionsUnknownFieldsAndFalseAge() {
        val json = fixture("tracking-loss.jsonl")
        listOf(
            json.replace("pose-debug-v1", "unapproved-v2"),
            json.replace("\"age_known\":false", "\"age_known\":true"),
            json.replace("\"source\":\"ARCORE\"", "\"private_payload\":\"leak\""),
            json.replace("ARCORE_FRAME_UNDEFINED", "ELAPSED_REALTIME"),
        ).forEach { bad -> assertThrows(IllegalArgumentException::class.java) { PoseJsonl.decode(bad) } }
    }

    @Test fun parserRejectsRegressingReceiveTimePartialPoseAndMissingBoundary() {
        val json = fixture("tracking-loss.jsonl")
        val invalid = listOf(
            json.replace("\"received_at_monotonic_ns\":1101", "\"received_at_monotonic_ns\":999"),
            json.replace("\"x_m\":2", "\"x_m\":null"),
            json.lineSequence().filterNot { it.contains("\"kind\":\"BOUNDARY\"") }.joinToString("\n"),
            json.replace("\"qw\":1", "\"qw\":2"),
        )
        invalid.forEach { assertThrows(IllegalArgumentException::class.java) { PoseJsonl.decode(it) } }
    }

    @Test fun parserRejectsTruncationDuplicateKeysAndUnterminatedString() {
        val json = fixture("explicit-break.jsonl")
        val invalid = listOf(
            json.lineSequence().filterNot { it.contains("\"kind\":\"STOP\"") }.joinToString("\n"),
            json.replace("\"kind\":", "\"kind\":\"START\",\"kind\":"),
            json.replace("\"session_id\":\"synthetic_route\"", "\"session_id\":\"unterminated"),
        )
        invalid.forEach { assertThrows(IllegalArgumentException::class.java) { PoseJsonl.decode(it) } }
    }
}
