package com.kendamasenses.kenrec

import android.content.Context

/** How much detail a recording gets. Bigger numbers mean bigger files. */
enum class VideoQuality(
    val title: String,
    val detail: String,
    /** The bit rate asked of the encoder, or null to let the phone choose. */
    val targetBitsPerSecond: Int?,
    /** What a recording at this setting weighs, roughly, for the size estimates. */
    val approxBitsPerSecond: Int,
    val maxHeight: Int,
) {
    Small("Small files", "720p", 6_000_000, 6_000_000, 720),
    Standard("Standard", "1080p", 10_000_000, 10_000_000, 1080),
    Best("Best", "1080p, the phone's own quality", null, 20_000_000, 1080),
}

/** Which camera the app opens on. Flipping it on the camera screen only lasts for that recording. */
enum class StartCamera(val title: String) {
    Front("Front"),
    Back("Back"),

    /** The back camera zoomed out to its wide lens, on a phone that has one. */
    BackWide("Wide"),
}

/** After this many seconds without a touch, the screen goes dark while recording. 0 means never. */
val DIM_CHOICES = listOf(0, 10, 30, 60)
const val DEFAULT_DIM_SECONDS = 30

fun dimLabel(seconds: Int) = when {
    seconds == 0 -> "Off"
    seconds < 60 -> "$seconds s"
    else -> "${seconds / 60} min"
}

/** A recording restarts after one of these many minutes. */
val RESTART_CHOICES = listOf(15, 30, 45, 60)
const val DEFAULT_RESTART_MINUTES = 45
private const val AUDIO_BITS_PER_SECOND = 128_000

/** Roughly how many gigabytes a recording of [seconds] takes at [quality]. */
fun approxGigabytes(quality: VideoQuality, seconds: Long): Double =
    (quality.approxBitsPerSecond + AUDIO_BITS_PER_SECOND) / 8.0 * seconds / 1_000_000_000.0

/** The person's choices, kept on the phone. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("ken_rec_settings", Context.MODE_PRIVATE)

    var restartMinutes: Int
        get() = prefs.getInt("restart_minutes", DEFAULT_RESTART_MINUTES).takeIf { it in RESTART_CHOICES } ?: DEFAULT_RESTART_MINUTES
        set(value) = prefs.edit().putInt("restart_minutes", value).apply()

    var startCamera: StartCamera
        get() = StartCamera.entries.firstOrNull { it.name == prefs.getString("start_camera", null) } ?: StartCamera.Front
        set(value) = prefs.edit().putString("start_camera", value.name).apply()

    var dimAfterSeconds: Int
        get() = prefs.getInt("dim_after_seconds", DEFAULT_DIM_SECONDS).takeIf { it in DIM_CHOICES } ?: DEFAULT_DIM_SECONDS
        set(value) = prefs.edit().putInt("dim_after_seconds", value).apply()

    var quality: VideoQuality
        get() = VideoQuality.entries.firstOrNull { it.name == prefs.getString("quality", null) } ?: VideoQuality.Standard
        set(value) = prefs.edit().putString("quality", value.name).apply()

    /** Test builds can force a short restart time (see CONTRIBUTING.md); otherwise the setting decides. */
    val restartAfterMs: Long
        get() = if (BuildConfig.MAX_RECORDING_MS_OVERRIDE > 0) BuildConfig.MAX_RECORDING_MS_OVERRIDE else restartMinutes * 60_000L
}
