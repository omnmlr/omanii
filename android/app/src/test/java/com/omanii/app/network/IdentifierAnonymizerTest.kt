package com.omanii.app.network

import com.omanii.app.model.Availability
import com.omanii.app.network.token.IdentifierAnonymizer
import org.junit.Assert.*
import org.junit.Test

class IdentifierAnonymizerTest {
    @Test fun sameProviderDeterminismAndProviderIsolation() {
        val a = IdentifierAnonymizer("a")
        val b = IdentifierAnonymizer("b")
        assertEquals(a.anonymizeBssid("aa:bb:cc:dd:ee:ff"), a.anonymizeBssid("AA:BB:CC:DD:EE:FF"))
        assertNotEquals(a.anonymizeBssid("aa:bb:cc:dd:ee:ff"), b.anonymizeBssid("aa:bb:cc:dd:ee:ff"))
        assertFalse(a.anonymizeSsid("\"synthetic-network\"").value!!.contains("synthetic-network"))
    }
    @Test fun placeholdersRemainRedactedOrUnknown() {
        assertEquals(Availability.REDACTED, tokens.anonymizeBssid("02:00:00:00:00:00").availability)
        assertEquals(Availability.UNKNOWN, tokens.anonymizeBssid(null).availability)
        assertEquals(Availability.UNKNOWN, tokens.anonymizeSsid("<unknown ssid>").availability)
        assertEquals(Availability.UNKNOWN, tokens.anonymizeBssid("malformed").availability)
    }
}
