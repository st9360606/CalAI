package com.calai.bitecal.data.foodlog.repo

import com.calai.bitecal.data.foodlog.api.BarcodeReq
import com.calai.bitecal.data.foodlog.api.FoodLogsApi
import com.calai.bitecal.data.foodlog.model.CooldownActiveDto
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogListResponseDto
import com.calai.bitecal.data.foodlog.model.FoodLogOverrideRequestDto
import com.calai.bitecal.data.foodlog.model.FoodLogServerErrorDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.data.foodlog.model.ModelRefusedDto
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.util.Locale
import javax.inject.Inject

sealed interface HomeCardPollResult {
    data class Terminal(val env: FoodLogEnvelopeDto) : HomeCardPollResult
    data class StillPending(val last: FoodLogEnvelopeDto) : HomeCardPollResult
}

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

    suspend fun submitPhotoImage(
        part: MultipartBody.Part,
        deviceCapturedAtUtc: RequestBody? = null
    ): FoodLogEnvelopeDto =
        safeCall { api.postPhoto(part, deviceCapturedAtUtc) }

    suspend fun submitBarcode(barcode: String): FoodLogEnvelopeDto =
        submitBarcode(
            barcode = barcode,
            locale = defaultLocaleTag()
        )

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

    suspend fun save(id: String): FoodLogEnvelopeDto =
        safeCall { api.save(id) }

    suspend fun delete(id: String): FoodLogEnvelopeDto =
        safeCall { api.delete(id) }

    suspend fun listSaved(
        fromLocalDate: String,
        toLocalDate: String,
        page: Int = 0,
        size: Int = 20
    ): FoodLogListResponseDto =
        safeCall {
            api.listSaved(
                fromLocalDate = fromLocalDate,
                toLocalDate = toLocalDate,
                page = page,
                size = size
            )
        }

    suspend fun listHistory(
        status: String,
        fromLocalDate: String,
        toLocalDate: String,
        page: Int = 0,
        size: Int = 20
    ): FoodLogListResponseDto =
        safeCall {
            api.listHistory(
                status = status.trim().uppercase(Locale.ROOT),
                fromLocalDate = fromLocalDate,
                toLocalDate = toLocalDate,
                page = page,
                size = size
            )
        }

    suspend fun applyOverride(
        id: String,
        fieldKey: String,
        newValue: JsonElement,
        reason: String? = null
    ): FoodLogEnvelopeDto =
        safeCall {
            api.applyOverride(
                id = id,
                req = FoodLogOverrideRequestDto(
                    fieldKey = fieldKey,
                    newValue = newValue,
                    reason = reason
                )
            )
        }

    suspend fun downloadImageBytes(id: String): ByteArray =
        safeCall { api.getImage(id).bytes() }

    /**
     * 給 detail / blocking flow 用。
     * 可能在 maxAttempts 耗盡後仍回 PENDING，因此不建議首頁卡片直接使用。
     */
    suspend fun pollUntilTerminal(id: String, maxAttempts: Int = 60): FoodLogEnvelopeDto {
        var last: FoodLogEnvelopeDto = getOne(id)

        repeat(maxAttempts) {
            if (last.status != FoodLogStatus.PENDING) return last

            val sec = last.task?.pollAfterSec?.coerceIn(1, 30) ?: 2
            delay(sec * 1000L)

            last = try {
                getOne(id)
            } catch (e: FoodLogApiException.CooldownActive) {
                val backoff = (e.dto.cooldownSeconds ?: 10L).coerceIn(1L, 60L)
                delay(backoff * 1000L)
                getOne(id)
            }
        }
        return last
    }

    /**
     * 給 Home recent-upload 卡片用：
     * - 只在短時間內做 hot polling
     * - 若超過時間預算仍是 PENDING，交給 UI 顯示 Delayed 狀態
     */
    suspend fun pollForHomeCard(
        id: String,
        hotWindowMs: Long = 15_000L,
        maxAttempts: Int = 8
    ): HomeCardPollResult {
        val startedAt = System.currentTimeMillis()
        var last: FoodLogEnvelopeDto = getOne(id)

        repeat(maxAttempts) {
            if (last.status != FoodLogStatus.PENDING) {
                return HomeCardPollResult.Terminal(last)
            }

            val sec = last.task?.pollAfterSec?.coerceIn(1, 10) ?: 2
            val nextDelayMs = sec * 1000L
            val elapsed = System.currentTimeMillis() - startedAt

            if (elapsed + nextDelayMs > hotWindowMs) {
                return HomeCardPollResult.StillPending(last)
            }

            delay(nextDelayMs)

            last = try {
                getOne(id)
            } catch (e: FoodLogApiException.CooldownActive) {
                val backoffSec = (e.dto.cooldownSeconds ?: 10L).coerceIn(1L, 20L)
                val backoffMs = backoffSec * 1000L
                val nowElapsed = System.currentTimeMillis() - startedAt

                if (nowElapsed + backoffMs > hotWindowMs) {
                    return HomeCardPollResult.StillPending(last)
                }

                delay(backoffMs)
                getOne(id)
            }
        }

        return if (last.status == FoodLogStatus.PENDING) {
            HomeCardPollResult.StillPending(last)
        } else {
            HomeCardPollResult.Terminal(last)
        }
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

            if (code == 413) {
                throw FoodLogApiException.BusinessError(
                    FoodLogServerErrorDto(
                        errorCode = "IMAGE_TOO_LARGE",
                        message = "Image exceeds upload size limit",
                        clientAction = "RETAKE_PHOTO"
                    )
                )
            }
            throw e
        }
    }

    private fun defaultLocaleTag(): String? {
        val tag = runCatching { Locale.getDefault().toLanguageTag() }.getOrNull()
        return normalizeLocaleTag(tag)
    }

    private fun normalizeLocaleTag(raw: String?): String? {
        val s = raw?.trim()
        return if (s.isNullOrBlank()) null else s
    }
}
