package com.calai.bitecal.data.foodlog.repo

import com.calai.bitecal.data.foodlog.api.BarcodeReq
import com.calai.bitecal.data.foodlog.api.FoodLogsApi
import com.calai.bitecal.data.foodlog.model.CooldownActiveDto
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogServerErrorDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.data.foodlog.model.ModelRefusedDto
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.util.Locale
import javax.inject.Inject

class FoodLogsRepository @Inject constructor(
    private val api: FoodLogsApi
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    suspend fun submitAlbumImage(part: MultipartBody.Part): FoodLogEnvelopeDto =
        safeCall { api.postAlbum(part) }

    suspend fun submitLabelImage(
        part: MultipartBody.Part,
        deviceCapturedAtUtc: RequestBody? = null
    ): FoodLogEnvelopeDto =
        safeCall { api.postLabel(part, deviceCapturedAtUtc) }

    /**
     * ✅ 保持舊呼叫點不壞：
     * ViewModel 還是可以直接 repo.submitBarcode(barcode)
     */
    suspend fun submitBarcode(barcode: String): FoodLogEnvelopeDto =
        submitBarcode(
            barcode = barcode,
            locale = defaultLocaleTag()
        )

    /**
     * ✅ 若未來你有 App 內自選語言（不是 system locale），
     * 可以從上層明確傳 locale 進來。
     */
    suspend fun submitBarcode(
        barcode: String,
        locale: String?
    ): FoodLogEnvelopeDto =
        safeCall {
            api.postBarcode(
                BarcodeReq(
                    barcode = barcode,
                    locale = normalizeLocaleTag(locale)
                )
            )
        }

    suspend fun getOne(id: String): FoodLogEnvelopeDto =
        safeCall { api.getOne(id) }

    suspend fun retry(id: String): FoodLogEnvelopeDto =
        safeCall { api.retry(id) }

    suspend fun submitPhotoImage(
        part: MultipartBody.Part,
        deviceCapturedAtUtc: RequestBody? = null
    ): FoodLogEnvelopeDto =
        safeCall { api.postPhoto(part, deviceCapturedAtUtc) }

    suspend fun pollUntilTerminal(id: String, maxAttempts: Int = 60): FoodLogEnvelopeDto {
        var last: FoodLogEnvelopeDto = getOne(id)

        repeat(maxAttempts) {
            if (last.status != FoodLogStatus.PENDING) return last

            val sec = last.task?.pollAfterSec?.coerceIn(1, 30) ?: 2
            delay(sec * 1000L)

            last = try {
                getOne(id)
            } catch (e: FoodLogApiException.CooldownActive) {
                // ✅ 針對輪詢：不要把 cooldown 當錯誤，照建議秒數退避後繼續
                val backoff = (e.dto.cooldownSeconds ?: 10L).coerceIn(1L, 60L)
                delay(backoff * 1000L)
                getOne(id)
            }
        }
        return last
    }

    private suspend fun <T> safeCall(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: HttpException) {
            val code = e.code()
            val body = e.response()?.errorBody()?.string().orEmpty()

            if (code == 429 && body.isNotBlank()) {
                val dto = runCatching {
                    json.decodeFromString(CooldownActiveDto.serializer(), body)
                }.getOrNull()

                if (dto?.errorCode == "COOLDOWN_ACTIVE") {
                    throw FoodLogApiException.CooldownActive(dto)
                }
            }

            if (code == 422 && body.isNotBlank()) {
                val dto = runCatching {
                    json.decodeFromString(ModelRefusedDto.serializer(), body)
                }.getOrNull()

                if (dto?.errorCode == "MODEL_REFUSED") {
                    throw FoodLogApiException.ModelRefused(dto)
                }
            }

            if (body.isNotBlank()) {
                val dto = runCatching {
                    json.decodeFromString(FoodLogServerErrorDto.serializer(), body)
                }.getOrNull()

                if (dto?.normalizedCode() != null) {
                    throw FoodLogApiException.BusinessError(dto)
                }
            }

            // ✅ fallback：如果代理層直接回 413 / 非 JSON，也盡量轉成可顯示錯誤
            if (code == 413) {
                throw FoodLogApiException.BusinessError(
                    FoodLogServerErrorDto(
                        errorCode = "IMAGE_TOO_LARGE",
                        code = null,
                        message = "Image exceeds upload size limit",
                        requestId = null,
                        clientAction = "RETAKE_PHOTO",
                        retryAfterSec = null
                    )
                )
            }
            throw e
        }
    }

    /**
     * 預設用 system locale。
     * 例：
     * - zh-TW
     * - en-US
     * - pt-BR
     */
    private fun defaultLocaleTag(): String? {
        val tag = runCatching { Locale.getDefault().toLanguageTag() }.getOrNull()
        return normalizeLocaleTag(tag)
    }

    /**
     * 簡單正規化：
     * - null / blank -> null
     * - Kotlin/Java locale tag 原樣保留
     */
    private fun normalizeLocaleTag(raw: String?): String? {
        val s = raw?.trim()
        return if (s.isNullOrBlank()) null else s
    }
}
