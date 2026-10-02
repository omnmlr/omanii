package com.omanii.app.network.provider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.*
import com.omanii.app.model.*
import com.omanii.app.network.model.*
import com.omanii.app.network.sentinel.AndroidSentinelFilter as Filter
import com.omanii.app.network.sentinel.CellularMetricMapper
import com.omanii.app.network.token.IdentifierAnonymizer
import com.omanii.app.time.AndroidElapsedRealtimeClock

class AndroidNetworkContextProvider(
    context: Context,
    sessionId: String,
    clock: MonotonicClock = AndroidElapsedRealtimeClock,
) : NetworkContextProvider by NetworkContextLogger(AndroidNetworkReadSource(context, clock), clock, sessionId)

class AndroidNetworkReadSource(
    context: Context,
    private val clock: MonotonicClock = AndroidElapsedRealtimeClock,
) : NetworkReadSource {
    private val context = context.applicationContext
    private val tokens = IdentifierAnonymizer()
    private val domain = ClockDomainRef(ClockDomainKind.ANDROID_ELAPSED_REALTIME, java.util.UUID.randomUUID().toString())

    private fun <T : Any> available(value: T) = ValueState(value, Availability.AVAILABLE, null)
    private fun <T : Any> missing(state: Availability, reason: String) = ValueState<T>(null, state, reason)
    private fun timing(source: String) = ObservationTiming(clock.nowElapsedRealtimeNs(), source, null, null)
    private fun permission(name: String) = context.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED

    private fun unavailablePath(state: Availability, reason: String) = NetworkPathSnapshot(
        timing("ConnectivityManager.read"), missing(state, reason), missing(state, reason), missing(state, reason),
        missing(state, reason), missing(state, reason), missing(state, reason), missing(state, reason),
        missing(state, reason), missing(state, reason), missing(state, reason),
    )

    @SuppressLint("MissingPermission") // Checked immediately before access; revocation is caught below.
    override fun read(): NetworkRead {
        if (!permission(Manifest.permission.ACCESS_NETWORK_STATE)) return NetworkRead(
            unavailablePath(Availability.PERMISSION_DENIED, "ACCESS_NETWORK_STATE required"),
            missing(Availability.NOT_EVALUATED, "Path unavailable"), missing(Availability.NOT_EVALUATED, "Path unavailable"),
            missing(Availability.NOT_EVALUATED, "Path unavailable"))
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return NetworkRead(
            unavailablePath(Availability.TEMPORARILY_UNAVAILABLE, "Connectivity service absent"),
            missing(Availability.NOT_EVALUATED, "Path unavailable"), missing(Availability.NOT_EVALUATED, "Path unavailable"),
            missing(Availability.NOT_EVALUATED, "Path unavailable"))
        return try {
            val network = manager.activeNetwork
            if (network == null) {
                val na = Availability.NOT_APPLICABLE
                NetworkRead(NetworkPathSnapshot(timing("ConnectivityManager.activeNetwork"), available(NetworkTransport.NONE),
                    missing(na, "No active network"), missing(na, "No active network"), missing(na, "No active network"),
                    missing(na, "No active network"), missing(na, "No active network"), missing(na, "No active network"),
                    missing(na, "No active network"), missing(na, "No active network"), missing(na, "No active network")),
                    missing(na, "No active network"), missing(na, "No active network"), activeSubscription())
            } else {
                val caps = manager.getNetworkCapabilities(network)
                val links = manager.getLinkProperties(network)
                if (caps == null) return NetworkRead(unavailablePath(Availability.TEMPORARILY_UNAVAILABLE, "Network capabilities absent"),
                    missing(Availability.NOT_EVALUATED, "Path unknown"), missing(Availability.NOT_EVALUATED, "Path unknown"), activeSubscription())
                val transport = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkTransport.VPN
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.CELLULAR
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransport.ETHERNET
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkTransport.BLUETOOTH
                    else -> NetworkTransport.OTHER
                }
                val reported = mutableListOf<String>()
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) reported += "WIFI"
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) reported += "CELLULAR"
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) reported += "ETHERNET"
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) reported += "BLUETOOTH"
                if (Build.VERSION.SDK_INT >= 26 && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) reported += "WIFI_AWARE"
                if (Build.VERSION.SDK_INT >= 27 && caps.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)) reported += "LOWPAN"
                if (Build.VERSION.SDK_INT >= 31 && caps.hasTransport(NetworkCapabilities.TRANSPORT_USB)) reported += "USB"
                if (Build.VERSION.SDK_INT >= 34 && caps.hasTransport(NetworkCapabilities.TRANSPORT_THREAD)) reported += "THREAD"
                if (Build.VERSION.SDK_INT >= 35 && caps.hasTransport(NetworkCapabilities.TRANSPORT_SATELLITE)) reported += "SATELLITE"
                val underlying = if (transport != NetworkTransport.VPN) missing<String>(Availability.NOT_APPLICABLE, "Not a VPN")
                    else if (reported.isEmpty()) missing(Availability.UNKNOWN, "VPN has no reported underlying transport; physical path not established")
                    else ValueState(reported.sorted().joinToString(","), Availability.AVAILABLE, "Platform-reported non-VPN transports; not exact physical attribution")
                val dnsActive: ValueState<Boolean>
                val dnsMode: ValueState<String>
                if (Build.VERSION.SDK_INT < 28) {
                    dnsActive = missing(Availability.NOT_SUPPORTED, "Requires API 28")
                    dnsMode = missing(Availability.NOT_SUPPORTED, "Requires API 28")
                } else if (links == null) {
                    dnsActive = missing(Availability.TEMPORARILY_UNAVAILABLE, "LinkProperties absent")
                    dnsMode = missing(Availability.TEMPORARILY_UNAVAILABLE, "LinkProperties absent")
                } else {
                    dnsActive = available(links.isPrivateDnsActive)
                    dnsMode = available(when {
                        links.privateDnsServerName != null -> "STRICT_NETWORK_CONTEXT"
                        links.isPrivateDnsActive -> "OPPORTUNISTIC_IN_USE"
                        else -> "NOT_IN_USE_CONFIGURATION_UNKNOWN"
                    })
                }
                // Tokenize path descriptors in memory; never export route addresses/proxy/server names.
                val pathToken = if (links == null) missing<String>(Availability.TEMPORARILY_UNAVAILABLE, "LinkProperties absent") else
                    available(tokens.token("path", listOf(links.interfaceName, links.routes.sortedBy { it.toString() },
                        links.dnsServers.map { it.hostAddress }.sortedBy { it }, links.httpProxy,
                        if (Build.VERSION.SDK_INT >= 28) links.privateDnsServerName else null).toString()))
                val path = NetworkPathSnapshot(timing("ConnectivityManager.activeNetwork/capabilities/linkProperties"), available(transport),
                    available(tokens.token("network", network.toString())), available(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)),
                    available(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)),
                    available(!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) &&
                        !(Build.VERSION.SDK_INT >= 30 && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED))),
                    available(transport == NetworkTransport.VPN), underlying, dnsActive, dnsMode, pathToken)
                val subscription = activeSubscription()
                val wifi = if (transport == NetworkTransport.WIFI) readWifi() else missing<WifiConnectedMetrics>(Availability.NOT_APPLICABLE, "Active path is not direct Wi-Fi")
                val cells = if (transport == NetworkTransport.CELLULAR) readCells() else missing<List<CellularSignalMetrics>>(Availability.NOT_APPLICABLE, "Active path is not direct cellular")
                if (network != manager.activeNetwork || subscription != activeSubscription()) {
                    NetworkRead(unavailablePath(Availability.TEMPORARILY_UNAVAILABLE, "Path/subscription changed during read"),
                        missing(Availability.TEMPORARILY_UNAVAILABLE, "Read crossed boundary"),
                        missing(Availability.TEMPORARILY_UNAVAILABLE, "Read crossed boundary"),
                        missing(Availability.TEMPORARILY_UNAVAILABLE, "Read crossed boundary"))
                } else NetworkRead(path, wifi, cells, subscription)
            }
        } catch (_: SecurityException) {
            NetworkRead(unavailablePath(Availability.PERMISSION_DENIED, "Network access denied or revoked"),
                missing(Availability.NOT_EVALUATED, "Network access denied"), missing(Availability.NOT_EVALUATED, "Network access denied"),
                missing(Availability.NOT_EVALUATED, "Network access denied"))
        } catch (_: RuntimeException) {
            NetworkRead(unavailablePath(Availability.TEMPORARILY_UNAVAILABLE, "Network read failed"),
                missing(Availability.NOT_EVALUATED, "Network read failed"), missing(Availability.NOT_EVALUATED, "Network read failed"),
                missing(Availability.NOT_EVALUATED, "Network read failed"))
        }
    }

    private fun activeSubscription(): ValueState<String> {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return missing(Availability.NOT_SUPPORTED, "No telephony hardware")
        if (Build.VERSION.SDK_INT < 30) return missing(Availability.NOT_SUPPORTED, "Active data subscription getter requires API 30; default is not active")
        return try {
            val id = SubscriptionManager.getActiveDataSubscriptionId()
            if (id == SubscriptionManager.INVALID_SUBSCRIPTION_ID) missing(Availability.TEMPORARILY_UNAVAILABLE, "No chosen active data subscription")
            else available(tokens.token("subscription", id.toString()))
        } catch (_: SecurityException) { missing(Availability.PERMISSION_DENIED, "Active subscription access denied") }
        catch (_: UnsupportedOperationException) { missing(Availability.NOT_SUPPORTED, "Subscription API unsupported") }
        catch (_: RuntimeException) { missing(Availability.TEMPORARILY_UNAVAILABLE, "Subscription read failed") }
    }

    @SuppressLint("MissingPermission")
    private fun readWifi(): ValueState<WifiConnectedMetrics> {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) return missing(Availability.NOT_SUPPORTED, "No Wi-Fi hardware")
        if (!permission(Manifest.permission.ACCESS_WIFI_STATE)) return missing(Availability.PERMISSION_DENIED, "ACCESS_WIFI_STATE required")
        return try {
            // Public compatibility path reports internet-providing Wi-Fi because this task
            // never creates a peer/local-only network. Synchronous capabilities redact identity.
            @Suppress("DEPRECATION")
            val info: WifiInfo? = context.getSystemService(WifiManager::class.java)?.connectionInfo
            if (info == null) return missing(Availability.TEMPORARILY_UNAVAILABLE, "Connected WifiInfo absent")
            val frequency = Filter.wifiFrequency(info.frequency)
            val band = frequency.value?.let { available(WifiBand.fromFrequencyMhz(it)) } ?: missing( frequency.availability, "Frequency unavailable")
            val fine = permission(Manifest.permission.ACCESS_FINE_LOCATION)
            val location = locationEnabled()
            val identityState = when {
                !fine -> Availability.PERMISSION_DENIED
                location.value == false -> Availability.TEMPORARILY_UNAVAILABLE
                location.value == null -> location.availability
                else -> null
            }
            available(WifiConnectedMetrics(timing("WifiManager.connectionInfo/compatibility-current-internet-wifi"),
                Filter.wifiRssi(info.rssi), frequency, band, Filter.wifiSpeed(info.linkSpeed),
                if (Build.VERSION.SDK_INT >= 29) Filter.wifiSpeed(info.rxLinkSpeedMbps) else missing(Availability.NOT_SUPPORTED, "Requires API 29"),
                if (Build.VERSION.SDK_INT >= 29) Filter.wifiSpeed(info.txLinkSpeedMbps) else missing(Availability.NOT_SUPPORTED, "Requires API 29"),
                if (Build.VERSION.SDK_INT >= 30) WifiStandard.fromAndroidWifiStandard(info.wifiStandard).let {
                    if (it == WifiStandard.UNKNOWN) missing(Availability.UNKNOWN, "Standard unreported") else available(it)
                } else missing(Availability.NOT_SUPPORTED, "Requires API 30"),
                if (identityState != null) missing(identityState, "Wi-Fi identity requires applicable location access/state") else tokens.anonymizeSsid(info.ssid),
                if (identityState != null) missing(identityState, "Wi-Fi identity requires applicable location access/state") else tokens.anonymizeBssid(info.bssid)))
        } catch (_: SecurityException) { missing(Availability.PERMISSION_DENIED, "Wi-Fi access denied or revoked") }
        catch (_: RuntimeException) { missing(Availability.TEMPORARILY_UNAVAILABLE, "Wi-Fi read failed") }
    }

    private fun locationEnabled(): ValueState<Boolean> = try {
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null) missing(Availability.TEMPORARILY_UNAVAILABLE, "Location service absent") else available(
            if (Build.VERSION.SDK_INT >= 28) manager.isLocationEnabled else
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    } catch (_: SecurityException) { missing(Availability.PERMISSION_DENIED, "Location state denied") }
    catch (_: RuntimeException) { missing(Availability.UNKNOWN, "Location state unknown") }

    @SuppressLint("MissingPermission")
    private fun readCells(): ValueState<List<CellularSignalMetrics>> {
        val manager = context.getSystemService(TelephonyManager::class.java)
        val access = RadioAccessGate.cell(context.packageManager.hasSystemFeature(if (Build.VERSION.SDK_INT >= 33) PackageManager.FEATURE_TELEPHONY_RADIO_ACCESS else PackageManager.FEATURE_TELEPHONY),
            manager != null, permission(Manifest.permission.ACCESS_FINE_LOCATION), locationEnabled())
        if (access.value != true) return missing(access.availability, requireNotNull(access.reason))
        requireNotNull(manager)
        return try {
            val list = manager.allCellInfo ?: return missing(Availability.TEMPORARILY_UNAVAILABLE, "Cell cache unavailable")
            if (list.isEmpty()) return missing(Availability.TEMPORARILY_UNAVAILABLE, "Cell cache empty")
            val received = clock.nowElapsedRealtimeNs()
            available(list.map { cell -> mapCell(cell, received) })
        } catch (_: SecurityException) { missing(Availability.PERMISSION_DENIED, "Fine location denied or revoked") }
        catch (_: UnsupportedOperationException) { missing(Availability.NOT_SUPPORTED, "Cell API unsupported") }
        catch (_: RuntimeException) { missing(Availability.TEMPORARILY_UNAVAILABLE, "Cell cache read failed") }
    }

    private fun mapCell(cell: CellInfo, received: Long): CellularSignalMetrics {
        val stamp = if (Build.VERSION.SDK_INT >= 30) Filter.millisToNanos(cell.timestampMillis) else {
            @Suppress("DEPRECATION")
            cell.timeStamp.takeIf { it >= 0 && it != Long.MAX_VALUE }
        }
        var tech = CellularTech.UNKNOWN
        var identity: String? = null
        val fields = linkedMapOf<String, ValueState<Int>>()
        when {
            cell is CellInfoLte -> {
                tech = CellularTech.LTE
                val id = cell.cellIdentity
                if (id.ci != Int.MAX_VALUE && id.tac != Int.MAX_VALUE && id.mcc != Int.MAX_VALUE && id.mnc != Int.MAX_VALUE)
                    identity = "LTE:${id.mcc}:${id.mnc}:${id.tac}:${id.ci}"
                val ss = cell.cellSignalStrength
                fields.putAll(CellularMetricMapper.lte(Build.VERSION.SDK_INT, ss.dbm, ss.asuLevel, ss.timingAdvance,
                    if (Build.VERSION.SDK_INT >= 26) ss.rsrp else null,
                    if (Build.VERSION.SDK_INT >= 26) ss.rsrq else null,
                    if (Build.VERSION.SDK_INT >= 26) ss.rssnr else null,
                    if (Build.VERSION.SDK_INT >= 26) ss.cqi else null))
            }
            Build.VERSION.SDK_INT >= 29 && cell is CellInfoNr -> {
                tech = CellularTech.NR
                val id = cell.cellIdentity as? CellIdentityNr
                if (id != null && id.nci != Long.MAX_VALUE && id.tac != Int.MAX_VALUE && id.mccString != null && id.mncString != null)
                    identity = "NR:${id.mccString}:${id.mncString}:${id.tac}:${id.nci}"
                val ss = cell.cellSignalStrength as? CellSignalStrengthNr
                fields.putAll(CellularMetricMapper.nr(ss?.dbm, ss?.asuLevel, ss?.csiRsrp, ss?.csiRsrq, ss?.csiSinr,
                    ss?.ssRsrp, ss?.ssRsrq, ss?.ssSinr))
            }
            else -> fields["signal"] = missing(Availability.NOT_EVALUATED, "Only LTE/NR metric adapters implemented")
        }
        return CellularSignalMetrics(Filter.cellTiming(received, stamp, domain), tech, cell.isRegistered,
            identity?.let { available(tokens.token("cell", it)) } ?: missing(Availability.UNKNOWN, "Complete cell identity unavailable"),
            missing(Availability.UNKNOWN, "getAllCellInfo covers all radios; registered does not establish active-data attribution"), fields)
    }
}
