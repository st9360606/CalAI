package com.calai.bitecal.data.foodlog.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

/**
 * 放在 src/test 的 Robolectric 測試。
 *
 * 注意：
 * - 你的 app minSdk = 30，所以 Robolectric 不能用 sdk=27
 * - 這裡固定用 sdk=30，符合 app manifest 要求
 * - 主要驗證：
 *   1. 壓縮後有輸出 JPEG bytes
 *   2. resize 行為正確
 *   3. 小圖不放大
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ImageCompressUtilRobolectricTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun compressFileToJpegBytes_should_return_jpeg_bytes_and_resize_if_needed() {
        val srcFile = createBitmapFile(
            ctx = context,
            fileName = "source_large.png",
            width = 3000,
            height = 2000,
            format = Bitmap.CompressFormat.PNG
        )

        try {
            val outBytes = ImageCompressUtil.compressFileToJpegBytes(
                file = srcFile,
                maxSide = 1600,
                quality = 82
            )

            assertTrue(outBytes.isNotEmpty())

            // JPEG magic number: FF D8 FF
            assertTrue(outBytes.size >= 3)
            assertTrue(outBytes[0] == 0xFF.toByte())
            assertTrue(outBytes[1] == 0xD8.toByte())
            assertTrue(outBytes[2] == 0xFF.toByte())

            val outBitmap = BitmapFactory.decodeByteArray(outBytes, 0, outBytes.size)
            assertNotNull(outBitmap)

            val longest = maxOf(outBitmap!!.width, outBitmap.height)
            assertTrue("longest side should be <= 1600, actual=$longest", longest <= 1600)

            outBitmap.recycle()
        } finally {
            srcFile.delete()
        }
    }

    @Test
    fun compressUriToJpegBytes_should_return_jpeg_bytes_and_resize_if_needed() {
        val srcFile = createBitmapFile(
            ctx = context,
            fileName = "source_uri_large.png",
            width = 2400,
            height = 1800,
            format = Bitmap.CompressFormat.PNG
        )

        try {
            val uri = Uri.fromFile(srcFile)

            val outBytes = ImageCompressUtil.compressUriToJpegBytes(
                ctx = context,
                uri = uri,
                maxSide = 1600,
                quality = 82
            )

            assertTrue(outBytes.isNotEmpty())

            // JPEG magic number: FF D8 FF
            assertTrue(outBytes.size >= 3)
            assertTrue(outBytes[0] == 0xFF.toByte())
            assertTrue(outBytes[1] == 0xD8.toByte())
            assertTrue(outBytes[2] == 0xFF.toByte())

            val outBitmap = BitmapFactory.decodeByteArray(outBytes, 0, outBytes.size)
            assertNotNull(outBitmap)

            val longest = maxOf(outBitmap!!.width, outBitmap.height)
            assertTrue("longest side should be <= 1600, actual=$longest", longest <= 1600)

            outBitmap.recycle()
        } finally {
            srcFile.delete()
        }
    }

    @Test
    fun compressFileToJpegBytes_should_keep_small_image_without_upscaling() {
        val srcFile = createBitmapFile(
            ctx = context,
            fileName = "source_small.jpg",
            width = 800,
            height = 600,
            format = Bitmap.CompressFormat.JPEG
        )

        try {
            val outBytes = ImageCompressUtil.compressFileToJpegBytes(
                file = srcFile,
                maxSide = 1600,
                quality = 82
            )

            assertTrue(outBytes.isNotEmpty())

            val outBitmap = BitmapFactory.decodeByteArray(outBytes, 0, outBytes.size)
            assertNotNull(outBitmap)

            // 小圖不應被放大
            assertTrue(outBitmap!!.width <= 800)
            assertTrue(outBitmap.height <= 600)

            outBitmap.recycle()
        } finally {
            srcFile.delete()
        }
    }

    private fun createBitmapFile(
        ctx: Context,
        fileName: String,
        width: Int,
        height: Int,
        format: Bitmap.CompressFormat
    ): File {
        val file = File(ctx.cacheDir, fileName)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xFF66CC99.toInt())
        }

        FileOutputStream(file).use { out ->
            val ok = bitmap.compress(format, 95, out)
            if (!ok) {
                bitmap.recycle()
                throw IllegalStateException("Failed to write bitmap file: $fileName")
            }
        }

        bitmap.recycle()
        return file
    }
}
