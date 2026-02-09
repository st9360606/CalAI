package com.calai.bitecal.data.foodlog.repo

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * 把圖片轉成 MultipartBody.Part
 * - 純 Kotlin/OkHttp：方便寫 unit test
 */
object MultipartParts {

    fun imagePartFromFile(
        fieldName: String,
        filename: String,
        file: File,
        mediaType: String = "image/jpeg"
    ): MultipartBody.Part {
        val bytes = file.readBytes()
        val reqBody = bytes.toRequestBody(mediaType.toMediaType())
        return MultipartBody.Part.createFormData(fieldName, filename, reqBody)
    }
}
