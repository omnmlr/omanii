package com.omanii.app.network

import com.omanii.app.model.*
import com.omanii.app.network.model.*
import com.omanii.app.network.provider.*
import com.omanii.app.network.sentinel.*
import org.junit.Assert.*
import org.junit.Test

class RadioSemanticsTest {
    @Test fun coarseOnlyCannotPassCellGateAndRevocationRemainsExplicit() {
        assertEquals(Availability.PERMISSION_DENIED, RadioAccessGate.cell(true, true, false, ok(true)).availability)
        assertEquals(true, RadioAccessGate.cell(true, true, true, ok(true)).value)
        assertEquals(Availability.TEMPORARILY_UNAVAILABLE, RadioAccessGate.cell(true, true, true, ok(false)).availability)
        assertEquals(Availability.UNKNOWN, RadioAccessGate.cell(true, true, true, absent()).availability)
        assertEquals(Availability.NOT_SUPPORTED, RadioAccessGate.cell(false, true, true, ok(true)).availability)
    }
    @Test fun lteBoundariesAndCqiZeroRemainAvailable() {
        val mapped = CellularMetricMapper.lte(26, -140, 0, 1282, -43, -34, 30, 0)
        assertEquals(0, mapped.getValue("cqi").value)
        assertEquals(-43, mapped.getValue("rsrp_dbm").value)
        assertEquals(30, mapped.getValue("rssnr_db").value)
        assertEquals(1282, mapped.getValue("timing_advance_index").value)
        assertEquals(-34, mapped.getValue("rsrq_db").value)
        val old = CellularMetricMapper.lte(24, -100, 30, 0, null, null, null, null)
        assertEquals(Availability.NOT_SUPPORTED, old.getValue("rsrp_dbm").availability)
    }
    @Test fun nrFullDocumentedRangesArePreserved() {
        val low = CellularMetricMapper.nr(-156, 0, -156, -20, -23, -156, -43, -23)
        val high = CellularMetricMapper.nr(-31, 97, -31, -3, 23, -31, 20, 40)
        assertTrue(low.values.all { it.availability == Availability.AVAILABLE })
        assertTrue(high.values.all { it.availability == Availability.AVAILABLE })
        assertNull(CellularMetricMapper.nr(null, null, -157, -21, 24, -30, 21, 41).getValue("ss_rsrq_db").value)
    }
    @Test fun unavailableDoesNotEstablishUnsupportedAndAsuIsTechnologySpecific() {
        val mapped = CellularMetricMapper.lte(37, Int.MAX_VALUE, 255, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
        assertTrue(mapped.values.all { it.value == null })
        assertEquals(Availability.TEMPORARILY_UNAVAILABLE, mapped.getValue("cqi").availability)
        assertEquals(97, AndroidSentinelFilter.nrAsu(97).value)
        assertNull(AndroidSentinelFilter.nrAsu(99).value)
        assertNull(AndroidSentinelFilter.lteAsu(-1).value)
    }
    @Test fun wifiMissingAndZeroSpeedHaveDifferentMeanings() {
        assertNull(AndroidSentinelFilter.wifiRssi(-127).value)
        assertEquals(-60, AndroidSentinelFilter.wifiRssi(-60).value)
        assertEquals(0, AndroidSentinelFilter.wifiSpeed(0).value)
        assertNull(AndroidSentinelFilter.wifiSpeed(-1).value)
        assertNull(AndroidSentinelFilter.wifiFrequency(0).value)
    }
    @Test fun platformReceiptIsNeverMeasurementTimeIncludingFutureReceipt() {
        for (raw in listOf(0L, 50L, 150L, Long.MAX_VALUE, -1L)) {
            val timing = AndroidSentinelFilter.cellTiming(100, raw, domain)
            assertNull(timing.measurementAtElapsedRealtimeNs)
            assertFalse(timing.ageKnown)
            timing.sourceTimestamp?.let { assertEquals(SourceTimeMeaning.PLATFORM_RECEIPT, it.meaning) }
        }
        assertNull(AndroidSentinelFilter.millisToNanos(Long.MAX_VALUE))
        assertEquals(5_000_000L, AndroidSentinelFilter.millisToNanos(5))
    }
    @Test fun changedValuesFirstReadAndTransportSwitchDoNotEstablishIndependence() {
        var value = read()
        val logger = NetworkContextLogger(NetworkReadSource { value }, MonotonicClock { 100 }, "synthetic-session")
        assertNull(logger.captureRadioObservation().independence.value)
        value = value.copy(wifi = ok(wifi(rssi = -70)))
        assertNull(logger.captureRadioObservation().independence.value)
        value = read(transport = NetworkTransport.CELLULAR, cells = listOf(cell()))
        assertNull(logger.captureRadioObservation().independence.value)
    }
    @Test fun perCellReceiptUpdatesSeparateRecencyFromIndependence() {
        var value = read(transport = NetworkTransport.CELLULAR, cells = listOf(cell("a", 1), cell("b", 3)))
        val logger = NetworkContextLogger(NetworkReadSource { value }, MonotonicClock { 100 }, "s")
        val first = logger.captureRadioObservation()
        assertEquals(listOf(1L, 3L), first.read.cells.value!!.map { it.timing.sourceTimestamp!!.timestampNs })
        val repeated = logger.captureRadioObservation()
        assertTrue(repeated.read.cells.value!!.all { it.sourceRecency == SourceRecency.REPEATED_SOURCE_RECEIPT })
        value = value.copy(cells = ok(listOf(cell("a", 2), cell("b", 3))))
        val updated = logger.captureRadioObservation()
        assertEquals(SourceRecency.UPDATED_SOURCE_RECEIPT, updated.read.cells.value!![0].sourceRecency)
        assertEquals(SourceRecency.REPEATED_SOURCE_RECEIPT, updated.read.cells.value!![1].sourceRecency)
        assertNull(updated.independence.value)
        assertTrue(updated.read.cells.value!!.all { !it.timing.ageKnown && it.activeDataAttribution.value == null })
    }
    @Test fun sourceCollectionsCannotEraseRetainedEvidence() {
        val cells = mutableListOf(cell())
        val logger = NetworkContextLogger(NetworkReadSource { read(transport = NetworkTransport.CELLULAR, cells = cells) }, MonotonicClock { 100 }, "s")
        logger.captureRadioObservation()
        cells.clear()
        assertEquals(1, logger.transitions().first().after.cells.value!!.size)
        try { (logger.transitions().first().after.cells.value as MutableList).clear(); fail("Mutable evidence") } catch (_: UnsupportedOperationException) { }
    }
}
