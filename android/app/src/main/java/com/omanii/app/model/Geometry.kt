package com.omanii.app.model

import kotlin.math.abs

/**
 * Binding of the session-local spatial model in protocol/README.md and ADR-003:
 * coordinates are metres in a right-handed frame with +Y up. Horizontal projection
 * uses XZ (xM, zM); yM retains height. The origin is local to the session/frame.
 */
data class PositionM(val xM: Double, val yM: Double, val zM: Double) {
    init { require(xM.isFinite() && yM.isFinite() && zM.isFinite()) }
}

/**
 * Orientation in that session-local frame: a normalized Hamilton quaternion in
 * XYZW order, mapping physical-camera-local vectors to session-local vectors.
 * Canonical spatial model: protocol/README.md and ADR-003.
 */
data class QuaternionXyzw(val x: Double, val y: Double, val z: Double, val w: Double) {
    init {
        require(x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite())
        require(abs(x * x + y * y + z * z + w * w - 1.0) <= 1e-6)
    }
}
