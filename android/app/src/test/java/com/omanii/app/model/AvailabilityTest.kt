package com.omanii.app.model

import org.junit.Assert.*
import org.junit.Test

class AvailabilityTest {
    @Test fun zeroAndFalseRemainAvailableValues() {
        assertEquals(0, ValueState(0, Availability.AVAILABLE, null).value)
        assertEquals(false, ValueState(false, Availability.AVAILABLE, null).value)
    }

    @Test fun missingStatesNeverCarryMeasurements() {
        for (state in Availability.entries.filter { it != Availability.AVAILABLE }) {
            assertNull(ValueState<Int>(null, state, "platform limitation").value)
            assertThrows(IllegalArgumentException::class.java) { ValueState(0, state, null) }
        }
        assertThrows(IllegalArgumentException::class.java) { ValueState<Int>(null, Availability.AVAILABLE, null) }
    }

    @Test fun unknownAndNotEvaluatedRemainDifferent() {
        val unknown = ValueState<Boolean>(null, Availability.UNKNOWN, "evaluation inconclusive")
        val unevaluated = ValueState<Boolean>(null, Availability.NOT_EVALUATED, "owned by another adapter")
        assertNotEquals(unknown, unevaluated)
    }

    @Test fun reasonsAreOptionalButCannotBeBlank() {
        assertNull(ValueState<Int>(null, Availability.UNKNOWN, null).reason)
        assertThrows(IllegalArgumentException::class.java) { ValueState<Int>(null, Availability.UNKNOWN, " ") }
    }
}
