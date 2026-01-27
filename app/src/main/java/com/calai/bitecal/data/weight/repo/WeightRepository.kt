package com.calai.bitecal.data.weight.repo

import android.util.Log
import com.calai.bitecal.data.common.RepoInvalidationBus
import com.calai.bitecal.data.weight.api.WeightApi
import com.calai.bitecal.data.weight.api.WeightItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WeightRepository @Inject constructor(
    private val api: WeightApi,
    private val bus: RepoInvalidationBus
) {

    private fun guessImageMime(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            else -> "application/octet-stream"
        }
    }

    suspend fun log(
        weightKg: Double,
        weightLbs: Double,
        logDate: String?,
        photoFile: File?
    ): WeightItemDto = withContext(Dispatchers.IO) {
        val wKg: RequestBody  = weightKg.toString().toRequestBody(MultipartBody.FORM)
        val wLbs: RequestBody = weightLbs.toString().toRequestBody(MultipartBody.FORM)
        val d: RequestBody?   = logDate?.toRequestBody(MultipartBody.FORM)

        val part = photoFile?.let { f ->
            val mime = guessImageMime(f).toMediaType()
            Log.d("WeightRepo", "upload photo name=${f.name} mime=$mime size=${f.length()}")

            MultipartBody.Part.createFormData(
                name = "photo",
                filename = f.name,
                body = f.asRequestBody(mime)
            )
        }

        val resp = api.logWeight(wKg, wLbs, d, part)
        bus.invalidateWeight()// ✅ 寫入成功 -> invalidate（失敗會 throw，不會走到這行）
        resp
    }

    suspend fun recent7() = withContext(Dispatchers.IO) { api.recent7() }
    suspend fun summary(range: String) = withContext(Dispatchers.IO) { api.summary(range) }

    suspend fun ensureBaseline() {
        Log.d("WeightRepo", "ensureBaseline() called")
        runCatching { api.ensureBaseline() }
            .onSuccess {
                // ✅ baseline 成功 -> invalidate（讓 current/timeseries 重新抓）
                bus.invalidateWeight()
            }
            .onFailure { e ->
                Log.e("WeightRepo", "ensureBaseline failed", e)
            }
    }

    fun kgToLbsInt(kg: Double): Int = (kg * 2.20462262).roundToInt()
}
