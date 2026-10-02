package com.omanii.app.capability

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import android.location.LocationManager
import com.omanii.app.network.provider.RadioAccessGate
import com.omanii.app.model.*
import com.omanii.app.time.AndroidElapsedRealtimeClock
import java.util.UUID

class AndroidCapabilityRepository(private val context: Context, private val clock: MonotonicClock = AndroidElapsedRealtimeClock) : CapabilityRepository {
    override fun getCapabilityReport(): CapabilityReport {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val pkg = pm.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        val version = if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode else pkg.versionCode.toLong()
        val permissions = listOf(Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).associateWith {
            ValueState(context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED, Availability.AVAILABLE, "Current grant; request history not inferred")
        }
        val network = CapabilityGate.evaluate(true, true, context.getSystemService(ConnectivityManager::class.java) != null,
            permissions.getValue(Manifest.permission.ACCESS_NETWORK_STATE).value == true)
        val wifi = CapabilityGate.evaluate(true, pm.hasSystemFeature(PackageManager.FEATURE_WIFI), context.getSystemService(WifiManager::class.java) != null,
            permissions.getValue(Manifest.permission.ACCESS_WIFI_STATE).value == true)
        val locationState: ValueState<Boolean> = try {
            val lm = context.getSystemService(LocationManager::class.java)
            if (lm == null) ValueState(null, Availability.TEMPORARILY_UNAVAILABLE, "Location service absent") else
                ValueState(if (Build.VERSION.SDK_INT >= 28) lm.isLocationEnabled else
                    lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER), Availability.AVAILABLE, null)
        } catch (_: SecurityException) { ValueState(null, Availability.PERMISSION_DENIED, "Location state access denied") }
        catch (_: RuntimeException) { ValueState(null, Availability.UNKNOWN, "Location state unavailable") }
        val cellGate = RadioAccessGate.cell(pm.hasSystemFeature(if (Build.VERSION.SDK_INT >= 33) PackageManager.FEATURE_TELEPHONY_RADIO_ACCESS else PackageManager.FEATURE_TELEPHONY),
            context.getSystemService(TelephonyManager::class.java) != null, permissions.getValue(Manifest.permission.ACCESS_FINE_LOCATION).value == true, locationState)
        val cell = ValueState(if (cellGate.value == true) CapabilitySupport.SUPPORTED else null, cellGate.availability, cellGate.reason)
        val notEvaluated = ValueState<CapabilitySupport>(null, Availability.NOT_EVALUATED, "Owned by later tasks")
        return CapabilityReport(UUID.randomUUID().toString(), clock.nowElapsedRealtimeNs(), DeviceMetadata(Build.MANUFACTURER, Build.MODEL,
            Build.BRAND, Build.DEVICE, Build.VERSION.SDK_INT, Build.VERSION.RELEASE, pkg.versionName ?: "unknown", version), permissions,
            mapOf("network_path" to network, "connected_wifi" to wifi, "cell_cache_lte_nr" to cell,
                "wifi_rx_tx_speed" to CapabilityGate.evaluate(Build.VERSION.SDK_INT >= 29, pm.hasSystemFeature(PackageManager.FEATURE_WIFI),
                    context.getSystemService(WifiManager::class.java) != null, permissions.getValue(Manifest.permission.ACCESS_WIFI_STATE).value == true)),
            listOf("low_data_responsiveness", "scan_survey", "verification_low_data", "verification_deep", "deliberate_benchmark", "gaming_assessment")
                .associateWith { notEvaluated }, notEvaluated)
    }
}
