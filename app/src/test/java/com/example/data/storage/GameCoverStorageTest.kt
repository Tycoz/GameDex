package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class GameCoverStorageTest {

    @Test
    fun saveCoverBitmap_createsValidFileAndReturnsUri() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(10, 10, Color.MAGENTA)

        val coverUri = GameCoverStorage.saveCoverBitmap(context, bitmap, "Super Mario 64")
        assertNotNull(coverUri)
        assertTrue("Should return a file:// URI", coverUri.startsWith("file://"))

        val filePath = coverUri.removePrefix("file://")
        val file = File(filePath)
        assertTrue("File should exist on disk", file.exists())
        assertTrue("File should not be empty", file.length() > 0)

        // Test deletion
        val deleted = GameCoverStorage.deleteCoverFile(coverUri)
        assertTrue("File should be successfully deleted", deleted)
        assertFalse("File should no longer exist", file.exists())
    }
}
