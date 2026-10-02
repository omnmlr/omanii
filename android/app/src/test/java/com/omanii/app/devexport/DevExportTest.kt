package com.omanii.app.devexport

import com.omanii.app.model.*
import com.omanii.app.network.*
import com.omanii.app.network.provider.*
import org.junit.Assert.*
import org.junit.Test

class DevExportTest {
    @Test fun metadataTimingAndDefaultRedactionArePresent() {
        val logger = NetworkContextLogger(NetworkReadSource { read() }, MonotonicClock { 100 }, "synthetic-session")
        val obs = logger.captureRadioObservation()
        val text = DevExportJsonSerializer.observation(obs, DevExportJsonSerializer.metadata(obs.observationId, "synthetic-session", "test-build"), true)
        assertTrue(text.contains("\"protocol_version\":\"${DevExportJsonSerializer.PROTOCOL}\""))
        assertTrue(text.contains("\"collection_id\":\"${DevExportJsonSerializer.PROTOCOL}\""))
        assertTrue(text.contains("\"age_known\":false"))
        assertTrue(text.contains("\"measurement_at_ns\":null"))
        assertTrue(text.contains("\"availability\":\"NOT_APPLICABLE\""))
        assertTrue(text.contains("\"synthetic\":true"))
        assertFalse(Regex("([0-9a-f]{2}:){5}[0-9a-f]{2}").containsMatchIn(text))
        assertFalse(text.contains("synthetic-only-fixture-salt"))
        assertEquals(text, DevExportJsonSerializer.observation(obs, DevExportJsonSerializer.metadata(obs.observationId, "synthetic-session", "test-build"), true))
    }
    @Test(expected = IllegalArgumentException::class) fun crossSessionMetadataIsRejected() {
        val obs = NetworkContextLogger(NetworkReadSource { read() }, MonotonicClock { 100 }, "a").captureRadioObservation()
        DevExportJsonSerializer.observation(obs, DevExportJsonSerializer.metadata(obs.observationId, "b", "test"))
    }
    @Test(expected = IllegalArgumentException::class) fun rawWirelessIdentifierCannotBeExported() {
        val original = read()
        val unsafe = original.copy(wifi = ok(original.wifi.value!!.copy(bssidToken = ok("aa:bb:cc:dd:ee:ff"))))
        val obs = NetworkContextLogger(NetworkReadSource { unsafe }, MonotonicClock { 100 }, "a").captureRadioObservation()
        DevExportJsonSerializer.observation(obs, DevExportJsonSerializer.metadata(obs.observationId, "a", "test"))
    }
    @Test fun jsonEscapesEveryControlCharacter() {
        assertEquals("\"\\u0000\\u000c\\u001f\\\"\\\\\"", DevExportJsonSerializer.json("\u0000\u000c\u001f\"\\"))
    }
}
