package com.omanii.app.pose

import com.google.ar.core.ArCoreApk
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CompletableFuture

class PoseCapabilityTest {
    @Test fun constructionAndInspectionDoNotRequestCamera() {
        val gate = PoseEntryGate()
        assertEquals(PoseEntryAction.WAIT, gate.next())
        assertEquals(PoseEntryAction.WAIT, gate.availability(ArRuntimeState.AVAILABLE))
        assertEquals(CameraPermissionState.NOT_REQUESTED, gate.capability.camera)
        assertEquals(PoseEntryAction.REQUEST_CAMERA, gate.enter())
    }
    @Test fun unsupportedIsFallbackWithoutCameraRequest() {
        val gate = PoseEntryGate(); gate.availability(ArRuntimeState.UNSUPPORTED)
        assertEquals(PoseEntryAction.FALLBACK, gate.enter()); assertFalse(gate.capability.canCreateSession)
        assertTrue(gate.capability.manualFallbackAvailable)
    }
    @Test fun bothCameraDenialStatesPreserveFallback() {
        listOf(CameraPermissionState.DENIED, CameraPermissionState.DENIED_NO_RATIONALE).forEach { denial ->
            val gate = PoseEntryGate(); gate.availability(ArRuntimeState.AVAILABLE); gate.permission(denial)
            assertEquals(PoseEntryAction.FALLBACK, gate.enter()); assertTrue(gate.capability.manualFallbackAvailable)
        }
    }
    @Test fun installAndUpdatePromptThenContinueWithFalse() {
        listOf(ArRuntimeState.INSTALL_REQUIRED, ArRuntimeState.UPDATE_REQUIRED).forEach { runtime ->
            val gate = PoseEntryGate(); gate.availability(runtime)
            assertEquals(PoseEntryAction.REQUEST_INSTALL, gate.enter()); assertTrue(gate.userRequestedInstall)
            assertEquals(PoseEntryAction.WAIT, gate.installRequested()); assertFalse(gate.userRequestedInstall)
            assertEquals(PoseEntryAction.REQUEST_CAMERA, gate.availability(ArRuntimeState.AVAILABLE))
        }
    }
    @Test fun cancellationIgnoresLateAvailabilityResult() {
        val gate = PoseEntryGate(); gate.enter(); gate.cancel()
        assertEquals(PoseEntryAction.WAIT, gate.availability(ArRuntimeState.AVAILABLE))
    }
    @Test fun runtimeUnknownDoesNotMasqueradeAsUnsupported() {
        assertEquals(ArRuntimeState.TIMED_OUT, ArCoreRuntime.map(ArCoreApk.Availability.UNKNOWN_TIMED_OUT))
        assertEquals(ArRuntimeState.UNKNOWN_ERROR, ArCoreRuntime.map(ArCoreApk.Availability.UNKNOWN_ERROR))
        assertEquals(ArRuntimeState.CHECKING, ArCoreRuntime.map(ArCoreApk.Availability.UNKNOWN_CHECKING))
        assertEquals(ArRuntimeState.AVAILABLE, ArCoreRuntime.map(ArCoreApk.Availability.SUPPORTED_INSTALLED))
    }

    @Test fun initializationCleanupWrapperPreservesCameraDenial() {
        val failure = ArCoreInitializationException(SecurityException(), CompletableFuture.completedFuture(Unit))
        assertTrue(ArCoreRuntime.cameraDenied(failure))
        assertFalse(ArCoreRuntime.cameraDenied(IllegalStateException()))
    }
}
