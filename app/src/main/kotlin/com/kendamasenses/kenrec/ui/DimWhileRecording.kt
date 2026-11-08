package com.kendamasenses.kenrec.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay

/** A fully red dot, so it reads as a recording light. */
private val RecordingRed = Color(0xFFFF1010)

/** The red dot is on for this long, then off for this long. */
private const val BLINK_MS = 700L

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** How the recording is going, written by the camera screen and read by the dim screen only. */
class RecordingProgress {
    var recording by mutableStateOf(false)
    var elapsedMs by mutableLongStateOf(0L)
    var maxMs by mutableLongStateOf(0L)
}

/**
 * Saves battery during a long recording. While [RecordingProgress.recording], and after [dimAfterSeconds] without
 * a touch, the screen goes black (system bars hidden) with a blinking red dot, the time, and a warning in the
 * last stretch before the automatic restart. Any tap, or the back button, wakes it. The camera screen underneath
 * stays as it is, so the recording is untouched and the screen stays on (the camera needs that).
 *
 * The backlight is not lowered on purpose: brightness is one setting for the whole screen, so lowering it would
 * dim the dot too. On an OLED screen black pixels are off anyway, so the battery saving does not need it.
 */
@Composable
fun DimWhileRecording(progress: RecordingProgress, dimAfterSeconds: Int, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var dimmed by remember { mutableStateOf(false) }
    var touches by remember { mutableIntStateOf(0) }
    val enabled = progress.recording && dimAfterSeconds > 0

    // Dim after a quiet spell. Every touch (or a change of state) starts the wait again.
    LaunchedEffect(enabled, touches) {
        if (!enabled) {
            dimmed = false
        } else {
            delay(dimAfterSeconds * 1000L)
            dimmed = true
        }
    }

    DisposableEffect(dimmed) {
        val window = context.findActivity()?.window
        if (dimmed && window != null) {
            WindowCompat.getInsetsController(window, view).hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (window != null) {
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    fun wake() {
        dimmed = false
        touches++
    }

    BackHandler(enabled = dimmed) { wake() }

    Box(
        Modifier
            .fillMaxSize()
            // Watches for touches without taking them, so the screen below works as usual.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed && !it.previousPressed }) touches++
                    }
                }
            },
    ) {
        content()
        if (dimmed) DimScreen(progress = progress, onWake = ::wake)
    }
}

@Composable
private fun DimScreen(progress: RecordingProgress, onWake: () -> Unit) {
    var on by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(BLINK_MS)
            on = !on
        }
    }
    // Whole seconds only, so the screen redraws once a second and not on every update.
    val elapsedSeconds by remember { derivedStateOf { progress.elapsedMs / 1000 } }
    val remainingSeconds = (progress.maxMs / 1000 - elapsedSeconds).coerceAtLeast(0)
    val restartSoon = progress.maxMs > 0 && remainingSeconds * 1000 <= RESTART_WARNING_MS
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Wake the screen",
                onClick = onWake,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(if (on) RecordingRed else Color.Transparent))
            Spacer(Modifier.height(20.dp))
            Text(
                formatClock(elapsedSeconds * 1000),
                color = Senses.Ink.copy(alpha = 0.75f),
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
            )
            if (restartSoon) {
                Spacer(Modifier.height(28.dp))
                Text(
                    "Restarting in ${formatClock(remainingSeconds * 1000)}",
                    color = Senses.Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "The old recording is thrown away. Tap to stop or restart.",
                    color = Senses.Ink.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}
