package com.omanii.app.network.provider

import com.omanii.app.model.*

object RadioAccessGate {
    fun cell(hardware: Boolean, service: Boolean, fineLocation: Boolean, locationEnabled: ValueState<Boolean>): ValueState<Boolean> = when {
        !hardware -> ValueState(null, Availability.NOT_SUPPORTED, "No radio-access feature")
        !service -> ValueState(null, Availability.TEMPORARILY_UNAVAILABLE, "Telephony service absent")
        !fineLocation -> ValueState(null, Availability.PERMISSION_DENIED, "getAllCellInfo requires fine location; coarse is insufficient")
        locationEnabled.value == null -> ValueState(null, locationEnabled.availability, "Location state unavailable")
        locationEnabled.value == false -> ValueState(null, Availability.TEMPORARILY_UNAVAILABLE, "Location disabled")
        else -> ValueState(true, Availability.AVAILABLE, null)
    }
}
