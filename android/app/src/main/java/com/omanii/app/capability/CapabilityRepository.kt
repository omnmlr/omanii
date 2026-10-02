package com.omanii.app.capability

import com.omanii.app.model.*

enum class CapabilitySupport { SUPPORTED }
data class CapabilityReport(
    val reportId: String,
    val createdAtElapsedRealtimeNs: Long,
    val device: DeviceMetadata,
    val permissions: Map<String, ValueState<Boolean>>,
    val observations: Map<String, ValueState<CapabilitySupport>>,
    val profiles: Map<String, ValueState<CapabilitySupport>>,
    val arPose: ValueState<CapabilitySupport>,
)
fun interface CapabilityRepository { fun getCapabilityReport(): CapabilityReport }

object CapabilityGate {
    fun evaluate(api: Boolean, hardware: Boolean, service: Boolean, permission: Boolean): ValueState<CapabilitySupport> = when {
        !api || !hardware -> ValueState(null, Availability.NOT_SUPPORTED, "API or hardware absent")
        !service -> ValueState(null, Availability.TEMPORARILY_UNAVAILABLE, "Service absent")
        !permission -> ValueState(null, Availability.PERMISSION_DENIED, "Required permission absent")
        else -> ValueState(CapabilitySupport.SUPPORTED, Availability.AVAILABLE, "Adapter/API gates satisfied; runtime result may still be unavailable")
    }
}
