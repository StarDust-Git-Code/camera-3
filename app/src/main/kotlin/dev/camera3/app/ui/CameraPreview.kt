package dev.camera3.app.ui

import android.hardware.display.DisplayManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dev.camera3.app.Camera3Application
import dev.camera3.model.CameraDescriptor
import dev.camera3.model.CameraPolicy
import dev.camera3.model.Dimensions
import dev.camera3.model.LensFacing
import dev.camera3.model.LogLevel

@Composable
fun CameraPreview(
    camera: CameraDescriptor,
    preview: Dimensions,
    grid: Boolean,
    modifier: Modifier = Modifier,
    attach: (Surface, Int) -> Unit,
    detach: (Surface) -> Unit,
) {
    val context = LocalContext.current
    val logger = remember(context) { (context.applicationContext as? Camera3Application)?.services?.logger }
    val attachLatest by rememberUpdatedState(attach)
    val detachLatest by rememberUpdatedState(detach)
    var rotation by remember { mutableIntStateOf(context.display?.rotation ?: Surface.ROTATION_0) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(id: Int) = Unit
            override fun onDisplayRemoved(id: Int) = Unit
            override fun onDisplayChanged(id: Int) { rotation = context.display?.rotation ?: Surface.ROTATION_0 }
        }
        manager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { manager.unregisterDisplayListener(listener) }
    }
    val relative = CameraPolicy.relativeRotation(camera.sensorOrientation, rotation * 90, camera.facing == LensFacing.FRONT)
    val aspect = if (relative % 180 == 0) preview.width.toFloat() / preview.height
        else preview.height.toFloat() / preview.width
    // Fit rather than stretch or silently crop. SurfaceView performs camera/display rotation.
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val previewModifier = if (maxWidth / maxHeight > aspect) Modifier.fillMaxHeight().aspectRatio(aspect)
            else Modifier.fillMaxWidth().aspectRatio(aspect)
        Box(previewModifier) {
            key(camera.id, preview.width, preview.height) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            val surfaceView = this
                            holder.setFixedSize(preview.width, preview.height)
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    val display = surfaceView.display
                                    val displayRotation = display?.rotation ?: Surface.ROTATION_0
                                    logger?.log(LogLevel.DEBUG, "Preview", "P-016 preview.surfaceCreated cameraId=${camera.id} surfaceValid=${holder.surface.isValid} surfaceHash=${System.identityHashCode(holder.surface)} displayPresent=${display != null} displayRotation=$displayRotation")
                                    attachLatest(holder.surface, displayRotation)
                                }
                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                                    val display = surfaceView.display
                                    val displayRotation = display?.rotation ?: Surface.ROTATION_0
                                    logger?.log(LogLevel.DEBUG, "Preview", "P-016 preview.surfaceChanged cameraId=${camera.id} format=$format size=${width}x$height surfaceValid=${holder.surface.isValid} surfaceHash=${System.identityHashCode(holder.surface)} displayPresent=${display != null} displayRotation=$displayRotation")
                                    attachLatest(holder.surface, displayRotation)
                                }
                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    logger?.log(LogLevel.DEBUG, "Preview", "P-016 preview.surfaceDestroyed cameraId=${camera.id} surfaceValid=${holder.surface.isValid} surfaceHash=${System.identityHashCode(holder.surface)}")
                                    detachLatest(holder.surface)
                                }
                            })
                            logger?.log(LogLevel.DEBUG, "Preview", "P-016 preview.callbackRegistered cameraId=${camera.id} preview=${preview.width}x${preview.height} surfaceValid=${holder.surface.isValid} surfaceHash=${System.identityHashCode(holder.surface)}")
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { view ->
                        logger?.log(LogLevel.DEBUG, "Preview", "P-016 preview.onRelease cameraId=${camera.id} surfaceValid=${view.holder.surface.isValid} surfaceHash=${System.identityHashCode(view.holder.surface)}")
                        detachLatest(view.holder.surface)
                    },
                )
            }
            if (grid) Canvas(Modifier.fillMaxSize()) {
                for (i in 1..2) {
                    drawLine(Color.White.copy(alpha = 0.28f), Offset(size.width * i / 3, 0f), Offset(size.width * i / 3, size.height))
                    drawLine(Color.White.copy(alpha = 0.28f), Offset(0f, size.height * i / 3), Offset(size.width, size.height * i / 3))
                }
            }
        }
    }
}
