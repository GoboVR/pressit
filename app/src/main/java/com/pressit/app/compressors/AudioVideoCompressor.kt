package com.pressit.app.compressors

import android.media.MediaMetadataRetriever
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Transcodes audio/video locally to hit a target output size, using the
 * on-device FFmpegKit library (all processing happens on the phone; nothing
 * is uploaded anywhere). Works by computing a target bitrate from the target
 * size and clip duration, then running up to a few refinement passes if the
 * first attempt overshoots.
 */
object AudioVideoCompressor {

    interface ProgressListener {
        fun onProgress(percent: Int)
    }

    fun compressVideo(
        inputPath: String,
        outputFile: File,
        targetBytes: Long,
        listener: ProgressListener? = null
    ): Boolean {
        val durationSec = durationSeconds(inputPath)
        if (durationSec <= 0) return false

        // Reserve ~10% of the size budget for container overhead + audio.
        val audioBitrate = 96_000L // bits/sec, fixed AAC bitrate
        var videoBitrate = ((targetBytes * 8 * 0.92) / durationSec).toLong() - audioBitrate
        videoBitrate = max(videoBitrate, 100_000L) // floor so we don't request nonsense

        var attempt = 0
        var success = false
        while (attempt < 3) {
            val cmd = "-y -i \"$inputPath\" -c:v libx264 -b:v ${videoBitrate} " +
                "-preset veryfast -c:a aac -b:a ${audioBitrate} \"${outputFile.absolutePath}\""
            val session = FFmpegKit.execute(cmd)
            listener?.onProgress(40 + attempt * 20)

            if (!ReturnCode.isSuccess(session.returnCode)) return false

            if (outputFile.length() <= targetBytes || attempt == 2) {
                success = true
                break
            }
            // Overshot: scale bitrate down proportionally and retry.
            val ratio = targetBytes.toDouble() / outputFile.length().toDouble()
            videoBitrate = (videoBitrate * ratio * 0.9).roundToLong().coerceAtLeast(80_000L)
            attempt++
        }
        return success
    }

    fun compressAudio(
        inputPath: String,
        outputFile: File,
        targetBytes: Long,
        listener: ProgressListener? = null
    ): Boolean {
        val durationSec = durationSeconds(inputPath)
        if (durationSec <= 0) return false

        var bitrate = ((targetBytes * 8 * 0.95) / durationSec).toLong()
        bitrate = bitrate.coerceIn(32_000L, 320_000L)

        var attempt = 0
        var success = false
        while (attempt < 3) {
            val cmd = "-y -i \"$inputPath\" -c:a aac -b:a ${bitrate} \"${outputFile.absolutePath}\""
            val session = FFmpegKit.execute(cmd)
            listener?.onProgress(40 + attempt * 20)

            if (!ReturnCode.isSuccess(session.returnCode)) return false

            if (outputFile.length() <= targetBytes || attempt == 2) {
                success = true
                break
            }
            val ratio = targetBytes.toDouble() / outputFile.length().toDouble()
            bitrate = (bitrate * ratio * 0.9).roundToLong().coerceAtLeast(24_000L)
            attempt++
        }
        return success
    }

    private fun durationSeconds(path: String): Double {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            ms / 1000.0
        } catch (e: Exception) {
            0.0
        } finally {
            retriever.release()
        }
    }
}
