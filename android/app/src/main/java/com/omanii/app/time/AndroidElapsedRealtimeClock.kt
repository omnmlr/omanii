package com.omanii.app.time

import android.os.SystemClock
import com.omanii.app.model.MonotonicClock

object AndroidElapsedRealtimeClock : MonotonicClock {
    override fun nowElapsedRealtimeNs(): Long = SystemClock.elapsedRealtimeNanos()
}
