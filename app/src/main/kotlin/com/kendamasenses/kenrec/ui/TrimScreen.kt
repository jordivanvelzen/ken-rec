package com.kendamasenses.kenrec.ui

import android.media.MediaMetadataRetriever
import android.util.Log
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kendamasenses.kenrec.MediaStoreSaver
import com.kendamasenses.kenrec.StreamTrim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** Only the last stretch of a recording can be trimmed: the trick is always at the end. */
private const val WINDOW_MS = 90_000L
private const val DEFAULT_CLIP_MS = 30_000L
private const val MIN_CLIP_MS = 1_000L

private enum class Phase { Editing, Cutting, Saving }

private fun readDurationMs(file: File): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (_: Exception) {
        0L
    } finally {
        retriever.release()
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun TrimScreen(rawPath: String, onSaved: () -> Unit, onDiscard: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val raw = remember(rawPath) { File(rawPath) }
    val totalMs = remember(rawPath) { readDurationMs(raw) }
    if (!raw.exists() || totalMs <= 0) {
        LaunchedEffect(Unit) { onDiscard() }
        return
    }

    val windowStart = maxOf(0L, totalMs - WINDOW_MS)
    var startMs by remember { mutableLongStateOf(maxOf(windowStart, totalMs - DEFAULT_CLIP_MS)) }
    var endMs by remember { mutableLongStateOf(totalMs) }
    var phase by remember { mutableStateOf(Phase.Editing) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }

    val player = remember(rawPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(raw)))
            prepare()
            seekTo(startMs)
            playWhenReady = false
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // The preview loops inside the chosen range, never the whole recording.
    LaunchedEffect(player) {
        while (true) {
            delay(40)
            val pos = player.currentPosition
            if (player.isPlaying && (pos >= endMs || pos < startMs - 500)) {
                player.seekTo(startMs)
            } else if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(startMs)
                player.play()
            }
        }
    }

    fun setStart(value: Long) {
        startMs = value.coerceIn(windowStart, maxOf(windowStart, endMs - MIN_CLIP_MS))
        player.seekTo(startMs)
    }

    fun setEnd(value: Long) {
        endMs = value.coerceIn(minOf(totalMs, startMs + MIN_CLIP_MS), totalMs)
        player.seekTo((endMs - 1000).coerceAtLeast(startMs))
    }

    fun discard() {
        raw.delete()
        onDiscard()
    }

    fun save() {
        scope.launch {
            error = null
            player.pause()
            val cut = File(context.cacheDir, "cut_${System.currentTimeMillis()}.mp4")
            try {
                phase = Phase.Cutting
                Log.i("KenRec", "cut: recording=${totalMs}ms start=${startMs}ms end=${endMs}ms")
                withContext(Dispatchers.IO) {
                    StreamTrim.trim(raw.absolutePath, cut.absolutePath, startMs * 1000, endMs * 1000)
                }
                phase = Phase.Saving
                Log.i("KenRec", "cut done: ${readDurationMs(cut)}ms")
                withContext(Dispatchers.IO) { MediaStoreSaver.saveVideo(context, cut) }
                cut.delete()
                raw.delete()
                onSaved()
            } catch (e: Exception) {
                cut.delete()
                error = "Could not save the clip: ${e.message}"
                phase = Phase.Editing
            }
        }
    }

    BackHandler(enabled = phase == Phase.Editing) { confirmDiscard = true }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Logo()
        Spacer(Modifier.height(12.dp))
        SectionTitle("Trim your clip")
        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Senses.Surface)
                .clickable(enabled = phase == Phase.Editing) {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        if (player.currentPosition !in startMs until endMs) player.seekTo(startMs)
                        player.play()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (!playing) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Senses.BgDeep.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Senses.Ink, modifier = Modifier.size(36.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        TrimBar(
            windowStart = windowStart,
            total = totalMs,
            startMs = startMs,
            endMs = endMs,
            onStart = ::setStart,
            onEnd = ::setEnd,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Nudge("Start", formatClock(startMs), Modifier.weight(1f), onMinus = { setStart(startMs - 1000) }, onPlus = { setStart(startMs + 1000) })
            Nudge("End", formatClock(endMs), Modifier.weight(1f), onMinus = { setEnd(endMs - 1000) }, onPlus = { setEnd(endMs + 1000) })
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Clip length: ${(endMs - startMs) / 1000}s",
            color = Senses.Ink,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = Senses.Coral, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))

        if (phase == Phase.Editing) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton("Discard", onClick = { confirmDiscard = true }, style = PillStyle.Outlined, modifier = Modifier.weight(1f))
                PillButton("Save clip", onClick = ::save, modifier = Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxWidth().height(48.dp), verticalArrangement = Arrangement.Center) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Senses.Mint,
                    trackColor = Senses.Border,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (phase == Phase.Cutting) "Cutting your clip..." else "Saving to your gallery...",
                    color = Senses.Ink,
                    fontSize = 14.sp,
                )
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = Senses.Surface,
            title = { Text("Discard this recording?", color = Senses.Ink) },
            text = { Text("It will be deleted and cannot be brought back.", color = Senses.Ink) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; discard() }) { Text("Discard", color = Senses.Coral) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing", color = Senses.Mint) }
            },
        )
    }
}

