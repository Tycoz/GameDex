package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Manages persistent on-device storage for cropped video game cover images.
 * Files are kept in internal app storage (no runtime storage permission required)
 * and returned as local file URIs ("file:///data/user/0/.../game_covers/cover_xxx.jpg")
 * directly readable by Coil's AsyncImage and suitable for Firestore persistence.
 */
object GameCoverStorage {

    private const val TAG = "GameCoverStorage"
    private const val COVERS_DIR = "game_covers"
    private const val MAX_DECODE_SIDE = 1600
    private val SANITIZE_TITLE_REGEX = Regex("[^a-z0-9]")

    /**
     * Saves a cropped cover Bitmap to private internal storage.
     * @return Local file URI formatted as "file:///..." or empty string on failure.
     */
    fun saveCoverBitmap(context: Context, bitmap: Bitmap, title: String = ""): String {
        return try {
            val coversDir = File(context.filesDir, COVERS_DIR)
            if (!coversDir.exists()) {
                coversDir.mkdirs()
            }

            val sanitizedTitle = title.trim()
                .lowercase()
                .replace(SANITIZE_TITLE_REGEX, "_")
                .take(16)
                .ifBlank { "game" }

            val fileName = "cover_${sanitizedTitle}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}.jpg"
            val file = File(coversDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            Log.d(TAG, "Saved cropped game cover to ${file.absolutePath} (${file.length()} bytes)")
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cropped cover bitmap", e)
            ""
        }
    }

    /**
     * Deletes a local cover file if no longer needed.
     */
    fun deleteCoverFile(coverUrl: String): Boolean {
        return try {
            if (coverUrl.startsWith("file://")) {
                val path = coverUrl.removePrefix("file://")
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                } else false
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete cover file: $coverUrl", e)
            false
        }
    }

    /**
     * Loads a memory-bounded Bitmap (max 1600px on longest side) from a local file:// or content:// URI.
     */
    fun loadBitmapFromCoverUrl(context: Context, coverUrl: String): Bitmap? {
        return try {
            when {
                coverUrl.startsWith("file://") -> {
                    val path = coverUrl.removePrefix("file://")
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(path, bounds)
                    val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DECODE_SIDE)
                    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    BitmapFactory.decodeFile(path, opts)
                }
                coverUrl.startsWith("content://") -> {
                    val uri = coverUrl.toUri()
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, bounds)
                    }
                    val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DECODE_SIDE)
                    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, opts)
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from coverUrl: $coverUrl", e)
            null
        }
    }

    private fun computeInSampleSize(width: Int, height: Int, maxSide: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sampleSize = 1
        while (width / sampleSize > maxSide || height / sampleSize > maxSide) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
