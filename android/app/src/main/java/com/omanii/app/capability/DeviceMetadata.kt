package com.omanii.app.capability

/**
 * Standard device hardware and platform metadata.
 */
data class DeviceMetadata(
    val manufacturer: String,
    val model: String,
    val brand: String,
    val device: String,
    val apiLevel: Int,
    val releaseVersion: String,
    val appVersionName: String,
    val appVersionCode: Long
)
