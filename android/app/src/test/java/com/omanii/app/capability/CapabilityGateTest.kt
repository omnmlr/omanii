package com.omanii.app.capability

import com.omanii.app.model.*
import org.junit.Assert.*
import org.junit.Test

class CapabilityGateTest {
    @Test fun apiHardwareServiceAndPermissionAreDistinct() {
        assertEquals(Availability.NOT_SUPPORTED, CapabilityGate.evaluate(false, true, true, true).availability)
        assertEquals(Availability.NOT_SUPPORTED, CapabilityGate.evaluate(true, false, true, true).availability)
        assertEquals(Availability.TEMPORARILY_UNAVAILABLE, CapabilityGate.evaluate(true, true, false, true).availability)
        assertEquals(Availability.PERMISSION_DENIED, CapabilityGate.evaluate(true, true, true, false).availability)
        assertEquals(CapabilitySupport.SUPPORTED, CapabilityGate.evaluate(true, true, true, true).value)
    }
    @Test fun canonicalAvailabilityNeverAllowsAvailableNull() {
        Availability.entries.filter { it != Availability.AVAILABLE }.forEach { assertNull(ValueState<String>(null, it, "Synthetic reason").value) }
        try { ValueState<String>(null, Availability.AVAILABLE, null); fail("AVAILABLE null accepted") } catch (_: IllegalArgumentException) { }
    }
}
