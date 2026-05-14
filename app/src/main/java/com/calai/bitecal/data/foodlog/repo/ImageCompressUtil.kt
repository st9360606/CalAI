package com.calai.bitecal.data.foodlog.repo

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.io.File

object ImageCompressUtil {

    /**
     * 將 Uri 圖片壓縮成 JPEG bytes
     * - 最長邊限制 maxSide
     * - JPEG 品質 quality
     *
     * 用途：
     * - Photo Picker / Gallery / 外部分享進來的圖片 Uri
     * - 可把 HEIC / HEIF / PNG / WebP 等來源統一轉成 JPEG 上傳
     */
    fun compressUriToJpegBytes(
        ctx: Context,
        uri: Uri,
        maxSide: Int = 1600,
        quality: Int = 82
    ): ByteArray {
        val bitmap = decodeBitmapFromUri(ctx, uri)
        return bitmapToJpegBytes(bitmap, maxSide, quality)
    }

    /**
     * 將 File 圖片壓縮成 JPEG bytes
     * - 最長邊限制 maxSide
     * - JPEG 品質 quality
     *
     * 用途：
     * - CameraX 輸出的拍照檔
     * - 其他已落地到暫存 File 的圖片
     */
    fun compressFileToJpegBytes(
        file: File,
        maxSide: Int = 1600,
        quality: Int = 82
    ): ByteArray {
        val bitmap = decodeBitmapFromFile(file)
        return bitmapToJpegBytes(bitmap, maxSide, quality)
    }

    private fun bitmapToJpegBytes(
        bitmap: Bitmap,
        maxSide: Int,
        quality: Int
    ): ByteArray {
        val scaled = scaleBitmapKeepRatio(bitmap, maxSide)
        if (scaled !== bitmap) {
            bitmap.recycle()
        }

        return ByteArrayOutputStream().use { out ->
            val ok = scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            scaled.recycle()

            if (!ok) {
                throw IllegalStateException("Failed to encode bitmap to JPEG")
            }
            out.toByteArray()
        }
    }

    private fun decodeBitmapFromUri(
        ctx: Context,
        uri: Uri
    ): Bitmap {
        // API 28+：優先用 ImageDecoder，對 HEIC / HEIF 通常更穩
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val source = ImageDecoder.createSource(ctx.contentResolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            }
        }

        // fallback：舊版 / ImageDecoder 失敗時走 BitmapFactory
        return decodeBitmapFromUriLegacy(ctx.contentResolver, uri)
    }

    private fun decodeBitmapFromUriLegacy(
        resolver: ContentResolver,
        uri: Uri
    ): Bitmap {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Failed to open image input stream: $uri" }
            BitmapFactory.decodeStream(input, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("Failed to read image bounds from uri: $uri")
        }

        val sampleSize = calculateInSampleSize(
            srcWidth = bounds.outWidth,
            srcHeight = bounds.outHeight,
            reqMaxSide = 1600
        )

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Failed to open image input stream: $uri" }
            BitmapFactory.decodeStream(input, null, decodeOpts)
                ?: throw IllegalStateException("Failed to decode image from uri: $uri")
        }
    }

    private fun decodeBitmapFromFile(file: File): Bitmap {
        // API 28+：優先用 ImageDecoder，對 HEIC / HEIF 較穩
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val source = ImageDecoder.createSource(file)
                return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            }
        }

        // fallback：舊版 / ImageDecoder 失敗時走 BitmapFactory
        return decodeBitmapFromFileLegacy(file)
    }

    private fun decodeBitmapFromFileLegacy(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("Failed to read image bounds from file: ${file.absolutePath}")
        }

        val sampleSize = calculateInSampleSize(
            srcWidth = bounds.outWidth,
            srcHeight = bounds.outHeight,
            reqMaxSide = 1600
        )

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
            ?: throw IllegalStateException("Failed to decode file: ${file.absolutePath}")
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

    private fun scaleBitmapKeepRatio(
        bitmap: Bitmap,
        maxSide: Int
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longest = maxOf(width, height)

        if (longest <= maxSide) return bitmap

        val ratio = maxSide.toFloat() / longest.toFloat()
        val newW = (width * ratio).toInt().coerceAtLeast(1)
        val newH = (height * ratio).toInt().coerceAtLeast(1)

        return bitmap.scale(newW, newH)
    }
}
