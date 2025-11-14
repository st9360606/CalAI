package com.calai.app.data.weight.repo

import com.calai.app.data.weight.api.WeightApi
import com.calai.app.data.weight.api.WeightItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val api: WeightApi
) {
    suspend fun log(weightKg: Double, logDate: String?, photoFile: File?): WeightItemDto =
        withContext(Dispatchers.IO) {
            val w = weightKg.toString().toRequestBody(MultipartBody.FORM)
            val d = logDate?.toRequestBody(MultipartBody.FORM)
            val part = photoFile?.let {
                MultipartBody.Part.createFormData(
                    name = "photo",
                    filename = it.name,
                    body = it.asRequestBody()
                )
            }
            api.logWeight(w, d, part)
        }

    suspend fun recent7() = withContext(Dispatchers.IO) { api.recent7() }
    suspend fun summary(range: String) = withContext(Dispatchers.IO) { api.summary(range) }

    fun kgToLbsInt(kg: Double): Int = (kg * 2.20462262).roundToInt()
}
