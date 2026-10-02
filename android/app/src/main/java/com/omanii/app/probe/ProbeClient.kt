package com.omanii.app.probe

import com.omanii.app.model.Availability
import com.omanii.app.model.MonotonicClock
import com.omanii.app.model.ValueState
import com.omanii.app.session.ContextContinuity
import com.omanii.app.session.ProbeContextHooks
import java.io.IOException
import java.io.InputStream
import java.io.BufferedInputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/** Blocking foreground building block: the session controller chooses when/where to run it. */
class ProbeClient internal constructor(
    private val clock: MonotonicClock,
    private val contextHooks: ProbeContextHooks,
    private val prepareProbeId: () -> String,
    private val createUploadPayloadFiller: () -> (ByteArray) -> Unit,
) : AutoCloseable {
    constructor(clock: MonotonicClock, contextHooks: ProbeContextHooks) : this(
        clock, contextHooks, { UUID.randomUUID().toString() }, { SecureRandom()::nextBytes },
    )
    private val gate = Any()
    private var cancelled = false
    private var running = false
    private var active: Socket? = null
    private var activeProbeId: String? = null
    private var input: InputStream? = null
    private val watchdog = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "omanii-probe-deadline").apply { isDaemon = true }
    }

    fun cancel() {
        val socket = synchronized(gate) { cancelled = true; active }
        socket?.close()
    }
    override fun close() { cancel(); watchdog.shutdownNow() }

    fun run(endpoint: EndpointContext, profile: TestProfile, budget: ByteBudget): List<ProbeObservation> {
        validate(endpoint, profile)
        synchronized(gate) { check(!running) { "One run per client at a time" }; running = true }
        val observations = mutableListOf<ProbeObservation>()
        val profileStart = clock.nowElapsedRealtimeNs()
        var socket: Socket? = null
        try {
            for (sequence in 0 until profile.samples) {
                // Preparation consumes the total profile budget, before HTTP interval timing.
                val probeId = prepareProbeId()
                val start = clock.nowElapsedRealtimeNs()
                val beginContext = contextHooks.begin(probeId, start)
                var outcome = ProbeOutcome.SUCCESS
                var reason: String? = null
                var actual = 0L
                var unconfirmed = 0L
                var health: String? = null
                var validated = false
                var serverUpload: Long? = null
                var connection = ConnectionSemantics.NOT_OPENED
                var timedOut = false
                var lease: ByteReservation? = null
                var alarm: java.util.concurrent.ScheduledFuture<*>? = null
                val remaining = profile.totalTimeoutNanos - (start - profileStart)
                try {
                    if (isCancelled(budget)) throw Stopped()
                    if (remaining <= 0) throw SocketTimeoutException()
                    lease = budget.reserve(profile.requestedBytes + MAX_HEADER_BYTES + MAX_REQUEST_BYTES)
                    if (lease == null) { outcome = ProbeOutcome.BUDGET_EXHAUSTED; reason = "reservation_refused" }
                    else {
                        val timeoutNanos = minOf(profile.sampleTimeoutNanos, remaining)
                        val deadline = start + timeoutNanos
                        alarm = synchronized(gate) {
                            checkWork(budget, deadline)
                            val watchdogDelay = deadline - clock.nowElapsedRealtimeNs()
                            if (watchdogDelay <= 0) throw SocketTimeoutException()
                            activeProbeId = probeId
                            watchdog.schedule({
                                synchronized(gate) {
                                    if (activeProbeId == probeId) { timedOut = true; active?.close() }
                                }
                            }, watchdogDelay, TimeUnit.NANOSECONDS)
                        }
                        if (socket == null || socket.isClosed) {
                            socket = open(endpoint, deadline, budget)
                            connection = ConnectionSemantics.COLD
                        } else connection = ConnectionSemantics.REUSED
                        val owned = socket
                        // One bounded buffer per response; charge bytes at the socket read boundary,
                        // including bytes fetched ahead of parsing, rather than per parsed character.
                        val source = input ?: owned.getInputStream().also { input = it }
                        var readBytes = 0L
                        val incoming = BufferedInputStream(object : InputStream() {
                            override fun read(): Int {
                                val one = ByteArray(1)
                                return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 255
                            }
                            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                                checkWork(budget, deadline)
                                val allowed = profile.requestedBytes + MAX_HEADER_BYTES - readBytes
                                if (allowed <= 0) throw InvalidEndpoint()
                                val count = source.read(buffer, offset, minOf(length.toLong(), allowed).toInt())
                                if (count > 0) { lease.charge(count.toLong()); readBytes += count }
                                return count
                            }
                        }, 1024)
                        checkWork(budget, deadline)
                        val durationMs = ((timeoutNanos + 999_999) / 1_000_000).coerceAtLeast(1)
                        val path = when (profile.type) {
                            ProbeType.RESPONSIVENESS -> "/v1/echo"
                            ProbeType.DOWNLOAD -> "/v1/download?bytes=${profile.requestedBytes}&durationMs=$durationMs"
                            ProbeType.UPLOAD -> "/v1/upload?durationMs=$durationMs"
                        }
                        val request = buildString {
                            append(if (profile.type == ProbeType.UPLOAD) "POST" else "GET")
                            append(" $path HTTP/1.1\r\nHost: ${endpoint.host}:${endpoint.port}\r\n")
                            append("X-Test-Id: $probeId\r\nAccept-Encoding: identity\r\nCache-Control: no-cache\r\n")
                            append("Connection: keep-alive\r\n")
                            if (profile.type == ProbeType.UPLOAD) append("Content-Length: ${profile.requestedBytes}\r\n")
                            append("\r\n")
                        }.toByteArray(StandardCharsets.US_ASCII)
                        check(request.size <= MAX_REQUEST_BYTES)
                        lease.charge(request.size.toLong())
                        owned.getOutputStream().write(request)
                        if (profile.type == ProbeType.UPLOAD) {
                            val fillPayload = createUploadPayloadFiller()
                            while (actual < profile.requestedBytes) {
                                checkWork(budget, deadline)
                                val chunk = ByteArray(minOf(CHUNK_BYTES.toLong(), profile.requestedBytes - actual).toInt())
                                fillPayload(chunk)
                                checkWork(budget, deadline)
                                lease.charge(chunk.size.toLong())
                                unconfirmed = chunk.size.toLong()
                                owned.getOutputStream().write(chunk)
                                actual += chunk.size
                                unconfirmed = 0
                            }
                        }
                        val response = readHeaders(incoming)
                        health = response.headers["x-endpoint-health"]
                        validated = response.headers["x-endpoint-id"] == endpoint.endpointId &&
                            response.headers["x-service-version"] == endpoint.serviceVersion &&
                            response.headers["x-protocol-version"] == endpoint.protocolVersion &&
                            response.headers["x-endpoint-region"] == endpoint.region &&
                            response.headers["x-test-id"] == probeId && health == "healthy"
                        if (!validated || response.status == 503) throw InvalidEndpoint()
                        if (response.status != 200) throw IOException("http_status")
                        if (response.headers.containsKey("transfer-encoding") ||
                            response.headers["content-encoding"]?.let { it != "identity" } == true ||
                            response.headers["cache-control"]?.contains("no-store") != true) throw InvalidEndpoint()
                        val expected = if (profile.type == ProbeType.UPLOAD) 0L else profile.requestedBytes
                        if (response.headers["content-length"]?.toLongOrNull() != expected) throw InvalidEndpoint()
                        if (profile.type == ProbeType.UPLOAD) {
                            serverUpload = response.headers["x-actual-upload-bytes"]?.toLongOrNull()
                            if (serverUpload != actual) throw InvalidEndpoint()
                        } else {
                            val buffer = ByteArray(CHUNK_BYTES)
                            while (actual < expected) {
                                checkWork(budget, deadline)
                                val count = incoming.read(buffer, 0, minOf(buffer.size.toLong(), expected - actual).toInt())
                                if (count < 0) { outcome = ProbeOutcome.PARTIAL; reason = "early_eof"; break }
                                actual += count
                            }
                        }
                        checkWork(budget, deadline)
                        if (response.headers["connection"]?.equals("close", true) == true || outcome != ProbeOutcome.SUCCESS) {
                            owned.close(); socket = null; input = null
                        }
                    }
                } catch (_: Stopped) { outcome = ProbeOutcome.CANCELLED; reason = "cancelled" }
                catch (_: InvalidEndpoint) { outcome = ProbeOutcome.SERVER_INVALID; reason = "endpoint_invalid" }
                catch (_: SocketTimeoutException) {
                    outcome = if (isCancelled(budget)) ProbeOutcome.CANCELLED else ProbeOutcome.TIMEOUT
                    reason = if (outcome == ProbeOutcome.CANCELLED) "cancelled" else "deadline_or_io_timeout"
                }
                catch (_: IOException) {
                    outcome = when {
                        isCancelled(budget) -> ProbeOutcome.CANCELLED
                        synchronized(gate) { timedOut } || clock.nowElapsedRealtimeNs() - start >= minOf(profile.sampleTimeoutNanos, remaining) -> ProbeOutcome.TIMEOUT
                        else -> ProbeOutcome.ERROR
                    }
                    reason = when (outcome) { ProbeOutcome.TIMEOUT -> "deadline_or_io_timeout"; ProbeOutcome.CANCELLED -> "cancelled"; else -> "request_failure" }
                } finally {
                    synchronized(gate) { activeProbeId = null }
                    alarm?.cancel(false)
                    if (outcome != ProbeOutcome.SUCCESS) { socket?.close(); socket = null; input = null }
                    lease?.close()
                }
                val end = clock.nowElapsedRealtimeNs()
                val reportedSpan = contextHooks.end(probeId, end)
                val span = if (reportedSpan.movementSpanM.value == null && reportedSpan.movementSpanM.reason == null) {
                    reportedSpan.copy(movementSpanM = ValueState(null, reportedSpan.movementSpanM.availability,
                        "movement_reason_not_supplied"))
                } else reportedSpan
                val safe = outcome == ProbeOutcome.SUCCESS && validated &&
                    span.continuity == ContextContinuity.CONTINUOUS && span.boundaries.isEmpty() &&
                    span.start == beginContext && span.start.atElapsedRealtimeNs == start && span.end.atElapsedRealtimeNs == end &&
                    span.start.sessionId == span.end.sessionId &&
                    span.start.segment.availability == Availability.AVAILABLE && span.start.segment.value != null &&
                    span.start.segment.value == span.end.segment.value &&
                    span.start.networkEpoch.availability == Availability.AVAILABLE && span.start.networkEpoch.value != null &&
                    span.start.networkEpoch.value == span.end.networkEpoch.value &&
                    span.start.coordinateFrame == span.end.coordinateFrame &&
                    span.start.coordinateFrame.availability in setOf(Availability.AVAILABLE, Availability.NOT_APPLICABLE)
                observations += ProbeObservation(
                    probeId, sequence, "probe-wave1-alpha-1", endpoint, profile, start, end, clock.nowElapsedRealtimeNs(), end,
                    profile.requestedBytes, actual, lease?.chargedBytes ?: 0, unconfirmed,
                    outcome, reason, health, validated, serverUpload, beginContext, span, safe, connection, "NOT_EVALUATED",
                    if (profile.type == ProbeType.RESPONSIVENESS && outcome == ProbeOutcome.SUCCESS) end - start else null,
                    profile.type != ProbeType.RESPONSIVENESS,
                )
                if (outcome == ProbeOutcome.CANCELLED || outcome == ProbeOutcome.BUDGET_EXHAUSTED ||
                    outcome == ProbeOutcome.SERVER_INVALID || clock.nowElapsedRealtimeNs() - profileStart >= profile.totalTimeoutNanos) break
            }
        } finally {
            socket?.close()
            synchronized(gate) { active = null; activeProbeId = null; input = null; running = false }
        }
        return observations
    }

    private fun isCancelled(budget: ByteBudget): Boolean = synchronized(gate) { cancelled } || budget.snapshot().cancelled
    private fun checkWork(budget: ByteBudget, deadline: Long) {
        if (isCancelled(budget)) throw Stopped()
        if (clock.nowElapsedRealtimeNs() >= deadline) throw SocketTimeoutException()
    }
    private fun open(endpoint: EndpointContext, deadline: Long, budget: ByteBudget): Socket {
        checkWork(budget, deadline)
        val raw = Socket()
        synchronized(gate) {
            try { checkWork(budget, deadline) } catch (error: IOException) { raw.close(); throw error }
            active = raw
        }
        try {
            checkWork(budget, deadline)
            val timeoutNanos = deadline - clock.nowElapsedRealtimeNs()
            if (timeoutNanos <= 0) throw SocketTimeoutException()
            val timeoutMs = ((timeoutNanos + 999_999) / 1_000_000).toInt().coerceAtLeast(1)
            raw.soTimeout = timeoutMs
            raw.tcpNoDelay = true
            raw.connect(InetSocketAddress(endpoint.address, endpoint.port), timeoutMs)
            if (!endpoint.tls) return raw
            val ssl = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                .createSocket(raw, endpoint.host, endpoint.port, true) as SSLSocket
            synchronized(gate) {
                if (cancelled || budget.snapshot().cancelled) { ssl.close(); throw Stopped() }
                active = ssl
            }
            ssl.soTimeout = timeoutMs
            ssl.sslParameters = SSLParameters().apply { endpointIdentificationAlgorithm = "HTTPS" }
            ssl.startHandshake()
            return ssl
        } catch (error: Exception) { raw.close(); throw error }
    }

    private data class Response(val status: Int, val headers: Map<String, String>)
    private fun readHeaders(incoming: InputStream): Response {
        val text = StringBuilder()
        while (!text.endsWith("\r\n\r\n")) {
            if (text.length >= MAX_HEADER_BYTES) throw InvalidEndpoint()
            val byte = incoming.read()
            if (byte < 0) throw IOException("header_eof")
            text.append(byte.toChar())
        }
        val lines = text.toString().split("\r\n")
        val status = Regex("HTTP/1\\.[01] ([0-9]{3})(?: .*)?").matchEntire(lines.first())
            ?.groupValues?.get(1)?.toInt() ?: throw InvalidEndpoint()
        val headers = mutableMapOf<String, String>()
        for (line in lines.drop(1).filter { it.isNotEmpty() }) {
            val split = line.indexOf(':')
            if (split < 1) throw InvalidEndpoint()
            val key = line.substring(0, split).lowercase(java.util.Locale.ROOT)
            if (headers.put(key, line.substring(split + 1).trim()) != null) throw InvalidEndpoint()
        }
        return Response(status, headers)
    }

    private fun validate(endpoint: EndpointContext, profile: TestProfile) {
        require(endpoint.port in 1..65535 && Regex("[A-Za-z0-9.-]{1,253}").matches(endpoint.host))
        require(endpoint.tls || endpoint.address.isLoopbackAddress) { "Cleartext experiments are loopback only" }
        require(profile.name in setOf("low_data_responsiveness", "verification_low_data", "deliberate_benchmark"))
        require(profile.version.isNotBlank() && profile.samples in 1..64)
        require(profile.sampleTimeoutNanos in 1_000_000..5_000_000_000L)
        require(profile.totalTimeoutNanos in profile.sampleTimeoutNanos..30_000_000_000L)
        require(profile.requestedBytes in 1..1_048_576)
        require(profile.type != ProbeType.RESPONSIVENESS || profile.requestedBytes == 1L)
        require(profile.name != "low_data_responsiveness" || profile.type == ProbeType.RESPONSIVENESS)
    }
    private class Stopped : IOException()
    private class InvalidEndpoint : IOException()
    companion object {
        const val MAX_HEADER_BYTES = 8192
        const val MAX_REQUEST_BYTES = 1024
        const val CHUNK_BYTES = 16384
    }
}
