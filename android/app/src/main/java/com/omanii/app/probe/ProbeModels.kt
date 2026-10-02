package com.omanii.app.probe

import com.omanii.app.session.ContextSnapshot
import com.omanii.app.session.ProbeIntervalContext
import java.net.InetAddress

// Task-owned execution/HTTP DTOs. Shared context/time meaning remains with the Wave 1 contracts.
enum class ProbeType { RESPONSIVENESS, DOWNLOAD, UPLOAD }
enum class ProbeOutcome { SUCCESS, TIMEOUT, ERROR, CANCELLED, SERVER_INVALID, PARTIAL, BUDGET_EXHAUSTED }
enum class ConnectionSemantics { COLD, REUSED, NOT_OPENED }

data class EndpointContext(
    val endpointId: String, val serviceVersion: String, val region: String,
    val protocolVersion: String, val host: String, val port: Int,
    val address: InetAddress, val tls: Boolean,
)
data class TestProfile(
    val name: String, val version: String, val type: ProbeType, val samples: Int,
    val requestedBytes: Long, val sampleTimeoutNanos: Long, val totalTimeoutNanos: Long,
)
data class ProbeObservation(
    val probeId: String, val sequence: Int, val schemaVersion: String,
    val endpoint: EndpointContext, val profile: TestProfile,
    val startNanos: Long, val endNanos: Long, val receivedAtNanos: Long,
    val measurementAtNanos: Long?, val requestedBytes: Long, val actualBytes: Long,
    val httpBytesCharged: Long, val unconfirmedUploadBytes: Long,
    val outcome: ProbeOutcome, val reason: String?, val endpointHealth: String?,
    val endpointValidated: Boolean, val serverUploadBytes: Long?,
    val beginContext: ContextSnapshot, val context: ProbeIntervalContext, val comparisonSafe: Boolean,
    val connection: ConnectionSemantics, val concurrentLoad: String,
    val httpApplicationRttNanos: Long?, val lowerBound: Boolean,
)
data class ByteBudgetSnapshot(
    val budgetId: String, val totalBytes: Long, val consumedBytes: Long,
    val reservedBytes: Long, val exhausted: Boolean, val cancelled: Boolean,
)
interface ByteReservation : AutoCloseable {
    fun charge(bytes: Long)
    val chargedBytes: Long
    override fun close()
}
interface ByteBudget {
    val budgetId: String
    fun reserve(maximumBytes: Long): ByteReservation?
    fun snapshot(): ByteBudgetSnapshot
    fun cancel()
}
