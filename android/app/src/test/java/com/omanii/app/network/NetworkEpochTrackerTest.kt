package com.omanii.app.network

import com.omanii.app.network.epoch.NetworkEpochTracker
import com.omanii.app.network.model.*
import com.omanii.app.model.*
import org.junit.Assert.*
import org.junit.Test

class NetworkEpochTrackerTest {
    private fun boundary(before: NetworkRead, after: NetworkRead, reason: TransitionReason) {
        val tracker = NetworkEpochTracker("s")
        tracker.evaluate(before, 10)
        val event = requireNotNull(tracker.evaluate(after, 20))
        assertTrue("Missing $reason in ${event.reasons}", reason in event.reasons)
        assertNotEquals(event.previous, event.next)
        assertEquals("s", event.next.sessionId)
        assertEquals(before, event.before)
        assertEquals(after, event.after)
    }
    @Test fun sameTransportNetworkReplacementIsBoundary() = boundary(read(), read(net = "b"), TransitionReason.NETWORK_REPLACED)
    @Test fun vpnAndPathChangesAreBoundaries() {
        boundary(read(), read(vpn = true), TransitionReason.VPN_CHANGED)
        val a = read()
        boundary(a, a.copy(path = a.path.copy(pathToken = ok(tokens.token("path", "changed")))), TransitionReason.PATH_CHANGED)
    }
    @Test fun vpnReportedUnderlyingTransportSwitchIsBoundaryWithoutNetworkReplacement() {
        val a = read(transport = NetworkTransport.VPN, vpn = true)
        boundary(a.copy(path = a.path.copy(underlyingTransports = ok("CELLULAR"))),
            a.copy(path = a.path.copy(underlyingTransports = ok("WIFI"))), TransitionReason.PATH_CHANGED)
    }
    @Test fun providerRestartsDoNotReuseEpochIdentities() {
        val a = NetworkEpochTracker("same-session")
        val b = NetworkEpochTracker("same-session")
        a.evaluate(read(), 10)
        b.evaluate(read(), 10)
        assertNotEquals(a.currentEpoch(), b.currentEpoch())
    }
    @Test fun activeDataSubscriptionChangeIsActuallyEmitted() = boundary(read(), read(subscription = "b"), TransitionReason.CELLULAR_SUBSCRIPTION_CHANGED)
    @Test fun servingIdentityChangeWithoutRatChangeIsBoundary() = boundary(
        read(transport = NetworkTransport.CELLULAR, cells = listOf(cell("a"))),
        read(transport = NetworkTransport.CELLULAR, cells = listOf(cell("b"))), TransitionReason.SERVING_CELL_CHANGED)
    @Test fun apBandValidationAndTransportAreBoundaries() {
        boundary(read(), read(ap = "b"), TransitionReason.WIFI_AP_CHANGED)
        boundary(read(), read(frequency = 2412), TransitionReason.WIFI_BAND_CHANGED)
        boundary(read(), read(validated = false), TransitionReason.NETWORK_VALIDATION_CHANGED)
        boundary(read(), read(transport = NetworkTransport.CELLULAR), TransitionReason.TRANSPORT_CHANGED)
    }
    @Test fun noneRequiresObservedLossAndUnknownIsNotNone() {
        boundary(read(), read(transport = NetworkTransport.NONE), TransitionReason.NETWORK_LOST)
        val a = read()
        val unknown = a.copy(path = a.path.copy(transport = absent(Availability.PERMISSION_DENIED), vpn = absent(Availability.PERMISSION_DENIED)))
        val tracker = NetworkEpochTracker("s")
        tracker.evaluate(a, 10)
        val event = requireNotNull(tracker.evaluate(unknown, 20))
        assertFalse(TransitionReason.NETWORK_LOST in event.reasons)
        assertTrue(TransitionReason.OBSERVABILITY_CHANGED in event.reasons)
        val none = requireNotNull(tracker.evaluate(read(transport = NetworkTransport.NONE), 30))
        assertFalse("An inaccessible prior path does not prove a lost network", TransitionReason.NETWORK_LOST in none.reasons)
    }
    @Test fun aToBToAPreservesBothBoundaries() {
        val tracker = NetworkEpochTracker("s")
        val a = read()
        tracker.evaluate(a, 10)
        tracker.evaluate(read(net = "b"), 20)
        tracker.evaluate(a, 30)
        assertEquals(3, tracker.transitions().size)
        assertTrue(tracker.currentEpoch()!!.epochToken.endsWith(":epoch-3"))
    }
    @Test fun neighborChangesDoNotPretendServingCellChangedAndTimingDoesNotChangeEpoch() {
        val tracker = NetworkEpochTracker("s")
        tracker.evaluate(read(transport = NetworkTransport.CELLULAR, cells = listOf(cell(), cell("neighbor-a", registered = false))), 10)
        assertNull(tracker.evaluate(read(transport = NetworkTransport.CELLULAR, cells = listOf(cell(stamp = 5), cell("neighbor-b", registered = false))), 20))
    }
    @Test(expected = IllegalArgumentException::class) fun regressingEventTimeIsRejected() {
        val tracker = NetworkEpochTracker("s")
        tracker.evaluate(read(), 10)
        tracker.evaluate(read(), 9)
    }
}
