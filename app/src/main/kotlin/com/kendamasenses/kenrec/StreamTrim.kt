package com.kendamasenses.kenrec

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.nio.ByteBuffer

/**
 * Cuts [startUs, endUs] out of a video by copying the already-encoded samples into a new
 * file: no decoding and no re-encoding, so it takes seconds however long the source is.
 *
 * The picture can only start on a keyframe, so the cut starts on the keyframe at or just
 * before [startUs] (up to about a second early). Picture and sound both start there, so
 * they stay in step: starting the sound at [startUs] and the picture on the earlier
 * keyframe leaves a clip whose sound lags, which other apps' re-encoders can choke on.
 */
object StreamTrim {
    fun trim(inPath: String, outPath: String, startUs: Long, endUs: Long) {
        val rotation = MediaMetadataRetriever().let { retriever ->
            try {
                retriever.setDataSource(inPath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            } finally {
                retriever.release()
            }
        }

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        try {
            extractor.setDataSource(inPath)
            muxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(rotation)

            val trackMap = HashMap<Int, Int>()
            var bufferSize = 1 shl 20
            var videoTrack = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    trackMap[i] = muxer.addTrack(format)
                    if (mime.startsWith("video/") && videoTrack < 0) videoTrack = i
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        bufferSize = maxOf(bufferSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                    }
                }
            }
            if (trackMap.isEmpty()) throw IllegalStateException("no audio or video track")

            // Find the keyframe on the video track alone, then start both tracks there.
            var cutStartUs = startUs
            if (videoTrack >= 0) {
                extractor.selectTrack(videoTrack)
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                if (extractor.sampleTime >= 0) cutStartUs = extractor.sampleTime
                extractor.unselectTrack(videoTrack)
            }
            for (i in trackMap.keys) extractor.selectTrack(i)

            muxer.start()
            muxerStarted = true
            extractor.seekTo(cutStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocate(bufferSize)
            val info = MediaCodec.BufferInfo()
            while (true) {
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val time = extractor.sampleTime
                if (time > endUs) break
                val track = trackMap[extractor.sampleTrackIndex]
                if (track != null) {
                    info.offset = 0
                    info.size = size
                    info.presentationTimeUs = maxOf(0L, time - cutStartUs)
                    info.flags = extractor.sampleFlags
                    muxer.writeSampleData(track, buffer, info)
                }
                extractor.advance()
            }
        } finally {
            try {
                if (muxerStarted) muxer?.stop()
            } catch (_: Exception) {
            }
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
            extractor.release()
        }
    }
}
