package com.omanii.app.network

import com.omanii.app.model.*
import com.omanii.app.network.model.*
import com.omanii.app.network.provider.*
import com.omanii.app.devexport.DevExportJsonSerializer
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class NetworkFixturesReplayTest {
    @Test fun replaySyntheticCasesThroughProductionLoggerAndCompareGoldenExport() {
        val root = File("../../fixtures/network")
        assertTrue(root.isDirectory)
        val lines = mutableListOf<String>()
        var currentCase = ""
        var now = 0L
        var value = read()
        var logger: NetworkContextLogger? = null
        root.resolve("repair-cases.tsv").readLines().filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
            val p = line.split('\t')
            assertEquals(15, p.size)
            val case = p[0]
            now = p[1].toLong()
            if (case != currentCase) {
                currentCase = case
                logger = NetworkContextLogger(NetworkReadSource { value }, MonotonicClock { now }, "synthetic-$case", "synthetic-provider")
            }
            val transport = NetworkTransport.valueOf(p[2])
            value = read(now, transport, p[3], p[10].toBooleanStrict(), p[11].toBooleanStrict(), p[4], p[5].toInt(), p[6],
                if (p[7] == "-") emptyList() else listOf(cell(p[7], p[8].toLong(), now)))
            if (value.wifi.value != null) value = value.copy(wifi = ok(value.wifi.value!!.copy(rssiDbm = ok(p[9].toInt()))))
            if (p[12] != "AVAILABLE") {
                val reason = Availability.valueOf(p[12])
                value = value.copy(path = value.path.copy(transport = absent(reason), networkToken = absent(reason), vpn = absent(reason),
                    internet = absent(reason), validated = absent(reason), metered = absent(reason), underlyingTransports = absent(reason),
                    privateDnsActive = absent(reason), privateDnsMode = absent(reason), pathToken = absent(reason)),
                    wifi = absent(reason), cells = absent(reason), activeDataSubscriptionToken = absent(reason))
            }
            val observation = logger!!.captureRadioObservation()
            assertEquals("$case at $now", "synthetic-provider:${p[13]}", observation.epoch.epochToken)
            assertNull(observation.independence.value)
            assertFalse(observation.timing.ageKnown)
            if (p[14] != "-") assertTrue(logger!!.transitions().last().reasons.contains(TransitionReason.valueOf(p[14])))
            val metadata = DevExportJsonSerializer.metadata(observation.observationId, observation.epoch.sessionId, "synthetic-test-build")
            lines += DevExportJsonSerializer.observation(observation, metadata, true)
        }
        // Candidate output is retained separately for explicit golden review/initial fixture creation.
        val candidate = File("build/task002-replay-candidate.jsonl")
        candidate.parentFile.mkdirs()
        candidate.writeText(lines.joinToString("\n", postfix = "\n"))
        assertEquals(root.resolve("repair-golden.jsonl").readText().replace("\r\n", "\n"), candidate.readText())
    }
}
