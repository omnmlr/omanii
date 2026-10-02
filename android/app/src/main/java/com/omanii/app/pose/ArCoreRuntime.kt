package com.omanii.app.pose

import android.app.Activity
import android.content.Context
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

object ArCoreRuntime {
    fun check(context: Context, result: (ArRuntimeState) -> Unit) {
        try {
            ArCoreApk.getInstance().checkAvailabilityAsync(context) { result(map(it)) }
        } catch (_: Exception) { result(ArRuntimeState.UNKNOWN_ERROR) }
    }

    /** Main thread, explicit entry only. Returns INSTALLING when the activity will pause for installation. */
    fun install(activity: Activity, userRequestedInstall: Boolean): ArRuntimeState = try {
        when (ArCoreApk.getInstance().requestInstall(activity, userRequestedInstall)) {
            ArCoreApk.InstallStatus.INSTALLED -> ArRuntimeState.AVAILABLE
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> ArRuntimeState.INSTALLING
        }
    } catch (e: Exception) { failure(e) }

    fun failure(error: Exception): ArRuntimeState = when (error) {
        is ArCoreInitializationException -> failure(error.initializationFailure)
        is UnavailableDeviceNotCompatibleException -> ArRuntimeState.UNSUPPORTED
        is UnavailableArcoreNotInstalledException -> ArRuntimeState.INSTALL_REQUIRED
        is UnavailableApkTooOldException -> ArRuntimeState.UPDATE_REQUIRED
        is UnavailableUserDeclinedInstallationException -> ArRuntimeState.INSTALL_DECLINED
        is UnavailableSdkTooOldException -> ArRuntimeState.INITIALIZATION_FAILED
        else -> ArRuntimeState.INITIALIZATION_FAILED
    }

    fun cameraDenied(error: Exception): Boolean = error is SecurityException ||
        (error is ArCoreInitializationException && cameraDenied(error.initializationFailure))

    internal fun map(value: ArCoreApk.Availability): ArRuntimeState = when (value) {
        ArCoreApk.Availability.UNKNOWN_CHECKING -> ArRuntimeState.CHECKING
        ArCoreApk.Availability.UNKNOWN_ERROR -> ArRuntimeState.UNKNOWN_ERROR
        ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> ArRuntimeState.TIMED_OUT
        ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> ArRuntimeState.UNSUPPORTED
        ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> ArRuntimeState.INSTALL_REQUIRED
        ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> ArRuntimeState.UPDATE_REQUIRED
        ArCoreApk.Availability.SUPPORTED_INSTALLED -> ArRuntimeState.AVAILABLE
    }
}
