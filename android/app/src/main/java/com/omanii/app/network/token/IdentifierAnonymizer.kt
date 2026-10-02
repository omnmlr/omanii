package com.omanii.app.network.token

import com.omanii.app.model.*
import java.security.MessageDigest
import java.util.UUID

/** Ephemeral provider lifetime; see TASK-002 evidence and ADR-005. */
class IdentifierAnonymizer(private val sessionSalt: String = UUID.randomUUID().toString()) {
    init { require(sessionSalt.isNotBlank()) }
    fun token(kind: String, raw: String): String {
        require(kind.matches(Regex("[a-z_]+")) && raw.isNotBlank())
        return kind + "_tok_" + MessageDigest.getInstance("SHA-256")
            .digest("$sessionSalt\u0000$kind\u0000$raw".toByteArray()).joinToString("") { "%02x".format(it) }
    }
    fun anonymizeSsid(raw: String?): ValueState<String> {
        val clean = raw?.trim()?.removeSurrounding("\"")
        return if (clean.isNullOrBlank() || clean == "<unknown ssid>") ValueState(null, Availability.UNKNOWN, "SSID absent or concealed")
        else ValueState(token("ssid", clean), Availability.AVAILABLE, null)
    }
    fun anonymizeBssid(raw: String?): ValueState<String> {
        val clean = raw?.trim()?.lowercase()
        return when {
            clean.isNullOrBlank() -> ValueState(null, Availability.UNKNOWN, "No BSSID supplied")
            clean in setOf("02:00:00:00:00:00", "00:00:00:00:00:00") -> ValueState(null, Availability.REDACTED, "Platform placeholder")
            !clean.matches(Regex("([0-9a-f]{2}:){5}[0-9a-f]{2}")) -> ValueState(null, Availability.UNKNOWN, "Invalid BSSID")
            else -> ValueState(token("bssid", clean), Availability.AVAILABLE, null)
        }
    }
}
