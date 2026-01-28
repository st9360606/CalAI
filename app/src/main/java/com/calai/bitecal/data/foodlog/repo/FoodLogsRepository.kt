package com.calai.bitecal.data.foodlog.repo

import com.calai.bitecal.data.foodlog.api.BarcodeReq
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

    suspend fun submitLabelImage(part: okhttp3.MultipartBody.Part): FoodLogEnvelopeDto =
        api.postLabel(part)

    suspend fun submitBarcode(barcode: String): FoodLogEnvelopeDto =
        api.postBarcode(BarcodeReq(barcode))

    suspend fun getOne(id: String): FoodLogEnvelopeDto = api.getOne(id)
    suspend fun retry(id: String): FoodLogEnvelopeDto = api.retry(id)

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
