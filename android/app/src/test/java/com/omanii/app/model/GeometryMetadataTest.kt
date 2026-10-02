package com.omanii.app.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class GeometryMetadataTest {
    @Test fun coordinatesRetainHeightAndRejectNonFiniteValues() {
        val p = PositionM(-2.0, 1.5, 3.0)
        assertEquals(1.5, p.yM, 0.0)
        assertThrows(IllegalArgumentException::class.java) { p.copy(xM = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { p.copy(zM = Double.POSITIVE_INFINITY) }
    }

    @Test fun xyzwRepresentsIdentityAndKnownQuarterTurn() {
        val identity = QuaternionXyzw(0.0, 0.0, 0.0, 1.0)
        val quarterTurn = QuaternionXyzw(0.0, sqrt(0.5), 0.0, sqrt(0.5))
        assertEquals(1.0, identity.w, 0.0)
        assertEquals(0.0, quarterTurn.x, 0.0)
        QuaternionXyzw(-quarterTurn.x, -quarterTurn.y, -quarterTurn.z, -quarterTurn.w)
    }

    @Test fun invalidRotationsAreNotSilentlyNormalized() {
        assertThrows(IllegalArgumentException::class.java) { QuaternionXyzw(0.0, 0.0, 0.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { QuaternionXyzw(1.0, 1.0, 1.0, 1.0) }
        assertThrows(IllegalArgumentException::class.java) { QuaternionXyzw(Double.NaN, 0.0, 0.0, 1.0) }
    }

    @Test fun identitiesAreSessionScopedAndRemainSeparateConcepts() {
        assertNotEquals(SegmentRef("session-a", "1"), SegmentRef("session-b", "1"))
        assertNotEquals(NetworkEpochRef("session-a", "1"), NetworkEpochRef("session-b", "1"))
        assertNotEquals(CoordinateFrameRef("session-a", "1"), CoordinateFrameRef("session-b", "1"))
        assertThrows(IllegalArgumentException::class.java) { SegmentRef(" ", "1") }
        assertThrows(IllegalArgumentException::class.java) { NetworkEpochRef("a", " ") }
        assertThrows(IllegalArgumentException::class.java) { CoordinateFrameRef("a", " ") }
    }

    @Test fun developmentVersionAxesAreIndependentAndPassiveProfileIsExplicit() {
        val metadata = DevelopmentRecordMetadata("record-a", "session-a", "pose-adapter", "development-build",
            "test-schema", "test-procedure", ValueState(null, Availability.NOT_APPLICABLE, "passive pose capture"))
        assertEquals("test-schema", metadata.schemaVersion)
        assertEquals("test-procedure", metadata.protocolVersion)
        assertThrows(IllegalArgumentException::class.java) { metadata.copy(appBuild = " ") }
        assertThrows(IllegalArgumentException::class.java) { ProfileRef("test-profile", " ") }
    }
}
