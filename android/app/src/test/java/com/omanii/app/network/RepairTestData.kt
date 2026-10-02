package com.omanii.app.network

import com.omanii.app.model.*
import com.omanii.app.network.model.*
import com.omanii.app.network.token.IdentifierAnonymizer
import com.omanii.app.network.sentinel.AndroidSentinelFilter

internal fun <T : Any> ok(v: T) = ValueState(v, Availability.AVAILABLE, null)
internal fun <T : Any> absent(a: Availability = Availability.UNKNOWN) = ValueState<T>(null, a, "Synthetic unavailable")
internal val tokens = IdentifierAnonymizer("synthetic-only-fixture-salt")
internal val domain = ClockDomainRef(ClockDomainKind.ANDROID_ELAPSED_REALTIME, "synthetic-boot")
internal fun at(n: Long) = ObservationTiming(n, "synthetic-read", null, null)
internal fun wifi(n: Long = 10, ap: String = "a", frequency: Int = 5180, rssi: Int = -60): WifiConnectedMetrics = WifiConnectedMetrics(
    at(n), ok(rssi), ok(frequency), ok(WifiBand.fromFrequencyMhz(frequency)), ok(0), ok(433), ok(72), ok(WifiStandard.WIFI_5_11AC),
    ok(tokens.token("ssid", "synthetic")), ok(tokens.token("bssid", ap)))
internal fun cell(id: String = "a", stamp: Long = 1, n: Long = 10, registered: Boolean = true, tech: CellularTech = CellularTech.LTE) =
    CellularSignalMetrics(AndroidSentinelFilter.cellTiming(n, stamp, domain), tech, registered, ok(tokens.token("cell", id)), absent(), mapOf("dbm" to ok(-100)))
internal fun read(n: Long = 10, transport: NetworkTransport = NetworkTransport.WIFI, net: String = "a", vpn: Boolean = false,
    validated: Boolean = true, ap: String = "a", frequency: Int = 5180, subscription: String = "a", cells: List<CellularSignalMetrics> = emptyList()): NetworkRead = NetworkRead(
    NetworkPathSnapshot(at(n), ok(transport), ok(tokens.token("network", net)), ok(true), ok(validated), ok(false), ok(vpn),
        absent(if (vpn) Availability.UNKNOWN else Availability.NOT_APPLICABLE), ok(false), ok("NOT_IN_USE_CONFIGURATION_UNKNOWN"), ok(tokens.token("path", net))),
    if (transport == NetworkTransport.WIFI) ok(wifi(n, ap, frequency)) else absent(Availability.NOT_APPLICABLE),
    if (transport == NetworkTransport.CELLULAR) ok(cells) else absent(Availability.NOT_APPLICABLE), ok(tokens.token("subscription", subscription))).let { read ->
        if (transport != NetworkTransport.NONE) read else read.copy(path = read.path.copy(
            networkToken = absent(Availability.NOT_APPLICABLE), internet = absent(Availability.NOT_APPLICABLE),
            validated = absent(Availability.NOT_APPLICABLE), metered = absent(Availability.NOT_APPLICABLE),
            vpn = absent(Availability.NOT_APPLICABLE), underlyingTransports = absent(Availability.NOT_APPLICABLE),
            privateDnsActive = absent(Availability.NOT_APPLICABLE), privateDnsMode = absent(Availability.NOT_APPLICABLE),
            pathToken = absent(Availability.NOT_APPLICABLE)))
    }
