package com.omanii.app.spatial

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.omanii.app.model.MonotonicClock
import com.omanii.app.time.AndroidElapsedRealtimeClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.omanii.app.pose.ArCoreRuntime
import com.omanii.app.pose.ArCoreInitializationException
import com.omanii.app.pose.ArRuntimeState
import com.omanii.app.pose.CameraPermissionState
import com.omanii.app.pose.CanonicalPoseRecord
import com.omanii.app.pose.PoseEntryAction
import com.omanii.app.pose.PoseEntryGate
import com.omanii.app.pose.PoseJsonl
import com.omanii.app.pose.PoseReason
import com.omanii.app.pose.PoseRecord
import com.omanii.app.pose.PoseRecordKind
import java.util.concurrent.CompletableFuture

/** Task-only debug entry using the approved injected elapsed-realtime clock and explicit build label. */
@Composable
fun PoseDebugPanel(activity: ComponentActivity, appBuild: String, clock: MonotonicClock = AndroidElapsedRealtimeClock) {
    val gate = remember { PoseEntryGate() }
    val records = remember { mutableStateListOf<PoseRecord>() }
    val canonicalRecords = remember { mutableStateListOf<CanonicalPoseRecord>() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var surface by remember { mutableStateOf<PoseDebugSurface?>(null) }
    var capability by remember { mutableStateOf(gate.capability) }
    var message by remember { mutableStateOf("Start pose debug to check AR availability and camera permission") }
    var waitingPermission by remember { mutableStateOf(false) }
    var waitingInstall by remember { mutableStateOf(false) }
    var alive by remember { mutableStateOf(true) }
    var requestEpoch by remember { mutableIntStateOf(0) }
    var recordsSessionId by remember { mutableStateOf<String?>(null) }
    var cleanupState by remember { mutableStateOf("none") }
    var exportBusy by remember { mutableStateOf(false) }
    var exportPending by remember { mutableStateOf<String?>(null) }
    var process by remember { mutableStateOf<(PoseEntryAction) -> Unit>({}) }
    fun observeCleanup(future: CompletableFuture<Unit>?) {
        if (future == null) return
        cleanupState = "closing"
        future.whenComplete { _, failure -> mainHandler.post {
            if (alive) cleanupState = if (failure == null) "released" else "release failed"
        } }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-ndjson")) { uri ->
        val contents = exportPending
        exportPending = null
        if (uri != null && contents != null) {
            exportBusy = true
            Thread({
                val saved = try {
                    val stream = activity.contentResolver.openOutputStream(uri) ?: error("No document stream")
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(contents) }
                    true
                } catch (_: Exception) { false }
                mainHandler.post {
                    if (alive) {
                        exportBusy = false
                        message = if (saved) "Export saved: session-local coordinates only" else "Export failed"
                    }
                }
            }, "omanii-pose-export").start()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        waitingPermission = false
        if (alive && gate.entryRequested) {
            val state = if (granted) CameraPermissionState.GRANTED else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                CameraPermissionState.DENIED else CameraPermissionState.DENIED_NO_RATIONALE
            process(gate.permission(state))
        }
    }
    fun stop(reason: PoseReason) {
        requestEpoch++; gate.cancel()
        waitingPermission = false; waitingInstall = false
        val ending = surface
        ending?.finish(reason)
        observeCleanup(ending?.cleanup)
        surface = null
    }
    process = { action ->
        capability = gate.capability
        if (alive && gate.entryRequested) when (action) {
            PoseEntryAction.CHECK_AVAILABILITY -> {
                val epoch = requestEpoch
                message = "Checking ARCore support"
                ArCoreRuntime.check(activity) { state ->
                    if (alive && epoch == requestEpoch && gate.entryRequested) process(gate.availability(state))
                }
            }
            PoseEntryAction.REQUEST_INSTALL -> {
                val state = ArCoreRuntime.install(activity, gate.userRequestedInstall)
                if (state == ArRuntimeState.INSTALLING) {
                    waitingInstall = true; gate.installRequested(); capability = gate.capability
                    message = "ARCore install/update requested"
                } else process(gate.availability(state))
            }
            PoseEntryAction.REQUEST_CAMERA -> {
                waitingPermission = true
                gate.permission(CameraPermissionState.REQUESTING); capability = gate.capability
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            PoseEntryAction.START_CAPTURE -> {
                if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    records.clear()
                    canonicalRecords.clear()
                    try {
                        val created = PoseDebugSurface(activity, clock, appBuild) { canonical ->
                            val record = canonical.debugRecord
                            if (alive && record.sessionId == recordsSessionId) {
                                records += record
                                canonicalRecords += canonical
                                if (record.kind == PoseRecordKind.STOP) {
                                    message = "Stopped: ${record.reason}"
                                    if (record.reason == PoseReason.CAMERA_DENIED) {
                                        gate.permission(CameraPermissionState.DENIED); capability = gate.capability
                                    }
                                    val ended = surface
                                    ended?.finish(record.reason ?: PoseReason.UPDATE_FAILED)
                                    observeCleanup(ended?.cleanup)
                                    surface = null
                                } else if (records.size >= 3000 && surface != null) stop(PoseReason.BUFFER_LIMIT)
                            }
                        }
                        recordsSessionId = created.sessionId
                        surface = created
                        gate.captureStarted()
                        message = "Pose debug active; route uses observed keyframes only"
                    } catch (e: Exception) {
                        gate.availability(ArCoreRuntime.failure(e)); capability = gate.capability
                        if (ArCoreRuntime.cameraDenied(e)) {
                            gate.permission(CameraPermissionState.DENIED); capability = gate.capability
                        }
                        if (e is ArCoreInitializationException) observeCleanup(e.cleanup)
                        gate.cancel(); message = "AR initialization failed; manual workflow remains available"
                    }
                } else message = "Resume foreground before starting capture"
            }
            PoseEntryAction.FALLBACK -> { gate.cancel(); message = "AR unavailable: manual two-spot workflow capability retained" }
            PoseEntryAction.WAIT -> Unit
        }
    }
    DisposableEffect(activity) {
        alive = true
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (surface != null) stop(PoseReason.BACKGROUNDED)
                    else if (!waitingInstall && !waitingPermission) stop(PoseReason.BACKGROUNDED)
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (waitingInstall && gate.entryRequested) {
                        waitingInstall = false
                        val state = ArCoreRuntime.install(activity, false)
                        if (state == ArRuntimeState.INSTALLING) waitingInstall = true else process(gate.availability(state))
                    } else if (gate.entryRequested && gate.capability.canCreateSession) process(gate.next())
                }
                Lifecycle.Event.ON_DESTROY -> { stop(PoseReason.OWNER_DESTROYED); alive = false }
                else -> Unit
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { stop(PoseReason.OWNER_DESTROYED); alive = false; activity.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        BasicText("Pose debug — development evidence only")
        BasicText("Google provides this feature through Google Play Services for AR (ARCore). Google's Terms of Service and Privacy Policy apply.")
        DebugAction("Google Privacy Policy", true) { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/privacy"))) }
        DebugAction("Google Terms of Service", true) { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/terms"))) }
        BasicText("Runtime: ${capability.runtime}; camera: ${capability.camera}")
        BasicText(message)
        BasicText("AR resource cleanup: $cleanupState")
        DebugAction("Start pose capture", enabled = surface == null && !gate.entryRequested) {
            requestEpoch++
            gate.permission(if (activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                CameraPermissionState.GRANTED else CameraPermissionState.NOT_REQUESTED)
            gate.availability(ArRuntimeState.CHECKING)
            process(gate.enter())
        }
        DebugAction("Cancel", surface != null || gate.entryRequested) { stop(PoseReason.CANCELLED) }
        DebugAction("End capture", surface != null) { stop(PoseReason.COMPLETED) }
        DebugAction("Mark discontinuity", surface != null) { surface?.discontinuity() }
        DebugAction("Export local pose JSONL", !exportBusy && surface == null && records.lastOrNull()?.kind == PoseRecordKind.STOP) {
            exportPending = PoseJsonl.encodeCanonical(canonicalRecords.toList())
            exportLauncher.launch("pose-debug.jsonl")
        }
        surface?.let { active -> AndroidView(factory = { active }, modifier = Modifier.fillMaxWidth().height(48.dp)) }
        val current = records.lastOrNull()
        BasicText("Tracking: ${current?.tracking ?: "not started"}; events: ${records.size}")
        records.lastOrNull { it.pose != null }?.let { r ->
            val p = r.pose!!.position
            BasicText("Last observed x/y/z (m): ${p.xM}, ${p.yM}, ${p.zM}; ${r.identity.segmentId}")
        }
        val traces = poseDebugTraces(records)
        traces.forEach { trace ->
            BasicText("${trace.identity.coordinateFrameId} — x/z (m), origin circle")
            Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                val extent = maxOf(1.0, trace.observedPoints.maxOf { maxOf(kotlin.math.abs(it.xM), kotlin.math.abs(it.zM)) })
                val scale = minOf(size.width, size.height) * 0.4f / extent.toFloat()
                fun screen(x: Double, z: Double) = Offset(size.width / 2 + x.toFloat() * scale, size.height / 2 - z.toFloat() * scale)
                drawCircle(Color.Gray, radius = 5f, center = screen(0.0, 0.0))
                // Straight keyframe connections are route guides, not observations between endpoints.
                trace.observedPoints.zipWithNext().forEach { (a, b) -> drawLine(Color.Gray, screen(a.xM, a.zM), screen(b.xM, b.zM)) }
                trace.observedPoints.forEach { p -> drawCircle(Color.Black, radius = 3f, center = screen(p.xM, p.zM)) }
            }
        }
        BasicText("Dots: observed poses. Lines: keyframe guides. Separate panels preserve frame breaks. No room or network geometry.")
    }
}

@Composable
private fun DebugAction(label: String, enabled: Boolean, action: () -> Unit) {
    BasicText(label + if (enabled) "" else " (unavailable)", Modifier.padding(vertical = 8.dp).clickable(enabled = enabled, onClick = action))
}
