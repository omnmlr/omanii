package com.omanii.app.devexport

import android.content.Context
import android.content.pm.ApplicationInfo
import com.omanii.app.capability.*
import com.omanii.app.network.provider.*
import java.io.File

/** Callable debug adapter only. Shared app entry point remains integration-owned. */
class DevDebugStateProvider(context: Context, private val sessionId: String) {
    private val context = context.applicationContext
    private val capabilities: CapabilityRepository = AndroidCapabilityRepository(this.context)
    private val network: NetworkContextProvider = AndroidNetworkContextProvider(this.context, sessionId)
    init { require(this.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) { "Development build required" } }

    fun captureSummary(): String {
        val observation = network.captureRadioObservation()
        return "transport=${observation.read.path.transport}; epoch=${observation.epoch}; " +
            "independence=${observation.independence}; Wi-Fi link speed is local link rate; measurement age remains unknown"
    }

    fun exportSnapshot(): File {
        val report = capabilities.getCapabilityReport()
        val observation = network.captureRadioObservation()
        val build = "${report.device.appVersionName}+${report.device.appVersionCode}"
        val target = File(context.filesDir, "task002-development.jsonl")
        target.bufferedWriter(Charsets.UTF_8).use { output ->
            output.appendLine(DevExportJsonSerializer.capability(report, DevExportJsonSerializer.metadata(report.reportId, sessionId, build)))
            network.transitions().forEachIndexed { index, event ->
                output.appendLine(DevExportJsonSerializer.transition(event, DevExportJsonSerializer.metadata("transition-$index", sessionId, build)))
            }
            output.appendLine(DevExportJsonSerializer.observation(observation, DevExportJsonSerializer.metadata(observation.observationId, sessionId, build)))
        }
        return target
    }
}
