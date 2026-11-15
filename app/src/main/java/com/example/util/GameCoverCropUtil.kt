package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Intelligent cropping utility for video game cover photos.
 * Supports:
 * 1. AI-assisted bounding box cropping (from normalized coordinates [ymin, xmin, ymax, xmax]).
 * 2. On-device smart background detection and edge cropping.
 * 3. Video game box ratio preservation (portrait DVD/case or square cartridge boxes).
 */
object GameCoverCropUtil {

    data class NormalizedBox(
        val ymin: Int,
        val xmin: Int,
        val ymax: Int,
        val xmax: Int
    ) {
        val isValid: Boolean
            get() = ymin in 0..1000 && xmin in 0..1000 &&
                    ymax in 0..1000 && xmax in 0..1000 &&
                    ymax > ymin && xmax > xmin
    }

    /**
     * Crops the bitmap according to normalized 0..1000 coordinates with boundary protection
     * and a slight safety margin to avoid cutting title typography.
     */
    fun cropFromNormalizedBox(
        bitmap: Bitmap,
        ymin: Int,
        xmin: Int,
        ymax: Int,
        xmax: Int,
        paddingPercent: Float = 0.015f
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 1 || height <= 1) return bitmap

        val y0 = (ymin.toFloat() / 1000f * height).toInt().coerceIn(0, height - 1)
        val x0 = (xmin.toFloat() / 1000f * width).toInt().coerceIn(0, width - 1)
        val y1 = (ymax.toFloat() / 1000f * height).toInt().coerceIn(y0 + 1, height)
        val x1 = (xmax.toFloat() / 1000f * width).toInt().coerceIn(x0 + 1, width)

        val boxWidth = x1 - x0
        val boxHeight = y1 - y0

        val padX = (boxWidth * paddingPercent).toInt()
        val padY = (boxHeight * paddingPercent).toInt()

        val cropLeft = (x0 - padX).coerceIn(0, width - 1)
        val cropTop = (y0 - padY).coerceIn(0, height - 1)
        val cropRight = (x1 + padX).coerceIn(cropLeft + 1, width)
        val cropBottom = (y1 + padY).coerceIn(cropTop + 1, height)

        val finalW = cropRight - cropLeft
        val finalH = cropBottom - cropTop

