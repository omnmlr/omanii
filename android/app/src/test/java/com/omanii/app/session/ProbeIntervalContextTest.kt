package com.omanii.app.session

import com.omanii.app.model.*
import org.junit.Assert.*
import org.junit.Test

class ProbeIntervalContextTest {
    private fun <T : Any> known(value: T) = ValueState(value, Availability.AVAILABLE, null)
    private fun <T : Any> missing(state: Availability = Availability.NOT_EVALUATED) = ValueState<T>(null, state, null)
    private fun snapshot(time: Long, segment: String = "segment-a", epoch: String = "epoch-a") = ContextSnapshot(
        time, "session-a", known(SegmentRef("session-a", segment)), known(NetworkEpochRef("session-a", epoch)),
        known(CoordinateFrameRef("session-a", "frame-a")), missing<String>(),
    )

    private fun aToBToABoundaries() = listOf(
        ContextBoundary(30, "network transition", snapshot(30), snapshot(30, "segment-b", "epoch-b")),
        ContextBoundary(70, "network return", snapshot(70, "segment-b", "epoch-b"), snapshot(70)),
    )

    @Test fun movingPoseReferencesDoNotTurnAnIntervalIntoACompletionPoint() {
        val start = snapshot(10).copy(poseObservationId = known("pose-start"))
        val end = snapshot(2_000_000_010).copy(poseObservationId = known("pose-end"))
        val interval = ProbeIntervalContext(start, end, emptyList(), ContextContinuity.CONTINUOUS, known(2.0))
        assertEquals(2_000_000_000, interval.end.atElapsedRealtimeNs - interval.start.atElapsedRealtimeNs)
        assertNotEquals(interval.start.poseObservationId, interval.end.poseObservationId)
        assertEquals(2.0, interval.movementSpanM.value!!, 0.0)
    }

    @Test fun aToBToABoundariesCannotBeHiddenByMatchingEndpoints() {
        val start = snapshot(10)
        val end = snapshot(100)
        val boundaries = listOf(
            ContextBoundary(30, "network transition", snapshot(30), snapshot(30, "segment-b", "epoch-b")),
            ContextBoundary(70, "network return", snapshot(70, "segment-b", "epoch-b"), snapshot(70)),
        )
        val interval = ProbeIntervalContext(start, end, boundaries, ContextContinuity.BROKEN, missing())
        assertEquals(start.networkEpoch, end.networkEpoch)
        assertEquals(2, interval.boundaries.size)
        assertThrows(IllegalArgumentException::class.java) { interval.copy(continuity = ContextContinuity.CONTINUOUS) }
    }

    @Test fun clearingSourceListCannotEraseAToBToAEvidence() {
        val expected = aToBToABoundaries()
        val source = expected.toMutableList()
        val interval = ProbeIntervalContext(snapshot(10), snapshot(100), source, ContextContinuity.BROKEN, missing())
        val hashBefore = interval.hashCode()
        source.clear()
        assertEquals(expected, interval.boundaries)
        assertEquals(hashBefore, interval.hashCode())
        assertThrows(IllegalArgumentException::class.java) { interval.copy(continuity = ContextContinuity.CONTINUOUS) }
    }

    @Test fun insertingIntoSourceListCannotAlterContinuousInterval() {
        val source = mutableListOf<ContextBoundary>()
        val interval = ProbeIntervalContext(snapshot(10), snapshot(100), source, ContextContinuity.CONTINUOUS, missing())
        source.addAll(aToBToABoundaries())
        assertTrue(interval.boundaries.isEmpty())
        assertEquals(ContextContinuity.CONTINUOUS, interval.continuity)
        assertThrows(IllegalArgumentException::class.java) { interval.copy(boundaries = source) }
    }

    @Test fun copiedAndReconstructedIntervalsOwnTheirBoundarySnapshots() {
        val original = ProbeIntervalContext(snapshot(10), snapshot(100), emptyList(), ContextContinuity.CONTINUOUS, missing())
        val expected = aToBToABoundaries()
        val source = expected.toMutableList()
        val copied = original.copy(boundaries = source, continuity = ContextContinuity.BROKEN)
        val reconstructed = ProbeIntervalContext(copied.start, copied.end, source, copied.continuity, copied.movementSpanM)
        val copiedAgain = copied.copy()
        source.clear()
        source.add(ContextBoundary(200, "outside interval", snapshot(200), snapshot(200, "segment-b")))
        for (interval in listOf(copied, copiedAgain, reconstructed)) {
            assertEquals(expected, interval.boundaries)
            assertThrows(IllegalArgumentException::class.java) { interval.copy(continuity = ContextContinuity.CONTINUOUS) }
        }
        assertTrue(original.boundaries.isEmpty())
    }

    @Test fun exposedListsCannotBeMutatedThroughPropertiesOrDestructuring() {
        val interval = ProbeIntervalContext(snapshot(10), snapshot(100), aToBToABoundaries(), ContextContinuity.BROKEN, missing())
        val continuous = ProbeIntervalContext(snapshot(10), snapshot(100), emptyList(), ContextContinuity.CONTINUOUS, missing())
        for (candidate in listOf(interval, interval.copy(), continuous, continuous.copy())) {
            val (_, _, boundaries) = candidate
            assertThrows(UnsupportedOperationException::class.java) { (boundaries as MutableList<ContextBoundary>).clear() }
            assertThrows(UnsupportedOperationException::class.java) {
                (candidate.boundaries as MutableList<ContextBoundary>).add(aToBToABoundaries().first())
            }
        }
        assertEquals(aToBToABoundaries(), interval.boundaries)
        assertTrue(continuous.boundaries.isEmpty())
    }

