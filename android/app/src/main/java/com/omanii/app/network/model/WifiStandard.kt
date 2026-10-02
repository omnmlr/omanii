package com.omanii.app.network.model

/**
 * Standard Wi-Fi generation/specification.
 */
enum class WifiStandard {
    UNKNOWN,
    LEGACY, // 802.11a/b/g
    WIFI_4_11N,
    WIFI_5_11AC,
    WIFI_6_11AX,
    WIFI_60GHZ_11AD,
    WIFI_7_11BE;

    companion object {
        fun fromAndroidWifiStandard(standard: Int?): WifiStandard {
            if (standard == null) return UNKNOWN
            return when (standard) {
                1 -> LEGACY
                4 -> WIFI_4_11N
                5 -> WIFI_5_11AC
                6 -> WIFI_6_11AX
                7 -> WIFI_60GHZ_11AD
                8 -> WIFI_7_11BE
                else -> UNKNOWN
            }
        }
    }
}
