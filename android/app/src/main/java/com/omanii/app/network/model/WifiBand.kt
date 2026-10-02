package com.omanii.app.network.model

/**
 * Standard Wi-Fi frequency band categories.
 */
enum class WifiBand {
    BAND_2_4_GHZ,
    BAND_5_GHZ,
    BAND_6_GHZ,
    BAND_60_GHZ,
    OTHER,
    UNKNOWN;

    companion object {
        fun fromFrequencyMhz(freqMhz: Int?): WifiBand {
            if (freqMhz == null || freqMhz <= 0) return UNKNOWN
            return when (freqMhz) {
                in 2400..2500 -> BAND_2_4_GHZ
                in 4900..5900 -> BAND_5_GHZ
                in 5925..7125 -> BAND_6_GHZ
                in 58000..71000 -> BAND_60_GHZ
                else -> OTHER
            }
        }
    }
}