    @Test fun constructionAndCopyValidateReplacementBoundaryEvidence() {
        val interval = ProbeIntervalContext(snapshot(10), snapshot(100), emptyList(), ContextContinuity.CONTINUOUS, missing())
        val valid = aToBToABoundaries()
        val outside = ContextBoundary(200, "outside interval", snapshot(200), snapshot(200, "segment-b"))
        val invalidCases = listOf(
            valid to ContextContinuity.CONTINUOUS,
            valid.reversed() to ContextContinuity.BROKEN,
            listOf(outside) to ContextContinuity.BROKEN,
        )
        for ((boundaries, continuity) in invalidCases) {
            assertThrows(IllegalArgumentException::class.java) {
                ProbeIntervalContext(interval.start, interval.end, boundaries, continuity, interval.movementSpanM)
            }
            assertThrows(IllegalArgumentException::class.java) { interval.copy(boundaries = boundaries, continuity = continuity) }
        }
        val broken = interval.copy(boundaries = valid, continuity = ContextContinuity.BROKEN)
        assertThrows(IllegalArgumentException::class.java) { broken.copy(start = snapshot(40)) }
        assertThrows(IllegalArgumentException::class.java) { broken.copy(end = snapshot(60)) }
    }

    @Test fun snapshotRepairPreservesValueCopyAndDestructuringBehavior() {
        val interval = ProbeIntervalContext(snapshot(10), snapshot(100), aToBToABoundaries(), ContextContinuity.BROKEN, missing())
        val copied = interval.copy()
        assertNotSame(interval, copied)
        assertEquals(interval, copied)
        assertEquals(interval.hashCode(), copied.hashCode())
        assertEquals(interval.toString(), copied.toString())
        val (start, end, boundaries, continuity, movement) = copied
        assertEquals(interval.start, start)
        assertEquals(interval.end, end)
        assertEquals(interval.boundaries, boundaries)
        assertEquals(interval.continuity, continuity)
        assertEquals(interval.movementSpanM, movement)
    }

    @Test fun matchingEndpointsDoNotUpgradeUnknownOrUnevaluatedCoverage() {
        for (state in listOf(ContextContinuity.UNKNOWN, ContextContinuity.NOT_EVALUATED)) {
            val interval = ProbeIntervalContext(snapshot(10), snapshot(100), emptyList(), state, missing())
            assertEquals(state, interval.continuity)
        }
    }

    @Test fun networkAndPoseDiscontinuitiesAreIndependent() {
        val start = snapshot(10)
        val changedSegment = snapshot(100, segment = "segment-b")
        assertEquals(start.networkEpoch, changedSegment.networkEpoch)
        val networkChange = snapshot(100, "segment-b", "epoch-b")
        assertEquals(start.coordinateFrame, networkChange.coordinateFrame)
        for (end in listOf(changedSegment, networkChange)) {
            assertThrows(IllegalArgumentException::class.java) {
                ProbeIntervalContext(start, end, emptyList(), ContextContinuity.CONTINUOUS, missing())
            }
        }
    }

    @Test fun poseFreeContextIsExplicitlyNotApplicable() {
        val start = snapshot(10).copy(coordinateFrame = missing(Availability.NOT_APPLICABLE))
        val interval = ProbeIntervalContext(start, start.copy(atElapsedRealtimeNs = 100), emptyList(),
            ContextContinuity.CONTINUOUS, missing(Availability.NOT_APPLICABLE))
        assertNull(interval.movementSpanM.value)
    }

    @Test fun missingNetworkOrFrameCannotCertifyContinuity() {
        val start = snapshot(10)
        for (unknown in listOf(start.copy(networkEpoch = missing()), start.copy(coordinateFrame = missing()))) {
            assertThrows(IllegalArgumentException::class.java) {
                ProbeIntervalContext(unknown, unknown.copy(atElapsedRealtimeNs = 100), emptyList(), ContextContinuity.CONTINUOUS, missing())
            }
        }
    }

    @Test fun crossSessionAndRegressingIntervalsAreRejected() {
        val start = snapshot(10)
        assertThrows(IllegalArgumentException::class.java) {
            start.copy(segment = known(SegmentRef("another-session", "segment-a")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ProbeIntervalContext(start, snapshot(9), emptyList(), ContextContinuity.UNKNOWN, missing())
        }
    }

    @Test fun boundariesMustBeOrderedAndInsideTheInterval() {
        val early = ContextBoundary(30, "tracking loss", snapshot(30), snapshot(30, "segment-b"))
        val late = ContextBoundary(70, "resume", snapshot(70, "segment-b"), snapshot(70))
        assertThrows(IllegalArgumentException::class.java) {
            ProbeIntervalContext(snapshot(10), snapshot(100), listOf(late, early), ContextContinuity.BROKEN, missing())
        }
        assertThrows(IllegalArgumentException::class.java) {
            ProbeIntervalContext(snapshot(40), snapshot(100), listOf(early), ContextContinuity.BROKEN, missing())
        }
    }

    @Test fun movementRemainsUnavailableInsteadOfZeroWhenUnknown() {
        val interval = ProbeIntervalContext(snapshot(10), snapshot(100), emptyList(), ContextContinuity.UNKNOWN,
            missing(Availability.UNKNOWN))
        assertNull(interval.movementSpanM.value)
        for (invalid in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { interval.copy(movementSpanM = known(invalid)) }
        }
    }
}
