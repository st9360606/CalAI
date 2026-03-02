package com.calai.bitecal.data.foodlog.repo

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * 建立 FoodLog upload 用的 multipart parts。
 *
 * 原則：
 * 1. 若來源可能是 HEIC / HEIF / WebP / PNG / 任意相簿圖，請先走 ImageCompressUtil 轉成 JPEG bytes
 * 2. 不要把未知格式的原始檔直接假裝成 image/jpeg 上傳
 */
object MultipartParts {

    /**
     * 只接受「已經是 JPEG 的 bytes」。
     * 適合搭配：
     * - ImageCompressUtil.compressUriToJpegBytes(...)
     * - ImageCompressUtil.compressFileToJpegBytes(...)
     */
    fun jpegImagePart(
        fieldName: String = "file",
        filename: String,
        jpegBytes: ByteArray
    ): MultipartBody.Part {
        val reqBody = jpegBytes.toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData(fieldName, filename, reqBody)
    }

    /**
     * 只有在你 100% 確定 file 本身就是對應格式時才能用。
     * 例如：
     * - 真的已經是 .jpg 檔
     * - 真的已經是 .png 檔
     *
     * 不建議拿來直接傳相簿選到的未知格式檔案。
     */
    fun encodedImagePartFromFile(
        fieldName: String,
        filename: String,
        file: File,
        mediaType: String
    ): MultipartBody.Part {
        val bytes = file.readBytes()
        val reqBody = bytes.toRequestBody(mediaType.toMediaType())
        return MultipartBody.Part.createFormData(fieldName, filename, reqBody)
    }
}