@Composable
private fun Nudge(label: String, value: String, modifier: Modifier, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Senses.InkMute, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoundTextButton("-", onMinus, diameter = 40.dp)
            Text(value, color = Senses.Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            RoundTextButton("+", onPlus, diameter = 40.dp)
        }
    }
}

/** Two handles over the last stretch of the recording. Drag either one, or use the buttons below. */
@Composable
private fun TrimBar(
    windowStart: Long,
    total: Long,
    startMs: Long,
    endMs: Long,
    onStart: (Long) -> Unit,
    onEnd: (Long) -> Unit,
) {
    val windowLen = (total - windowStart).coerceAtLeast(1L)
    val onStartNow by rememberUpdatedState(onStart)
    val onEndNow by rememberUpdatedState(onEnd)
    val startNow by rememberUpdatedState(startMs)
    val endNow by rememberUpdatedState(endMs)
    val handle = 22.dp

    BoxWithConstraints(Modifier.fillMaxWidth().height(56.dp)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val handlePx = with(density) { handle.toPx() }
        val travel = (widthPx - handlePx).coerceAtLeast(1f)
        fun leftOf(ms: Long) = ((ms - windowStart).toFloat() / windowLen) * travel

        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(Senses.Surface, size = size, cornerRadius = CornerRadius(12.dp.toPx()))
            val l = leftOf(startMs)
            val r = leftOf(endMs) + handlePx
            drawRect(Senses.Mint.copy(alpha = 0.22f), topLeft = Offset(l, 0f), size = Size(r - l, size.height))
            drawRect(Senses.Mint, topLeft = Offset(l, 0f), size = Size(r - l, 2.dp.toPx()))
            drawRect(Senses.Mint, topLeft = Offset(l, size.height - 2.dp.toPx()), size = Size(r - l, 2.dp.toPx()))
        }
        Box(
            Modifier
                .offset { IntOffset(leftOf(startMs).roundToInt(), 0) }
                .width(handle)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Senses.Mint)
                .pointerInput(travel) {
                    detectHorizontalDragGestures { change, dx ->
                        change.consume()
                        onStartNow(startNow + (dx / travel * windowLen).toLong())
                    }
                },
        )
        Box(
            Modifier
                .offset { IntOffset(leftOf(endMs).roundToInt(), 0) }
                .width(handle)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Senses.Mint)
                .pointerInput(travel) {
                    detectHorizontalDragGestures { change, dx ->
                        change.consume()
                        onEndNow(endNow + (dx / travel * windowLen).toLong())
                    }
                },
        )
    }
}
