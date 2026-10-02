package com.omanii.app.network

import com.omanii.app.network.model.WifiBand
import com.omanii.app.network.model.WifiStandard
import org.junit.Assert.assertEquals
import org.junit.Test

class WifiMetricsMappingTest {

    @Test
    fun wifiBand_mapsFrequencyMhzCorrectly() {
        // 2.4 GHz channels
        assertEquals(WifiBand.BAND_2_4_GHZ, WifiBand.fromFrequencyMhz(2412))
        assertEquals(WifiBand.BAND_2_4_GHZ, WifiBand.fromFrequencyMhz(2484))

        // 5 GHz channels
        assertEquals(WifiBand.BAND_5_GHZ, WifiBand.fromFrequencyMhz(5180))
        assertEquals(WifiBand.BAND_5_GHZ, WifiBand.fromFrequencyMhz(5825))

        // 6 GHz (Wi-Fi 6E / 7)
        assertEquals(WifiBand.BAND_6_GHZ, WifiBand.fromFrequencyMhz(5955))
        assertEquals(WifiBand.BAND_6_GHZ, WifiBand.fromFrequencyMhz(7115))

        // 60 GHz (WiGig)
        assertEquals(WifiBand.BAND_60_GHZ, WifiBand.fromFrequencyMhz(60000))

        // Invalid or null
        assertEquals(WifiBand.UNKNOWN, WifiBand.fromFrequencyMhz(null))
        assertEquals(WifiBand.UNKNOWN, WifiBand.fromFrequencyMhz(0))
        assertEquals(WifiBand.UNKNOWN, WifiBand.fromFrequencyMhz(-1))

        // Non-standard frequency
        assertEquals(WifiBand.OTHER, WifiBand.fromFrequencyMhz(900))
    }

    @Test
    fun wifiStandard_mapsAndroidStandardsCorrectly() {
        assertEquals(WifiStandard.LEGACY, WifiStandard.fromAndroidWifiStandard(1))
        assertEquals(WifiStandard.WIFI_4_11N, WifiStandard.fromAndroidWifiStandard(4))
        assertEquals(WifiStandard.WIFI_5_11AC, WifiStandard.fromAndroidWifiStandard(5))
        assertEquals(WifiStandard.WIFI_6_11AX, WifiStandard.fromAndroidWifiStandard(6))
        assertEquals(WifiStandard.WIFI_60GHZ_11AD, WifiStandard.fromAndroidWifiStandard(7))
        assertEquals(WifiStandard.WIFI_7_11BE, WifiStandard.fromAndroidWifiStandard(8))

        assertEquals(WifiStandard.UNKNOWN, WifiStandard.fromAndroidWifiStandard(0))
        assertEquals(WifiStandard.UNKNOWN, WifiStandard.fromAndroidWifiStandard(null))
        assertEquals(WifiStandard.UNKNOWN, WifiStandard.fromAndroidWifiStandard(99))
    }
}
