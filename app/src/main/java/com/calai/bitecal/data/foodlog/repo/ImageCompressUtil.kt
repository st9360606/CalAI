package com.calai.bitecal.data.foodlog.repo

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.core.graphics.scale

object ImageCompressUtil {

    /**
     * 將 Uri 圖片壓縮成 JPEG bytes
     * - 最長邊限制 maxSide
     * - JPEG 品質 quality
     */
    fun compressUriToJpegBytes(
        ctx: Context,
        uri: Uri,
        maxSide: Int = 1600,
        quality: Int = 82
    ): ByteArray {
        val resolver: ContentResolver = ctx.contentResolver

        // 第一次只讀尺寸
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }

        val sampleSize = calculateInSampleSize(
            srcWidth = bounds.outWidth,
            srcHeight = bounds.outHeight,
            reqMaxSide = maxSide
        )

        // 第二次真正 decode
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val bitmap = resolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, decodeOpts)
                ?: throw IllegalStateException("Failed to decode image from uri: $uri")
        }

        val scaled = scaleBitmapKeepRatio(bitmap, maxSide)
        if (scaled !== bitmap) {
            bitmap.recycle()
        }

        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            scaled.recycle()
            out.toByteArray()
        }
    }

    /**
     * 將 File 圖片壓縮成 JPEG bytes
     */
    fun compressFileToJpegBytes(
        file: File,
        maxSide: Int = 1600,
        quality: Int = 82
    ): ByteArray {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, bounds)

        val sampleSize = calculateInSampleSize(
            srcWidth = bounds.outWidth,
            srcHeight = bounds.outHeight,
            reqMaxSide = maxSide
        )

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
            ?: throw IllegalStateException("Failed to decode file: ${file.absolutePath}")

        val scaled = scaleBitmapKeepRatio(bitmap, maxSide)
        if (scaled !== bitmap) {
            bitmap.recycle()
        }

        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            scaled.recycle()
            out.toByteArray()
        }
    }

    private fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        reqMaxSide: Int
    ): Int {
        var inSampleSize = 1
        var width = srcWidth
        var height = srcHeight

        while (width > reqMaxSide * 2 || height > reqMaxSide * 2) {
            width /= 2
            height /= 2
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun scaleBitmapKeepRatio(bitmap: Bitmap, maxSide: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longest = maxOf(width, height)

        if (longest <= maxSide) return bitmap

        val ratio = maxSide.toFloat() / longest.toFloat()
        val newW = (width * ratio).toInt()
        val newH = (height * ratio).toInt()

        return bitmap.scale(newW, newH)
    }
}