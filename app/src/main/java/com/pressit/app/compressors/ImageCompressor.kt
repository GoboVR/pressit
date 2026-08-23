package com.pressit.app.compressors

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Compresses an image locally (no network calls) toward a target size by
 * progressively lowering JPEG quality and, if that isn't enough, downscaling
 * the bitmap dimensions. Runs entirely on-device via Android's built-in
 * BitmapFactory / Bitmap.compress APIs.
 */
object ImageCompressor {

    fun compress(input: InputStream, output: File, targetBytes: Long, orientation: Int): Boolean {
        val original = BitmapFactory.decodeStream(input) ?: return false
        var bitmap = applyExifRotation(original, orientation)

        var quality = 92
        var scale = 1.0f
        val minQuality = 10
        val minScale = 0.2f

        while (true) {
            val working = if (scale < 1.0f) {
                val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, w, h, true)
            } else bitmap

            FileOutputStream(output).use { fos ->
                working.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            }

            if (working !== bitmap) working.recycle()

            if (output.length() <= targetBytes || (quality <= minQuality && scale <= minScale)) {
                break
            }

            // Prefer dropping quality first; once it's low, start scaling down too.
            if (quality > minQuality) {
                quality -= 8
            } else {
                scale -= 0.15f
                if (scale < minScale) scale = minScale
            }
        }

        bitmap.recycle()
        return true
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
