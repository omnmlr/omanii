package com.omanii.app.devexport

import com.omanii.app.capability.CapabilityReport
import com.omanii.app.model.*
import com.omanii.app.network.model.*

object DevExportJsonSerializer {
    const val SCHEMA = "omanii-task002-development-v2"
    const val PROTOCOL = "task002-passive-platform-cache-v2"

    fun metadata(recordId: String, sessionId: String, build: String) = DevelopmentRecordMetadata(recordId, sessionId,
        "TASK-002", build, SCHEMA, PROTOCOL, ValueState(null, Availability.NOT_APPLICABLE, "Passive context collection; no active measurement profile"))

    fun observation(observation: RadioObservation, metadata: DevelopmentRecordMetadata, synthetic: Boolean = false): String {
        require(metadata.sessionId == observation.epoch.sessionId && metadata.recordId == observation.observationId)
        return record("radio_observation", metadata, synthetic, mapOf("timing" to timing(observation.timing),
            "epoch" to epoch(observation.epoch), "independence" to state(observation.independence), "read" to read(observation.read)))
    }
    fun transition(event: NetworkTransitionEvent, metadata: DevelopmentRecordMetadata, synthetic: Boolean = false): String {
        require(metadata.sessionId == event.next.sessionId)
        return record("transition_event", metadata, synthetic, mapOf("at_ns" to event.atElapsedRealtimeNs,
            "previous" to event.previous?.let { epoch(it) }, "next" to epoch(event.next), "reasons" to event.reasons.map { it.name },
            "before" to event.before?.let { read(it) }, "after" to read(event.after)))
    }
    fun capability(report: CapabilityReport, metadata: DevelopmentRecordMetadata, synthetic: Boolean = false): String {
        require(metadata.recordId == report.reportId)
        val d = report.device
        return record("capability_report", metadata, synthetic, mapOf("created_at_ns" to report.createdAtElapsedRealtimeNs,
            "device" to mapOf("manufacturer" to d.manufacturer, "model" to d.model, "brand" to d.brand, "device" to d.device,
                "api" to d.apiLevel, "release" to d.releaseVersion, "app_version" to d.appVersionName, "app_version_code" to d.appVersionCode),
            "permissions" to report.permissions.mapValues { state(it.value) }, "observations" to report.observations.mapValues { state(it.value) },
            "profiles" to report.profiles.mapValues { state(it.value) }, "ar_pose" to state(report.arPose)))
    }
    private fun record(kind: String, m: DevelopmentRecordMetadata, synthetic: Boolean, payload: Map<String, Any?>): String {
        require(m.schemaVersion == SCHEMA && m.protocolVersion == PROTOCOL)
        return json(mapOf("export_type" to "development_research", "synthetic" to synthetic, "record_type" to kind,
            "metadata" to mapOf("record_id" to m.recordId, "session_id" to m.sessionId, "producer" to m.producer,
                "app_build" to m.appBuild, "schema_version" to m.schemaVersion, "protocol_version" to m.protocolVersion,
                "collection_id" to PROTOCOL, "test_profile" to state(m.testProfile)), "payload" to payload))
    }
    private fun epoch(e: NetworkEpochRef) = mapOf("session_id" to e.sessionId, "epoch_token" to e.epochToken)
    private fun timing(t: ObservationTiming) = mapOf("received_at_ns" to t.receivedAtElapsedRealtimeNs, "source" to t.source,
        "measurement_at_ns" to t.measurementAtElapsedRealtimeNs, "age_known" to t.ageKnown, "source_timestamp" to t.sourceTimestamp?.let {
            mapOf("timestamp_ns" to it.timestampNs, "domain" to it.domain.kind.name, "instance_token" to it.domain.instanceToken, "meaning" to it.meaning.name)
        })
    private fun <T : Any> state(v: ValueState<T>, convert: (T) -> Any = { it }) = mapOf("availability" to v.availability.name,
        "value" to v.value?.let(convert), "reason" to v.reason)
    private fun token(v: ValueState<String>, kind: String): Map<String, Any?> {
        require(v.value == null || v.value.matches(Regex("${kind}_tok_[0-9a-f]{64}"))) { "Expected provider-scoped $kind token" }
        return state(v)
    }
    private fun read(r: NetworkRead): Map<String, Any?> {
        val p = r.path
        return mapOf("path" to mapOf("timing" to timing(p.timing), "transport" to state(p.transport),
            "network_token" to token(p.networkToken, "network"), "internet" to state(p.internet), "validated" to state(p.validated),
            "metered" to state(p.metered), "vpn" to state(p.vpn), "underlying_transports" to state(p.underlyingTransports),
            "private_dns_active" to state(p.privateDnsActive), "private_dns_mode" to state(p.privateDnsMode), "path_token" to token(p.pathToken, "path")),
            "wifi" to state(r.wifi) { w -> mapOf("timing" to timing(w.timing), "rssi_dbm" to state(w.rssiDbm), "frequency_mhz" to state(w.frequencyMhz),
                "band" to state(w.band), "link_speed_mbps" to state(w.linkSpeedMbps), "rx_link_speed_mbps" to state(w.rxLinkSpeedMbps),
                "tx_link_speed_mbps" to state(w.txLinkSpeedMbps), "standard" to state(w.standard), "ssid_token" to token(w.ssidToken, "ssid"), "bssid_token" to token(w.bssidToken, "bssid")) },
            "cells" to state(r.cells) { list -> list.map { c -> mapOf("timing" to timing(c.timing), "tech" to c.tech.name,
                "registered" to c.registered, "cell_token" to token(c.cellToken, "cell"), "active_data_attribution" to state(c.activeDataAttribution),
                "metrics" to c.metrics.mapValues { state(it.value) }, "source_recency" to c.sourceRecency.name) } },
            "active_data_subscription_token" to token(r.activeDataSubscriptionToken, "subscription"))
    }
    internal fun json(value: Any?): String = when (value) {
        null -> "null"
        is String -> quote(value)
        is Enum<*> -> quote(value.name)
        is Boolean, is Int, is Long -> value.toString()
        is ProfileRef -> json(mapOf("id" to value.id, "version" to value.version))
        is Map<*, *> -> value.entries.sortedBy { it.key.toString() }.joinToString(",", "{", "}") { quote(it.key.toString()) + ":" + json(it.value) }
        is Iterable<*> -> value.joinToString(",", "[", "]") { json(it) }
        else -> error("Unsupported development value")
    }
    private fun quote(s: String): String = buildString {
        append('"')
        s.forEach { ch -> when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            else -> if (ch.code < 32) append("\\u%04x".format(ch.code)) else append(ch)
        } }
        append('"')
    }
}
