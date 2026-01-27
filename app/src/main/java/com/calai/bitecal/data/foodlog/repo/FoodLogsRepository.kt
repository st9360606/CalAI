package com.calai.bitecal.data.foodlog.repo

import com.calai.bitecal.data.foodlog.api.FoodLogsApi
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import kotlinx.coroutines.delay
import javax.inject.Inject

class FoodLogsRepository @Inject constructor(
    private val api: FoodLogsApi
) {
    suspend fun submitAlbumImage(part: okhttp3.MultipartBody.Part): FoodLogEnvelopeDto =
        api.postAlbum(part)

    suspend fun getOne(id: String): FoodLogEnvelopeDto = api.getOne(id)

    suspend fun retry(id: String): FoodLogEnvelopeDto = api.retry(id)

    /**
     * MVP 輪詢策略：
     * - 看到 PENDING：delay(pollAfterSec or 2s) 後再打
     * - 看到 DRAFT/FAILED/SAVED/DELETED：停止
     */
    suspend fun pollUntilTerminal(id: String, maxAttempts: Int = 60): FoodLogEnvelopeDto {
        var last: FoodLogEnvelopeDto = api.getOne(id)
        repeat(maxAttempts) {
            if (last.status != FoodLogStatus.PENDING) return last
            val sec = last.task?.pollAfterSec?.coerceIn(1, 30) ?: 2
            delay(sec * 1000L)
            last = api.getOne(id)
        }
        return last
    }
}
