package com.omanii.app.spatial

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import com.omanii.app.pose.ArCoreFrameSource
import com.omanii.app.pose.PoseFrameIdentity
import com.omanii.app.pose.PoseProvider
import com.omanii.app.pose.PoseReason
import com.omanii.app.pose.PoseRecord
import com.omanii.app.pose.PoseRecordKind
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** GL surface only services ARCore updates. It never renders/retains camera pixels. */
internal class PoseDebugSurface(
    private val activity: ComponentActivity,
    nowNs: () -> Long,
    onRecord: (PoseRecord) -> Unit,
) : GLSurfaceView(activity) {
    private val closed = AtomicBoolean(false)
    private val source = ArCoreFrameSource.create(activity)
    val sessionId = UUID.randomUUID().toString()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var segment = 0
    private fun identity() = PoseFrameIdentity("debug_${sessionId}_s$segment", "debug_${sessionId}_f$segment")
    private val provider = PoseProvider(source, sessionId, identity(), { segment++; identity() }, nowNs, { record ->
        // Always enqueue, including UI-thread STOP. Control events must precede their terminal record.
        mainHandler.post {
            if (!closed.get() || record.kind != PoseRecordKind.SAMPLE) onRecord(record)
        }
    }, dispatchCleanup = { action ->
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post { action() }
    })
    val cleanup get() = provider.cleanup
    private var textureId = 0
    private var widthPx = 1
    private var heightPx = 1

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(object : Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                if (textureId != 0) provider.discontinuity()
                val names = IntArray(1)
                GLES20.glGenTextures(1, names, 0)
                textureId = names[0]
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            }
            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) { widthPx = width; heightPx = height }
            override fun onDrawFrame(gl: GL10?) {
                GLES20.glClearColor(0.08f, 0.08f, 0.08f, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                if (closed.get()) return
                @Suppress("DEPRECATION")
                val rotation = activity.windowManager.defaultDisplay.rotation
                provider.prepareGl(textureId, rotation, widthPx, heightPx)
                provider.update()
            }
        })
        provider.start()
    }

    fun discontinuity() = provider.discontinuity()
    fun finish(reason: PoseReason) {
        if (!closed.compareAndSet(false, true)) return
        provider.stop(reason) // Session.pause before GLSurfaceView.onPause; stop emissions immediately.
        queueEvent {
            if (textureId != 0) GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
        onPause()
    }
}
