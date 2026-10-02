package com.omanii.app.model

enum class ClockDomainKind {
    ANDROID_ELAPSED_REALTIME,
    ARCORE_FRAME_UNDEFINED,
    UNKNOWN,
}

data class ClockDomainRef(
    val kind: ClockDomainKind,
    val instanceToken: String,
) {
    init { require(instanceToken.isNotBlank()) }
}

enum class SourceTimeMeaning {
    MEASUREMENT,
    FRAME_CAPTURE,
    PLATFORM_RECEIPT,
    UNKNOWN,
}

data class SourceTimestamp(
    val timestampNs: Long,
    val domain: ClockDomainRef,
    val meaning: SourceTimeMeaning,
) {
    init {
        if (domain.kind == ClockDomainKind.ANDROID_ELAPSED_REALTIME) require(timestampNs >= 0)
    }
}

/** Canonical meaning: protocol/README.md, time contract; no source-clock alignment is inferred. */
data class ObservationTiming(
    val receivedAtElapsedRealtimeNs: Long,
    val source: String,
    val sourceTimestamp: SourceTimestamp?,
    val measurementAtElapsedRealtimeNs: Long?,
) {
    init {
        require(receivedAtElapsedRealtimeNs >= 0)
        require(source.isNotBlank())
        if (measurementAtElapsedRealtimeNs != null) {
            require(measurementAtElapsedRealtimeNs in 0..receivedAtElapsedRealtimeNs)
            require(sourceTimestamp != null)
            require(sourceTimestamp.domain.kind == ClockDomainKind.ANDROID_ELAPSED_REALTIME)
            require(sourceTimestamp.meaning == SourceTimeMeaning.MEASUREMENT)
            require(sourceTimestamp.timestampNs == measurementAtElapsedRealtimeNs)
        }
    }

    val ageKnown: Boolean get() = measurementAtElapsedRealtimeNs != null
}
