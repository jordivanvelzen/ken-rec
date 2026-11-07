package com.kendamasenses.kenrec.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kendamasenses.kenrec.AppSettings
import com.kendamasenses.kenrec.StartCamera
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/** In the last stretch before the restart, the screen offers to restart right away. */
private const val RESTART_WARNING_MS = 90_000L

/** How long a stop may take before we give up and let the person try again. */
private const val STOP_TIMEOUT_MS = 20_000L

private fun has(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

@Composable
fun RecordScreen(banner: String?, onRecorded: (String) -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    var cameraOk by remember { mutableStateOf(has(context, Manifest.permission.CAMERA)) }
    var micOk by remember { mutableStateOf(has(context, Manifest.permission.RECORD_AUDIO)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        cameraOk = has(context, Manifest.permission.CAMERA)
        micOk = has(context, Manifest.permission.RECORD_AUDIO)
    }
    // Coming back from the phone's settings: look again.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        cameraOk = has(context, Manifest.permission.CAMERA)
        micOk = has(context, Manifest.permission.RECORD_AUDIO)
    }
    LaunchedEffect(Unit) {
        if (!cameraOk) launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Logo()
        Spacer(Modifier.height(12.dp))
        if (cameraOk) {
            CameraBody(micOk = micOk, banner = banner, onRecorded = onRecorded, onOpenSettings = onOpenSettings)
        } else {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Ken Rec needs the camera to record. The microphone is optional.",
                    color = Senses.Ink,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(16.dp))
                PillButton(
                    text = "Open settings",
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private suspend fun awaitProvider(context: Context): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
}

@SuppressLint("MissingPermission")
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.CameraBody(
    micOk: Boolean,
    banner: String?,
    onRecorded: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Read once when the camera screen opens: changes in Settings apply to the next recording.
    val settings = remember { AppSettings(context) }
    val quality = remember { settings.quality }
    val maxMs = remember { settings.restartAfterMs }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER } }
    val recorder = remember {
        val selector = if (quality.maxHeight <= 720) {
            QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
        } else {
            QualitySelector.fromOrderedList(
                listOf(Quality.FHD, Quality.HD, Quality.SD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
            )
        }
        Recorder.Builder()
            .setQualitySelector(selector)
            .apply { quality.targetBitsPerSecond?.let { setTargetVideoEncodingBitRate(it) } }
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    var lens by remember {
        mutableIntStateOf(
            if (settings.startCamera == StartCamera.Front) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK,
        )
    }
    var rebind by remember { mutableIntStateOf(0) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var stopping by remember { mutableStateOf(false) }
    var restarting by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lens, rebind) {
        val provider = awaitProvider(context)
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        try {
            provider.unbindAll()
            // A phone without the wanted camera falls back to the other one.
            val wanted = CameraSelector.Builder().requireLensFacing(lens).build()
            val selector = if (runCatching { provider.hasCamera(wanted) }.getOrDefault(false)) wanted else CameraSelector.DEFAULT_BACK_CAMERA
            provider.bindToLifecycle(lifecycleOwner, selector, preview, videoCapture)
        } catch (e: Exception) {
            error = "Could not start the camera."
        }
    }

    // Leaving this screen (to trim) lets go of the camera.
    DisposableEffect(Unit) {
        onDispose {
            recording?.stop()
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    // A stop that never finishes must not freeze the screen.
    LaunchedEffect(stopping) {
        if (stopping) {
            delay(STOP_TIMEOUT_MS)
            if (stopping) {
                stopping = false
                restarting = false
                recording = null
                error = "Stopping took too long. Try again."
                rebind++
            }
        }
    }

    fun start() {
        error = null
        elapsedMs = 0
        val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.mp4")
        val options = FileOutputOptions.Builder(file).setDurationLimitMillis(maxMs).build()
        var pending = recorder.prepareRecording(context, options)
        if (micOk) pending = pending.withAudioEnabled()
        recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Status -> elapsedMs = event.recordingStats.recordedDurationNanos / 1_000_000
                is VideoRecordEvent.Finalize -> {
                    recording = null
                    stopping = false
                    val limitReached = event.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
                    if (restarting || limitReached) {
                        // Throw the old recording away and start a fresh one at once.
                        restarting = false
                        file.delete()
                        start()
                    } else if (!event.hasError() && file.length() > 0) {
                        onRecorded(file.absolutePath)
                    } else {
                        file.delete()
                        error = "The recording did not finish. Try again."
                    }
                }
                else -> Unit
            }
        }
    }

    val isRecording = recording != null

    if (banner != null && !isRecording) {
        Text(banner, color = Senses.Mint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
    }

    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Senses.Surface),
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (isRecording) {
            Row(
                Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Senses.BgDeep.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Senses.Coral))
                Spacer(Modifier.size(8.dp))
                Text(formatClock(elapsedMs), color = Senses.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            if (elapsedMs >= maxMs - RESTART_WARNING_MS && !stopping) {
                RestartBanner(
                    remainingMs = (maxMs - elapsedMs).coerceAtLeast(0),
                    onRestart = {
                        restarting = true
                        stopping = true
                        recording?.stop()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        } else {
            RoundIconButton(
                icon = Icons.Default.Settings,
                description = "Settings",
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )
            RoundIconButton(
                icon = Icons.Default.Refresh,
                description = "Flip camera",
                onClick = {
                    lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    error?.let {
        Text(it, color = Senses.Coral, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
    }
    if (!isRecording) {
        PillButton("Record", onClick = ::start, modifier = Modifier.fillMaxWidth())
    } else {
        PillButton(
            text = if (stopping) "Stopping..." else "Stop",
            onClick = { stopping = true; recording?.stop() },
            style = PillStyle.Danger,
            enabled = !stopping,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun formatClock(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Shown over the preview in the last 90 seconds before the automatic restart. */
@Composable
private fun RestartBanner(remainingMs: Long, onRestart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Senses.BgDeep.copy(alpha = 0.88f))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Restarting in ${formatClock(remainingMs)}. The old recording is thrown away.",
            color = Senses.Ink,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))
        PillButton("Restart now", onClick = onRestart, modifier = Modifier.fillMaxWidth())
    }
}
