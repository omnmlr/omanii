package com.omanii.app.spatial

import com.omanii.app.pose.HorizontalM
import com.omanii.app.pose.PoseFrameIdentity
import com.omanii.app.pose.PoseRecord
import com.omanii.app.pose.PoseRecordKind

data class PoseDebugTrace(val identity: PoseFrameIdentity, val observedPoints: List<HorizontalM>)

/** Separate polylines at every tracking/boundary event; never join coordinate frames or fill unknown space. */
fun poseDebugTraces(records: List<PoseRecord>): List<PoseDebugTrace> {
    val traces = mutableListOf<PoseDebugTrace>()
    var identity: PoseFrameIdentity? = null
    var points = mutableListOf<HorizontalM>()
    fun flush() {
        identity?.let { if (points.isNotEmpty()) traces += PoseDebugTrace(it, points.toList()) }
        points = mutableListOf()
    }
    records.forEach { record ->
        if (record.kind != PoseRecordKind.SAMPLE || record.identity != identity) {
            flush(); identity = record.identity
        }
        record.pose?.let { points += it.position.horizontal }
    }
    flush()
    return traces
}