        return if (finalW > 10 && finalH > 10) {
            Bitmap.createBitmap(bitmap, cropLeft, cropTop, finalW, finalH)
        } else {
            bitmap
        }
    }

    /**
     * Smart on-device cropping:
     * - Scans borders to detect background (e.g. table, desk, plain surface) and trims it.
     * - Adjusts excessive horizontal/vertical whitespace to center around the video game box.
     */
    fun autoCropGameCover(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 20 || height <= 20) return bitmap

        val cornerPixels = intArrayOf(
            bitmap.getPixel(0, 0),
            bitmap.getPixel(width - 1, 0),
            bitmap.getPixel(0, height - 1),
            bitmap.getPixel(width - 1, height - 1)
        )

        val threshold = 35

        fun isBackground(pixel: Int): Boolean {
            return cornerPixels.any { corner ->
                val dr = abs(Color.red(pixel) - Color.red(corner))
                val dg = abs(Color.green(pixel) - Color.green(corner))
                val db = abs(Color.blue(pixel) - Color.blue(corner))
                (dr + dg + db) / 3 < threshold
            }
        }

        var top = 0
        var bottom = height - 1
        var left = 0
        var right = width - 1

        val scanStepX = max(1, width / 25)
        val scanStepY = max(1, height / 25)
        val rowBuffer = IntArray(width)
        val colBuffer = IntArray(height)

        // Scan from top
        topScan@ for (y in 0 until height / 3) {
            bitmap.getPixels(rowBuffer, 0, width, 0, y, width, 1)
            var nonBg = 0
            for (x in 0 until width step scanStepX) {
                if (!isBackground(rowBuffer[x])) nonBg++
            }
            if (nonBg >= 3) {
                top = max(0, y - 2)
                break@topScan
            }
        }

        // Scan from bottom
        bottomScan@ for (y in height - 1 downTo height * 2 / 3) {
            bitmap.getPixels(rowBuffer, 0, width, 0, y, width, 1)
            var nonBg = 0
            for (x in 0 until width step scanStepX) {
                if (!isBackground(rowBuffer[x])) nonBg++
            }
            if (nonBg >= 3) {
                bottom = min(height - 1, y + 2)
                break@bottomScan
            }
        }

        // Scan from left
        leftScan@ for (x in 0 until width / 3) {
            bitmap.getPixels(colBuffer, 0, 1, x, 0, 1, height)
            var nonBg = 0
            for (y in 0 until height step scanStepY) {
                if (!isBackground(colBuffer[y])) nonBg++
            }
            if (nonBg >= 3) {
                left = max(0, x - 2)
                break@leftScan
            }
        }

        // Scan from right
        rightScan@ for (x in width - 1 downTo width * 2 / 3) {
            bitmap.getPixels(colBuffer, 0, 1, x, 0, 1, height)
            var nonBg = 0
            for (y in 0 until height step scanStepY) {
                if (!isBackground(colBuffer[y])) nonBg++
            }
            if (nonBg >= 3) {
                right = min(width - 1, x + 2)
                break@rightScan
            }
        }

        val detectedWidth = right - left + 1
        val detectedHeight = bottom - top + 1

        // If a meaningful distinct foreground area is found, crop to it
        if (detectedWidth > width * 0.30f && detectedHeight > height * 0.30f &&
            (detectedWidth < width * 0.98f || detectedHeight < height * 0.98f)
        ) {
            return Bitmap.createBitmap(bitmap, left, top, detectedWidth, detectedHeight)
        }

        // Secondary framing: standard video game cover aspect ratio normalization
        val currentRatio = width.toFloat() / height.toFloat()
        if (currentRatio > 1.25f) {
            // Horizontal photo of a portrait video game box: center crop to ~0.75-0.80 ratio
            val targetWidth = (height * 0.78f).toInt().coerceAtMost(width)
            val startX = (width - targetWidth) / 2
            return Bitmap.createBitmap(bitmap, startX, 0, targetWidth, height)
        } else if (currentRatio < 0.55f) {
            // Extremely narrow photo: center crop to ~0.70 ratio
            val targetHeight = (width / 0.70f).toInt().coerceAtMost(height)
            val startY = (height - targetHeight) / 2
            return Bitmap.createBitmap(bitmap, 0, startY, width, targetHeight)
        }

        return bitmap
    }

    /**
     * Rotates a bitmap by a specified angle in degrees (e.g. 90, 180, 270, -90).
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val normalizedDegrees = ((degrees % 360f) + 360f) % 360f
        if (normalizedDegrees == 0f) return bitmap
        val matrix = android.graphics.Matrix().apply {
            postRotate(normalizedDegrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Flips a bitmap horizontally (mirror).
     */
    fun flipBitmapHorizontal(bitmap: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix().apply {
            postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Crops a bitmap based on normalized percentage margins (0f..1f):
     * leftPercent, topPercent, rightPercent, bottomPercent.
     */
    fun cropByPercent(
        bitmap: Bitmap,
        leftPercent: Float,
        topPercent: Float,
        rightPercent: Float,
        bottomPercent: Float
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 10 || height <= 10) return bitmap

        val x0 = (width * leftPercent.coerceIn(0f, 0.45f)).toInt().coerceIn(0, width - 2)
        val y0 = (height * topPercent.coerceIn(0f, 0.45f)).toInt().coerceIn(0, height - 2)
        val x1 = (width * (1f - rightPercent.coerceIn(0f, 0.45f))).toInt().coerceIn(x0 + 10, width)
        val y1 = (height * (1f - bottomPercent.coerceIn(0f, 0.45f))).toInt().coerceIn(y0 + 10, height)

        val targetW = x1 - x0
        val targetH = y1 - y0

        return if (targetW > 10 && targetH > 10) {
            Bitmap.createBitmap(bitmap, x0, y0, targetW, targetH)
        } else {
            bitmap
        }
    }

    /**
     * Crops a bitmap to conform to a specific aspect ratio (e.g. 0.75 for 3:4 portrait, 1.33 for 4:3 landscape, 1.0 for square).
     */
    fun cropToTargetRatio(bitmap: Bitmap, targetRatio: Float): Bitmap {
        if (targetRatio <= 0f || bitmap.width <= 1 || bitmap.height <= 1) return bitmap
        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        if (abs(currentRatio - targetRatio) < 0.02f) return bitmap

        return if (currentRatio > targetRatio) {
            // Bitmap is wider than target ratio: crop sides
            val targetWidth = (bitmap.height * targetRatio).toInt().coerceAtMost(bitmap.width)
            val startX = (bitmap.width - targetWidth) / 2
            Bitmap.createBitmap(bitmap, startX, 0, targetWidth, bitmap.height)
        } else {
            // Bitmap is taller than target ratio: crop top/bottom
            val targetHeight = (bitmap.width / targetRatio).toInt().coerceAtMost(bitmap.height)
            val startY = (bitmap.height - targetHeight) / 2
            Bitmap.createBitmap(bitmap, 0, startY, bitmap.width, targetHeight)
        }
    }
}
