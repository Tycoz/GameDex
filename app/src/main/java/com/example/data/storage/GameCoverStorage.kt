package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
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
                .replace(Regex("[^a-z0-9]"), "_")
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
     * Loads a Bitmap from a local file:// or content:// URI.
     */
    fun loadBitmapFromCoverUrl(context: Context, coverUrl: String): Bitmap? {
        return try {
            if (coverUrl.startsWith("file://")) {
                val path = coverUrl.removePrefix("file://")
                android.graphics.BitmapFactory.decodeFile(path)
            } else if (coverUrl.startsWith("content://")) {
                context.contentResolver.openInputStream(coverUrl.toUri())?.use { input ->
                    android.graphics.BitmapFactory.decodeStream(input)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from coverUrl: $coverUrl", e)
            null
        }
    }
}
