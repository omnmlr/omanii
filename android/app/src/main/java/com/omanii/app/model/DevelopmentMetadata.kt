package com.omanii.app.model

data class ProfileRef(val id: String, val version: String) {
    init { require(id.isNotBlank() && version.isNotBlank()) }
}

/** Development evidence only. Version axes are owned by protocol/README.md and ADR-004. */
data class DevelopmentRecordMetadata(
    val recordId: String,
    val sessionId: String,
    val producer: String,
    val appBuild: String,
    val schemaVersion: String,
    val protocolVersion: String,
    val testProfile: ValueState<ProfileRef>,
) {
    init {
        require(listOf(recordId, sessionId, producer, appBuild, schemaVersion, protocolVersion).all { it.isNotBlank() })
    }
}
