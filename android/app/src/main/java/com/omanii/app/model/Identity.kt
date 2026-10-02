package com.omanii.app.model

data class SegmentRef(val sessionId: String, val segmentId: String) {
    init { require(sessionId.isNotBlank() && segmentId.isNotBlank()) }
}

data class NetworkEpochRef(val sessionId: String, val epochToken: String) {
    init { require(sessionId.isNotBlank() && epochToken.isNotBlank()) }
}

data class CoordinateFrameRef(val sessionId: String, val frameId: String) {
    init { require(sessionId.isNotBlank() && frameId.isNotBlank()) }
}
