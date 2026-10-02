package com.omanii.app.model

fun interface MonotonicClock {
    fun nowElapsedRealtimeNs(): Long
}
