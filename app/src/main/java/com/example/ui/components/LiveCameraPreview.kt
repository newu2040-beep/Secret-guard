package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Renders a real hardware CameraX `PreviewView` configured for Full HD 1080p (1080x1920)
 * resolution, supporting front/rear camera switching and low-latency high-definition JPEG frame
 * streaming for real-time multi-device video calls.
 */
@Composable
fun LiveCameraPreview(
    isFrontCamera: Boolean,
    isFullHd1080p: Boolean = true,
    onFrameCaptured: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, isFrontCamera, isFullHd1080p) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val targetSize = if (isFullHd1080p) Size(1080, 1920) else Size(720, 1280)
                val resolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            targetSize,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                val selector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                cameraProvider.unbindAll()
                if (cameraProvider.hasCamera(selector)) {
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview
                    )
                }
            } catch (e: Exception) {
                Log.w("LiveCameraPreview", "CameraX Full HD 1080p bind warning: ${e.message}")
            }
        }

        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    if (onFrameCaptured != null) {
        LaunchedEffect(previewView, isFrontCamera, isFullHd1080p) {
            while (true) {
                delay(480L)
                val bmp: Bitmap? = withContext(Dispatchers.Main) {
                    runCatching { previewView.bitmap }.getOrNull()
                }
                if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                    val encoded = withContext(Dispatchers.Default) {
                        runCatching {
                            val maxEdge = if (isFullHd1080p) 720 else 540
                            val scale = minOf(
                                maxEdge.toFloat() / bmp.width.coerceAtLeast(1),
                                maxEdge.toFloat() / bmp.height.coerceAtLeast(1),
                                1.0f
                            )
                            val targetW = (bmp.width * scale).toInt().coerceAtLeast(1)
                            val targetH = (bmp.height * scale).toInt().coerceAtLeast(1)
                            val scaled = Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
                            val out = ByteArrayOutputStream()
                            val quality = if (isFullHd1080p) 76 else 68
                            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                        }.getOrNull()
                    }
                    if (!encoded.isNullOrBlank() && encoded.length <= 280_000) {
                        onFrameCaptured(encoded)
                    }
                }
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

/**
 * Decodes and renders a high-definition Base64-encoded JPEG image (from a live 1080p video call frame,
 * HD photo/video attachment, or `data:image/jpeg;base64,...` custom profile avatar) using high filter quality.
 */
@Composable
fun Base64BitmapImage(
    base64Data: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var imageBitmap by remember(base64Data) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(base64Data) {
        if (base64Data.isBlank()) {
            imageBitmap = null
            return@LaunchedEffect
        }
        imageBitmap = withContext(Dispatchers.Default) {
            runCatching {
                val cleanB64 = base64Data.substringAfter("base64,", base64Data).trim()
                val bytes = Base64.decode(cleanB64, Base64.NO_WRAP)
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }

    val bmp = imageBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = contentDescription,
            contentScale = contentScale,
            filterQuality = FilterQuality.High,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color.Black.copy(alpha = 0.20f)))
    }
}
