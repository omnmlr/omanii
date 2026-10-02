package com.omanii.app.network.sentinel

import com.omanii.app.model.*

/** Android field mapping only; official API sources are recorded in TASK-002 evidence. */
object CellularMetricMapper {
    fun lte(api: Int, dbm: Int?, asu: Int?, ta: Int?, rsrp: Int?, rsrq: Int?, rssnr: Int?, cqi: Int?): Map<String, ValueState<Int>> {
        val result = linkedMapOf("dbm" to AndroidSentinelFilter.cellInt(dbm), "asu" to AndroidSentinelFilter.lteAsu(asu),
            "timing_advance_index" to AndroidSentinelFilter.cellInt(ta, 0..1282))
        if (api >= 26) {
            result["rsrp_dbm"] = AndroidSentinelFilter.cellInt(rsrp, -140..-43)
            // Android's public RSRQ getter specifies no numeric range. Preserve reported nonsentinel values.
            result["rsrq_db"] = AndroidSentinelFilter.cellInt(rsrq)
            result["rssnr_db"] = AndroidSentinelFilter.cellInt(rssnr, -20..30)
            result["cqi"] = AndroidSentinelFilter.cellInt(cqi, 0..15)
        } else listOf("rsrp_dbm", "rsrq_db", "rssnr_db", "cqi").forEach {
            result[it] = ValueState(null, Availability.NOT_SUPPORTED, "Requires API 26")
        }
        return result
    }

    fun nr(dbm: Int?, asu: Int?, csiRsrp: Int?, csiRsrq: Int?, csiSinr: Int?, ssRsrp: Int?, ssRsrq: Int?, ssSinr: Int?) = linkedMapOf(
        "dbm" to AndroidSentinelFilter.cellInt(dbm), "asu" to AndroidSentinelFilter.nrAsu(asu),
        "csi_rsrp_dbm" to AndroidSentinelFilter.cellInt(csiRsrp, -156..-31),
        "csi_rsrq_db" to AndroidSentinelFilter.cellInt(csiRsrq, -20..-3),
        "csi_sinr_db" to AndroidSentinelFilter.cellInt(csiSinr, -23..23),
        "ss_rsrp_dbm" to AndroidSentinelFilter.cellInt(ssRsrp, -156..-31),
        "ss_rsrq_db" to AndroidSentinelFilter.cellInt(ssRsrq, -43..20),
        "ss_sinr_db" to AndroidSentinelFilter.cellInt(ssSinr, -23..40),
    )
}
