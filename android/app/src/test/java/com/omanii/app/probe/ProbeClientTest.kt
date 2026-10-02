package com.omanii.app.probe

import com.omanii.app.model.*
import com.omanii.app.session.ProbeContextHooks
import com.omanii.app.time.MonotonicClock
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.InetSocketAddress
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class ProbeClientTest {
    private val clock = MonotonicClock { System.nanoTime() }
    private val probeSequence = AtomicLong()
    private fun probeClient(
        receiveClock: MonotonicClock = clock,
        contextHooks: ProbeContextHooks = hooks,
        prepareId: () -> String = { "synthetic-${probeSequence.incrementAndGet()}" },
        payloadFiller: () -> (ByteArray) -> Unit = { java.util.Random(0)::nextBytes },
    ) = ProbeClient(receiveClock, contextHooks, prepareId, payloadFiller)
    private val ref = ProbeContextRef("segment-a", "epoch-a")
    private val hooks = object : ProbeContextHooks {
        override fun begin(startNanos: Long): Any = startNanos
        override fun end(token: Any, endNanos: Long) = ProbeContextSpan(ref, ref, ContextContinuity.CONTINUOUS, emptyList())
    }
    private fun endpoint(port: Int) = EndpointContext("omanii-local-alpha", "0.1.0", "local-private",
        "http-probe-alpha-1", "localhost", port, InetAddress.getByName("127.0.0.1"), false)
    private fun profile(type: ProbeType = ProbeType.RESPONSIVENESS, bytes: Long = 1, samples: Int = 1,
        timeout: Long = 1_000_000_000) = TestProfile(
        if (type == ProbeType.RESPONSIVENESS) "low_data_responsiveness" else "verification_low_data",
        "experimental-1", type, samples, bytes, timeout, maxOf(timeout, 5_000_000_000),
    )

    private fun withNode(block: (EndpointContext) -> Unit) {
        val root = System.getProperty("omanii.repo") ?: generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .firstOrNull { File(it, "measurement-server/test/android-endpoint.ts").isFile && File(it, "fixtures/probe/observations.json").isFile }
            ?.absolutePath ?: error("Cannot locate TASK-004 repository")
        val process = ProcessBuilder("node", "$root/measurement-server/test/android-endpoint.ts").start()
        val readiness = Executors.newSingleThreadExecutor()
        try {
            val reader = process.inputStream.bufferedReader()
            val line = readiness.submit<String?> { reader.readLine() }.get(5, TimeUnit.SECONDS)
                ?: error(process.errorStream.bufferedReader().readText())
            block(endpoint(line.toInt()))
        } finally {
            process.destroy(); if (!process.waitFor(1, TimeUnit.SECONDS)) process.destroyForcibly()
            readiness.shutdownNow()
        }
    }
    private fun readRequest(socket: Socket): String {
        val text = StringBuilder()
        while (!text.endsWith("\r\n\r\n")) {
            val byte = socket.getInputStream().read(); check(byte >= 0); text.append(byte.toChar())
        }
        return text.toString()
    }
    private fun headers(request: String, length: Int, health: String = "healthy", status: Int = 200, version: String = "0.1.0") =
        "HTTP/1.1 $status OK\r\nContent-Length: $length\r\nCache-Control: no-store\r\n" +
        "X-Endpoint-Id: omanii-local-alpha\r\nX-Service-Version: $version\r\n" +
        "X-Endpoint-Region: local-private\r\nX-Protocol-Version: http-probe-alpha-1\r\n" +
        "X-Endpoint-Health: $health\r\n" + request.lines().first { it.startsWith("X-Test-Id:") }.trim() + "\r\n\r\n"
    private fun fake(handler: (Socket) -> Unit, block: (EndpointContext) -> Unit) {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val worker = Executors.newSingleThreadExecutor()
        val task = worker.submit { server.accept().use(handler) }
        try { block(endpoint(server.localPort)); task.get(2, TimeUnit.SECONDS) }
        finally { server.close(); worker.shutdownNow() }
    }

    @Test fun warmSamplesReuseConnectionAndAccumulateActualBytesAcrossRuns() = withNode { endpoint ->
        val budget = ConcurrentByteBudget("shared", 100_000)
        val client = probeClient()
        client.use {
            val observations = it.run(endpoint, profile(samples = 3), budget)
            assertEquals(3, observations.size)
            assertTrue(observations.all { r -> r.outcome == ProbeOutcome.SUCCESS && r.actualBytes == 1L })
            assertEquals(listOf(ConnectionSemantics.COLD, ConnectionSemantics.REUSED, ConnectionSemantics.REUSED), observations.map { r -> r.connection })
            assertTrue(observations.all { r -> r.httpApplicationRttNanos == r.endNanos - r.startNanos && r.receivedAtNanos >= r.endNanos && r.measurementAtNanos == r.endNanos })
            val next = it.run(endpoint, profile(), budget)
            assertEquals((observations + next).sumOf { r -> r.httpBytesCharged }, budget.snapshot().consumedBytes)
            assertEquals(0, budget.snapshot().reservedBytes)
        }
    }
    @Test fun boundedDownloadAndUploadAgainstTypescriptService() = withNode { endpoint ->
        val budget = ConcurrentByteBudget("transfers", 200_000)
        probeClient().use {
            val down = it.run(endpoint, profile(ProbeType.DOWNLOAD, 65536), budget).single()
            val up = it.run(endpoint, profile(ProbeType.UPLOAD, 65536), budget).single()
            assertEquals(ProbeOutcome.SUCCESS, down.outcome); assertEquals(65536, down.actualBytes)
            assertEquals(ProbeOutcome.SUCCESS, up.outcome); assertEquals(65536, up.actualBytes)
            assertEquals(up.actualBytes, up.serverUploadBytes)
            assertTrue(down.lowerBound && up.lowerBound && down.httpApplicationRttNanos == null)
            assertEquals(down.httpBytesCharged + up.httpBytesCharged, budget.snapshot().consumedBytes)
        }
    }
    @Test fun budgetRefusalProducesNoConnectionAndStopsSamples() {
        probeClient().use {
            val result = it.run(endpoint(1), profile(samples = 4), ConcurrentByteBudget("tiny", 1))
            assertEquals(1, result.size)
            assertEquals(ProbeOutcome.BUDGET_EXHAUSTED, result.single().outcome)
            assertEquals(ConnectionSemantics.NOT_OPENED, result.single().connection)
            assertEquals(0, result.single().actualBytes)
        }
    }
    @Test fun timeoutIsDistinctAndRetainsPartialBytes() = fake({ socket ->
        val request = readRequest(socket)
        socket.getOutputStream().write((headers(request, 65536) + "abc").toByteArray())
        Thread.sleep(300)
    }) { endpoint ->
        probeClient().use {
            val result = it.run(endpoint, profile(ProbeType.DOWNLOAD, 65536, timeout = 80_000_000), ConcurrentByteBudget("timeout", 100_000)).single()
            assertEquals(ProbeOutcome.TIMEOUT, result.outcome)
            assertEquals(3, result.actualBytes); assertTrue(result.lowerBound)
            assertNull(result.httpApplicationRttNanos)
        }
    }
    @Test fun connectionFailureIsErrorAndNeverPacketLoss() {
        val port = ServerSocket(0).use { it.localPort }
        probeClient().use {
            val result = it.run(endpoint(port), profile(), ConcurrentByteBudget("failure", 100_000)).single()
            assertEquals(ProbeOutcome.ERROR, result.outcome)
            assertNull(result.httpApplicationRttNanos)
            assertFalse(ProbeObservation::class.java.declaredFields.any { field -> field.name.contains("loss", true) })
        }
    }
    @Test fun earlyEndReturnsPartialLowerBound() = fake({ socket ->
        val request = readRequest(socket); socket.getOutputStream().write((headers(request, 65536) + "abc").toByteArray())
    }) { endpoint ->
        probeClient().use {
            val result = it.run(endpoint, profile(ProbeType.DOWNLOAD, 65536), ConcurrentByteBudget("partial", 100_000)).single()
            assertEquals(ProbeOutcome.PARTIAL, result.outcome); assertEquals(3, result.actualBytes)
            assertEquals("early_eof", result.reason); assertTrue(result.lowerBound)
        }
    }
    @Test fun overloadAndVersionMismatchInvalidate() {
        for ((health, status, version) in listOf(Triple("invalid", 503, "0.1.0"), Triple("healthy", 200, "future"))) {
            fake({ socket ->
                val request = readRequest(socket); socket.getOutputStream().write(headers(request, 1, health, status, version).toByteArray())
            }) { endpoint ->
                probeClient().use {
                    val result = it.run(endpoint, profile(samples = 4), ConcurrentByteBudget("invalid", 100_000))
                    assertEquals(1, result.size); assertEquals(ProbeOutcome.SERVER_INVALID, result.single().outcome)
                    assertFalse(result.single().endpointValidated)
                }
            }
        }
    }
    @Test fun boundariesPreserveAToBToAEvenWhenFinalContextMatchesStart() = fake({ socket ->
        val request = readRequest(socket)
        socket.getOutputStream().write((headers(request, 4) + "ab").toByteArray())
        Thread.sleep(2100)
        socket.getOutputStream().write("cd".toByteArray())
    }) { endpoint ->
        val transitions = object : ProbeContextHooks {
            override fun begin(startNanos: Long): Any = startNanos
            override fun end(token: Any, endNanos: Long) = ProbeContextSpan(ref, ref, ContextContinuity.BROKEN,
                listOf(token as Long + 1, endNanos - 1), 2.0)
        }
        probeClient(contextHooks = transitions).use {
            val result = it.run(endpoint, profile(ProbeType.DOWNLOAD, 4, timeout = 3_000_000_000), ConcurrentByteBudget("movement", 100_000)).single()
            assertEquals(result.toString(), ProbeOutcome.SUCCESS, result.outcome); assertFalse(result.comparisonSafe)
            assertEquals(2, result.context.boundaryNanos.size); assertTrue(result.endNanos - result.startNanos >= 2_000_000_000)
        }
    }
    @Test fun unknownAndUnevaluatedContinuityNeverSafe() = withNode { endpoint ->
        for (continuity in listOf(ContextContinuity.UNKNOWN, ContextContinuity.NOT_EVALUATED)) {
            val unknown = object : ProbeContextHooks {
                override fun begin(startNanos: Long): Any = startNanos
                override fun end(token: Any, endNanos: Long) = ProbeContextSpan(ref, ref, continuity, emptyList())
            }
            probeClient(contextHooks = unknown).use {
                assertFalse(it.run(endpoint, profile(), ConcurrentByteBudget("unknown", 100_000)).single().comparisonSafe)
            }
        }
    }
    @Test fun cancellationClosesBlockedReadWithin500msNoNewSamplesOrPayloadFor300ms() {
        val begun = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val payload = AtomicLong()
        fake({ socket ->
            val request = readRequest(socket)
            socket.getOutputStream().write(headers(request, 1048576).toByteArray())
            socket.getOutputStream().write(ByteArray(100)); payload.addAndGet(100); begun.countDown()
            socket.soTimeout = 1000
            assertEquals(-1, socket.getInputStream().read()); closed.countDown()
            val atClose = payload.get(); Thread.sleep(300); assertEquals(atClose, payload.get())
        }) { endpoint ->
            probeClient().use { client ->
                val executor = Executors.newSingleThreadExecutor()
                try {
                    val run = executor.submit<List<ProbeObservation>> { client.run(endpoint, profile(ProbeType.DOWNLOAD, 1048576, 5), ConcurrentByteBudget("cancel", 6_000_000)) }
                    assertTrue(begun.await(1, TimeUnit.SECONDS))
                    val before = System.nanoTime(); client.cancel()
                    val records = run.get(500, TimeUnit.MILLISECONDS)
                    assertTrue(closed.await(500, TimeUnit.MILLISECONDS))
                    assertTrue(System.nanoTime() - before < 500_000_000)
                    assertEquals(1, records.size); assertEquals(ProbeOutcome.CANCELLED, records.single().outcome)
                    assertEquals(ProbeOutcome.CANCELLED, client.run(endpoint, profile(), ConcurrentByteBudget("after", 100_000)).single().outcome)
                } finally { executor.shutdownNow() }
            }
        }
    }
    @Test fun cancelBeforeRunAndCancelledBudgetOpenNoConnection() {
        probeClient().use { client ->
            client.cancel()
            assertEquals(ProbeOutcome.CANCELLED, client.run(endpoint(1), profile(), ConcurrentByteBudget("pre", 100_000)).single().outcome)
        }
        val budget = ConcurrentByteBudget("cancelled", 100_000); budget.cancel()
        probeClient().use { client ->
            assertEquals(ProbeOutcome.CANCELLED, client.run(endpoint(1), profile(), budget).single().outcome)
        }
    }
    @Test fun malformedFramingAndOversizedHeadersInvalidated() {
        for (extra in listOf("Transfer-Encoding: chunked\r\n", "Content-Encoding: gzip\r\n", "X-Padding: ${"a".repeat(9000)}\r\n")) {
            fake({ socket ->
                val request = readRequest(socket)
                val text = headers(request, 1).replace("\r\n\r\n", "\r\n$extra\r\n")
                runCatching { socket.getOutputStream().write((text + "a").toByteArray()) }
            }) { endpoint ->
                val budget = ConcurrentByteBudget("bad", 100_000)
                probeClient().use {
                    assertEquals(ProbeOutcome.SERVER_INVALID, it.run(endpoint, profile(), budget).single().outcome)
                    assertTrue(budget.snapshot().consumedBytes <= 9217)
                }
            }
        }
    }
    @Test fun rejectsUnboundedProfileAndRemoteCleartextBeforeOpening() {
        probeClient().use { client ->
            for (invalid in listOf(profile().copy(samples = 65), profile().copy(sampleTimeoutNanos = 0), profile().copy(requestedBytes = Long.MAX_VALUE))) {
                assertThrows(IllegalArgumentException::class.java) { client.run(endpoint(1), invalid, ConcurrentByteBudget("invalid", 100_000)) }
            }
            assertThrows(IllegalArgumentException::class.java) {
                client.run(endpoint(1).copy(address = InetAddress.getByAddress(byteArrayOf(192.toByte(), 0, 2, 1))), profile(), ConcurrentByteBudget("remote", 100_000))
            }
        }
    }
    @Test fun concurrentClientsShareOneBudgetWithoutDoubleSpending() = withNode { endpoint ->
        val budget = ConcurrentByteBudget("both", 80_000)
        val pool = Executors.newFixedThreadPool(2)
        val begun = CountDownLatch(1)
        try {
            val runs = (1..2).map { pool.submit<List<ProbeObservation>> {
                begun.await()
                probeClient().use { it.run(endpoint, profile(ProbeType.DOWNLOAD, 65536), budget) }
            } }
            begun.countDown()
            val results = runs.flatMap { it.get(2, TimeUnit.SECONDS) }
            assertEquals(1, results.count { it.outcome == ProbeOutcome.SUCCESS })
            assertEquals(1, results.count { it.outcome == ProbeOutcome.BUDGET_EXHAUSTED })
            assertEquals(results.sumOf { it.httpBytesCharged }, budget.snapshot().consumedBytes)
            assertTrue(budget.snapshot().consumedBytes <= 80_000); assertEquals(0, budget.snapshot().reservedBytes)
        } finally { pool.shutdownNow() }
    }
    @Test fun closeRacingReservationReturnsCancellationWithoutExecutorRejection() {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val delegate = ConcurrentByteBudget("race", 100_000)
        val budget = object : ByteBudget by delegate {
            override fun reserve(maximumBytes: Long): ByteReservation? {
                entered.countDown(); release.await(); return delegate.reserve(maximumBytes)
            }
        }
        val client = probeClient()
        val pool = Executors.newSingleThreadExecutor()
        try {
            val run = pool.submit<List<ProbeObservation>> { client.run(endpoint(1), profile(), budget) }
            assertTrue(entered.await(1, TimeUnit.SECONDS)); client.close(); release.countDown()
            assertEquals(ProbeOutcome.CANCELLED, run.get(500, TimeUnit.MILLISECONDS).single().outcome)
            assertEquals(0, budget.snapshot().reservedBytes)
        } finally { release.countDown(); client.close(); pool.shutdownNow() }
    }
    @Test fun stalledUploadCancellationClosesActiveWriteAndPeerWithin500ms() {
        val server = ServerSocket().apply {
            receiveBufferSize = 1024
            bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0))
        }
        val begun = CountDownLatch(1); val cancelled = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val peer = pool.submit<Long> {
                server.accept().use { socket ->
                    readRequest(socket); begun.countDown(); cancelled.await()
                    socket.soTimeout = 500
                    var bytes = 0L; val buffer = ByteArray(16384)
                    while (true) { val count = socket.getInputStream().read(buffer); if (count < 0) break; bytes += count }
                    bytes
                }
            }
            probeClient().use { client ->
                val budget = ConcurrentByteBudget("upload-cancel", 2_000_000)
                val run = pool.submit<List<ProbeObservation>> { client.run(endpoint(server.localPort), profile(ProbeType.UPLOAD, 1048576, 3), budget) }
                assertTrue(begun.await(1, TimeUnit.SECONDS)); Thread.sleep(100)
                val before = System.nanoTime(); client.cancel(); cancelled.countDown()
                val record = run.get(500, TimeUnit.MILLISECONDS).single()
                assertEquals(ProbeOutcome.CANCELLED, record.outcome)
                assertTrue(System.nanoTime() - before < 500_000_000)
                assertTrue(peer.get(500, TimeUnit.MILLISECONDS) <= record.actualBytes + record.unconfirmedUploadBytes)
                val bytes = budget.snapshot().consumedBytes; Thread.sleep(300)
                assertEquals(bytes, budget.snapshot().consumedBytes); assertEquals(0, budget.snapshot().reservedBytes)
            }
        } finally { cancelled.countDown(); server.close(); pool.shutdownNow() }
    }

    private fun assertNoConnection(server: ServerSocket) {
        server.soTimeout = 100
        assertThrows(java.net.SocketTimeoutException::class.java) { server.accept().use { error("Unexpected probe socket") } }
    }
    @Test fun slowPreparationConsumesTotalProfileBudgetAndOpensNoSocket() {
        val now = AtomicLong(1_000)
        val selected = profile(samples = 3)
        val budget = ConcurrentByteBudget("slow-preparation", 100_000)
        var prepared = 0
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            probeClient(receiveClock = MonotonicClock { now.get() }, prepareId = {
                prepared++
                now.addAndGet(selected.totalTimeoutNanos)
                "synthetic-slow"
            }).use { client ->
                val record = client.run(endpoint(server.localPort), selected, budget).single()
                assertEquals(ProbeOutcome.TIMEOUT, record.outcome)
                assertEquals(ConnectionSemantics.NOT_OPENED, record.connection)
                assertEquals(0, record.actualBytes); assertEquals(0, record.httpBytesCharged)
                assertEquals(1, prepared); assertEquals(0, budget.snapshot().reservedBytes)
                assertNoConnection(server)
            }
        }
    }
    @Test fun cancellationDuringPreparationOpensNoSocketOrLaterSamples() {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        val budget = ConcurrentByteBudget("prepare-cancel", 100_000)
        var prepared = 0
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            probeClient(prepareId = {
                prepared++; entered.countDown(); release.await(); "synthetic-cancel"
            }).use { client ->
                try {
                    val run = pool.submit<List<ProbeObservation>> { client.run(endpoint(server.localPort), profile(samples = 3), budget) }
                    assertTrue(entered.await(1, TimeUnit.SECONDS))
                    client.cancel(); release.countDown()
                    val record = run.get(500, TimeUnit.MILLISECONDS).single()
                    assertEquals(ProbeOutcome.CANCELLED, record.outcome)
                    assertEquals(ConnectionSemantics.NOT_OPENED, record.connection)
                    assertEquals(1, prepared); assertEquals(0, budget.snapshot().consumedBytes)
                    assertNoConnection(server)
                } finally { release.countDown(); pool.shutdownNow() }
            }
        }
    }
    @Test fun totalBudgetExhaustedDuringReservationOpensNoSocketAndReleasesLease() {
        val now = AtomicLong(1_000)
        val selected = profile(samples = 3)
        val delegate = ConcurrentByteBudget("reserve-timeout", 100_000)
        val budget = object : ByteBudget by delegate {
            override fun reserve(maximumBytes: Long): ByteReservation? {
                now.addAndGet(selected.totalTimeoutNanos)
                return delegate.reserve(maximumBytes)
            }
        }
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            probeClient(receiveClock = MonotonicClock { now.get() }).use { client ->
                val record = client.run(endpoint(server.localPort), selected, budget).single()
                assertEquals(ProbeOutcome.TIMEOUT, record.outcome)
                assertEquals(ConnectionSemantics.NOT_OPENED, record.connection)
                assertEquals(0, record.httpBytesCharged); assertEquals(0, budget.snapshot().reservedBytes)
                assertNoConnection(server)
            }
        }
    }
    @Test fun laterSlowPreparationClosesWarmSocketAndSchedulesNoSecondRequestOrConnection() {
        val offset = AtomicLong()
        val selected = profile(samples = 3)
        var prepared = 0
        ServerSocket(0, 2, InetAddress.getByName("127.0.0.1")).use { server ->
            val worker = Executors.newSingleThreadExecutor()
            val peer = worker.submit {
                server.accept().use { socket ->
                    val request = readRequest(socket)
                    socket.getOutputStream().write((headers(request, 1) + "o").toByteArray())
                    socket.soTimeout = 500
                    assertEquals(-1, socket.getInputStream().read())
                }
            }
            try {
                val budget = ConcurrentByteBudget("later-timeout", 100_000)
                probeClient(receiveClock = MonotonicClock { System.nanoTime() + offset.get() }, prepareId = {
                    prepared++
                    if (prepared == 2) offset.addAndGet(selected.totalTimeoutNanos)
                    "synthetic-$prepared"
                }).use { client ->
                    val records = client.run(endpoint(server.localPort), selected, budget)
                    assertEquals(listOf(ProbeOutcome.SUCCESS, ProbeOutcome.TIMEOUT), records.map { it.outcome })
                    assertEquals(ConnectionSemantics.NOT_OPENED, records.last().connection)
                    assertEquals(0, records.last().httpBytesCharged)
                    assertEquals(2, prepared); assertEquals(0, budget.snapshot().reservedBytes)
                    peer.get(500, TimeUnit.MILLISECONDS)
                    assertNoConnection(server)
                }
            } finally { server.close(); worker.shutdownNow() }
        }
    }
    @Test fun slowUploadPayloadPreparationTimesOutBeforeSendingBody() = fake({ socket ->
        readRequest(socket)
        socket.soTimeout = 500
        assertEquals(-1, socket.getInputStream().read())
    }) { endpoint ->
        val offset = AtomicLong()
        val selected = profile(ProbeType.UPLOAD, 65536)
        val budget = ConcurrentByteBudget("payload-timeout", 100_000)
        probeClient(receiveClock = MonotonicClock { System.nanoTime() + offset.get() }, payloadFiller = {
            { _: ByteArray -> offset.addAndGet(selected.totalTimeoutNanos); Unit }
        }).use { client ->
            val record = client.run(endpoint, selected, budget).single()
            assertEquals(ProbeOutcome.TIMEOUT, record.outcome)
            assertEquals(0, record.actualBytes); assertEquals(0, record.unconfirmedUploadBytes)
            assertTrue(record.httpBytesCharged > 0) // Request headers were sent before body preparation.
            assertEquals(0, budget.snapshot().reservedBytes)
        }
    }
}
