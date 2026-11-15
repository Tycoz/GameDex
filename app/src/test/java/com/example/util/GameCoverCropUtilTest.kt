package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameCoverCropUtilTest {

    @Test
    fun testNormalizedBox_validation() {
        val validBox = GameCoverCropUtil.NormalizedBox(100, 200, 800, 900)
        assertTrue(validBox.isValid)

        val invalidBoxInverted = GameCoverCropUtil.NormalizedBox(800, 200, 100, 900)
        assertTrue(!invalidBoxInverted.isValid)

        val outOfBounds = GameCoverCropUtil.NormalizedBox(-5, 0, 1200, 500)
        assertTrue(!outOfBounds.isValid)
    }

    @Test
    fun testCropFromNormalizedBox_cropsAccurately() {
        val original = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        // Draw something inside
        original.setPixel(500, 500, Color.RED)

        // Request 20% to 80% box
        val cropped = GameCoverCropUtil.cropFromNormalizedBox(
            bitmap = original,
            ymin = 200,
            xmin = 200,
            ymax = 800,
            xmax = 800,
            paddingPercent = 0.0f
        )

        assertNotNull(cropped)
        assertTrue("Cropped width should be around 600", cropped.width in 590..610)
        assertTrue("Cropped height should be around 600", cropped.height in 590..610)
    }

    @Test
    fun testAutoCropGameCover_trimsUniformBorders() {
        // Create an image with a white border and colored center box
        val width = 400
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Fill background with white
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, Color.WHITE)
            }
        }

        // Fill center box with red (simulating game cover on a white desk)
        for (y in 80 until 520) {
            for (x in 60 until 340) {
                bitmap.setPixel(x, y, Color.RED)
            }
        }

        val autoCropped = GameCoverCropUtil.autoCropGameCover(bitmap)
        assertNotNull(autoCropped)
        assertTrue("Should have cropped out top/bottom margins", autoCropped.height < height)
        assertTrue("Should have cropped out left/right margins", autoCropped.width < width)
        assertTrue("Foreground width should be roughly centered around game box", autoCropped.width in 250..350)
    }

    @Test
    fun testAutoCropGameCover_wideLandscapePhoto_centersPortraitGameBox() {
        // Horizontal landscape photo from phone (800x400)
        val landscapeBitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
        for (y in 0 until 400) {
            for (x in 0 until 800) {
                bitmapAt(landscapeBitmap, x, y, Color.BLUE)
            }
        }

        val cropped = GameCoverCropUtil.autoCropGameCover(landscapeBitmap)
        assertNotNull(cropped)
        assertTrue("Cropped width should be normalized closer to portrait box", cropped.width <= 400)
    }

    @Test
    fun testRotateBitmap_90degrees_swapsDimensions() {
        val original = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        val rotated90 = GameCoverCropUtil.rotateBitmap(original, 90f)
        assertNotNull(rotated90)
        assertEquals(300, rotated90.width)
        assertEquals(200, rotated90.height)

        val rotated180 = GameCoverCropUtil.rotateBitmap(original, 180f)
        assertNotNull(rotated180)
        assertEquals(200, rotated180.width)
        assertEquals(300, rotated180.height)
    }

    @Test
    fun testFlipBitmapHorizontal_preservesDimensions() {
        val original = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        val flipped = GameCoverCropUtil.flipBitmapHorizontal(original)
        assertNotNull(flipped)
        assertEquals(200, flipped.width)
        assertEquals(300, flipped.height)
    }

    @Test
    fun testCropByPercent_reducesDimensionsCorrectly() {
        val original = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val cropped = GameCoverCropUtil.cropByPercent(
            bitmap = original,
            leftPercent = 0.10f,
            topPercent = 0.10f,
            rightPercent = 0.10f,
            bottomPercent = 0.10f
        )
        assertNotNull(cropped)
        assertEquals(160, cropped.width)
        assertEquals(160, cropped.height)
    }

    @Test
    fun testCropToTargetRatio_cropsToTarget() {
        val square = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        // Target ratio 3:4 = 0.75
        val portrait = GameCoverCropUtil.cropToTargetRatio(square, 0.75f)
        assertNotNull(portrait)
        assertEquals(300, portrait.width)
        assertEquals(400, portrait.height)

        // Target ratio 1:1 on wide image (400x200)
        val wide = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val squareFromWide = GameCoverCropUtil.cropToTargetRatio(wide, 1.0f)
        assertNotNull(squareFromWide)
        assertEquals(200, squareFromWide.width)
        assertEquals(200, squareFromWide.height)
    }

    private fun bitmapAt(b: Bitmap, x: Int, y: Int, color: Int) {
        b.setPixel(x, y, color)
    }
}
