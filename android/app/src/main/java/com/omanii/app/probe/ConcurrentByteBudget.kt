package com.omanii.app.probe

/** Caller shares one instance across all probes in the selected session/profile. */
class ConcurrentByteBudget(override val budgetId: String, private val totalBytes: Long) : ByteBudget {
    private var consumed = 0L
    private var reserved = 0L
    private var cancelled = false
    init { require(totalBytes >= 0); require(budgetId.isNotBlank()) }

    @Synchronized override fun reserve(maximumBytes: Long): ByteReservation? {
        require(maximumBytes > 0)
        if (cancelled || maximumBytes > totalBytes - consumed - reserved) return null
        reserved += maximumBytes
        return Lease(maximumBytes)
    }
    @Synchronized override fun snapshot() = ByteBudgetSnapshot(
        budgetId, totalBytes, consumed, reserved, consumed == totalBytes, cancelled,
    )
    @Synchronized override fun cancel() { cancelled = true }

    private inner class Lease(private val maximum: Long) : ByteReservation {
        private var charged = 0L
        private var closed = false
        override val chargedBytes: Long get() = synchronized(this@ConcurrentByteBudget) { charged }
        override fun charge(bytes: Long) = synchronized(this@ConcurrentByteBudget) {
            check(!closed)
            require(bytes >= 0 && bytes <= maximum - charged)
            charged += bytes
            consumed += bytes
            reserved -= bytes
        }
        override fun close() = synchronized(this@ConcurrentByteBudget) {
            if (!closed) { reserved -= maximum - charged; closed = true }
        }
    }
}
