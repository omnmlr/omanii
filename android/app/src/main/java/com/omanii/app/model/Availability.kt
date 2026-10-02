package com.omanii.app.model

enum class Availability {
    AVAILABLE,
    NOT_SUPPORTED,
    PERMISSION_DENIED,
    REDACTED,
    NOT_APPLICABLE,
    TEMPORARILY_UNAVAILABLE,
    UNKNOWN,
    NOT_EVALUATED,
}

/** Canonical meaning: protocol/README.md, availability contract. */
data class ValueState<T : Any>(
    val value: T?,
    val availability: Availability,
    val reason: String?,
) {
    init {
        require((availability == Availability.AVAILABLE) == (value != null))
        require(reason == null || reason.isNotBlank())
    }
}
