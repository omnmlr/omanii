package com.omanii.app.pose

enum class ArRuntimeState { CHECKING, UNKNOWN_ERROR, TIMED_OUT, UNSUPPORTED, INSTALL_REQUIRED, UPDATE_REQUIRED, AVAILABLE, INSTALLING, INSTALL_DECLINED, INITIALIZATION_FAILED }
enum class CameraPermissionState { NOT_REQUESTED, REQUESTING, GRANTED, DENIED, DENIED_NO_RATIONALE }
data class PoseCapability(val runtime: ArRuntimeState, val camera: CameraPermissionState) {
    val canCreateSession get() = runtime == ArRuntimeState.AVAILABLE && camera == CameraPermissionState.GRANTED
    val manualFallbackAvailable get() = true
}

enum class PoseEntryAction { CHECK_AVAILABILITY, REQUEST_INSTALL, REQUEST_CAMERA, START_CAPTURE, FALLBACK, WAIT }

/** No side effects on construction or capability inspection; actions require explicit feature entry. */
class PoseEntryGate {
    var capability = PoseCapability(ArRuntimeState.CHECKING, CameraPermissionState.NOT_REQUESTED)
        private set
    var entryRequested = false
        private set
    private var installPrompted = false
    val userRequestedInstall: Boolean get() = !installPrompted

    fun enter(): PoseEntryAction { entryRequested = true; installPrompted = false; return next() }
    fun availability(state: ArRuntimeState): PoseEntryAction {
        capability = capability.copy(runtime = state)
        return next()
    }
    fun permission(state: CameraPermissionState): PoseEntryAction {
        capability = capability.copy(camera = state)
        return next()
    }
    fun installRequested(): PoseEntryAction {
        installPrompted = true
        capability = capability.copy(runtime = ArRuntimeState.INSTALLING)
        return PoseEntryAction.WAIT
    }
    fun cancel() { entryRequested = false }
    fun captureStarted() { entryRequested = false }
    fun next(): PoseEntryAction {
        if (!entryRequested) return PoseEntryAction.WAIT
        return when (capability.runtime) {
            ArRuntimeState.CHECKING -> PoseEntryAction.CHECK_AVAILABILITY
            ArRuntimeState.INSTALL_REQUIRED, ArRuntimeState.UPDATE_REQUIRED -> PoseEntryAction.REQUEST_INSTALL
            ArRuntimeState.INSTALLING -> PoseEntryAction.WAIT
            ArRuntimeState.AVAILABLE -> when (capability.camera) {
                CameraPermissionState.GRANTED -> PoseEntryAction.START_CAPTURE
                CameraPermissionState.NOT_REQUESTED -> PoseEntryAction.REQUEST_CAMERA
                CameraPermissionState.REQUESTING -> PoseEntryAction.WAIT
                CameraPermissionState.DENIED, CameraPermissionState.DENIED_NO_RATIONALE -> PoseEntryAction.FALLBACK
            }
            else -> PoseEntryAction.FALLBACK
        }
    }
}
