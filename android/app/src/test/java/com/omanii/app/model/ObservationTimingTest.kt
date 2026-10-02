package com.omanii.app.model

import org.junit.Assert.*
import org.junit.Test

class ObservationTimingTest {
    private val elapsed = ClockDomainRef(ClockDomainKind.ANDROID_ELAPSED_REALTIME, "run-a")
    private val arcore = ClockDomainRef(ClockDomainKind.ARCORE_FRAME_UNDEFINED, "ar-session-a")

    @Test fun sameClockMeasurementPreservesReceiveDelay() {
        val source = SourceTimestamp(100, elapsed, SourceTimeMeaning.MEASUREMENT)
        val timing = ObservationTiming(150, "documented measurement API", source, 100)
        assertTrue(timing.ageKnown)
        assertEquals(50, timing.receivedAtElapsedRealtimeNs - timing.measurementAtElapsedRealtimeNs!!)
    }

    @Test fun arcoreTimestampCannotBecomeElapsedRealtimeMeasurementTime() {
        val source = SourceTimestamp(100, arcore, SourceTimeMeaning.FRAME_CAPTURE)
        val timing = ObservationTiming(150, "ARCore Frame", source, null)
        assertFalse(timing.ageKnown)
        assertSame(source, timing.sourceTimestamp)
        assertThrows(IllegalArgumentException::class.java) { timing.copy(measurementAtElapsedRealtimeNs = 100) }
    }

    @Test fun modemReceiptDoesNotEstablishActualMeasurementAge() {
        val source = SourceTimestamp(100_000_000, elapsed, SourceTimeMeaning.PLATFORM_RECEIPT)
        val timing = ObservationTiming(150_000_000, "CellInfo.getTimestampMillis", source, null)
        assertFalse(timing.ageKnown)
        assertThrows(IllegalArgumentException::class.java) { timing.copy(measurementAtElapsedRealtimeNs = source.timestampNs) }
    }

    @Test fun absentSourceIsNotReplacedWithReceiveTime() {
        val timing = ObservationTiming(150, "cached Wi-Fi read", null, null)
        assertFalse(timing.ageKnown)
        assertThrows(IllegalArgumentException::class.java) { timing.copy(measurementAtElapsedRealtimeNs = 150) }
    }

    @Test fun futureAndMismatchedMeasurementTimesAreRejected() {
        val source = SourceTimestamp(100, elapsed, SourceTimeMeaning.MEASUREMENT)
        assertThrows(IllegalArgumentException::class.java) { ObservationTiming(90, "API", source, 100) }
        assertThrows(IllegalArgumentException::class.java) { ObservationTiming(150, "API", source, 110) }
        assertFalse(ObservationTiming(90, "API", source, null).ageKnown)
    }

    @Test fun domainsAreScopedEvenWhenTimestampValuesMatch() {
        assertNotEquals(arcore, arcore.copy(instanceToken = "ar-session-b"))
        assertThrows(IllegalArgumentException::class.java) { elapsed.copy(instanceToken = " ") }
    }

    @Test fun sourceAndReceiveTimesAreNotRequiredToShareAnOrigin() {
        val raw = SourceTimestamp(Long.MAX_VALUE, arcore, SourceTimeMeaning.FRAME_CAPTURE)
        assertEquals(raw, ObservationTiming(10, "ARCore Frame", raw, null).sourceTimestamp)
    }
}
