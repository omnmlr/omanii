package com.omanii.app.probe

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ConcurrentByteBudgetTest {
    @Test fun concurrentReservationsCannotDoubleSpendRemainingBudget() {
        val budget = ConcurrentByteBudget("session", 100)
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1); val reserved = CountDownLatch(8); val release = CountDownLatch(1)
        try {
            val work = (1..8).map { executor.submit<Boolean> {
                start.await(); val lease = budget.reserve(40); reserved.countDown(); release.await()
                lease?.use { it.charge(35) }; lease != null
            } }
            start.countDown(); assertTrue(reserved.await(1, TimeUnit.SECONDS))
            assertEquals(80, budget.snapshot().reservedBytes)
            release.countDown()
            assertEquals(2, work.count { it.get(1, TimeUnit.SECONDS) })
            assertEquals(70, budget.snapshot().consumedBytes); assertEquals(0, budget.snapshot().reservedBytes)
            assertNull(budget.reserve(31))
        } finally { release.countDown(); executor.shutdownNow() }
    }
    @Test fun releasesUnusedReservationAndRejectsOverchargeAndDoubleClose() {
        val budget = ConcurrentByteBudget("bounded", 100)
        val lease = budget.reserve(80)!!
        lease.charge(20)
        assertThrows(IllegalArgumentException::class.java) { lease.charge(61) }
        lease.close(); lease.close()
        assertEquals(20, budget.snapshot().consumedBytes); assertEquals(0, budget.snapshot().reservedBytes)
        assertThrows(IllegalStateException::class.java) { lease.charge(1) }
        budget.reserve(80)!!.use { it.charge(80) }
        assertTrue(budget.snapshot().exhausted)
    }
    @Test fun cancellationPersistsAndInflightAccountingStillSettles() {
        val budget = ConcurrentByteBudget("cancel", 100)
        val lease = budget.reserve(80)!!; budget.cancel()
        assertNull(budget.reserve(1)); assertTrue(budget.snapshot().cancelled)
        lease.charge(10); lease.close()
        assertEquals(10, budget.snapshot().consumedBytes); assertEquals(0, budget.snapshot().reservedBytes)
    }
}
