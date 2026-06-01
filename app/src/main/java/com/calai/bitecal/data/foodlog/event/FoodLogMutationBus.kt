package com.calai.bitecal.data.foodlog.event

import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface FoodLogMutationEvent {
    data class Upserted(
        val env: FoodLogEnvelopeDto,
        val previewUri: String? = null,
        val timeText: String? = null,
        val moveToTop: Boolean = false
    ) : FoodLogMutationEvent

    data class Deleted(
        val foodLogId: String,
        val capturedLocalDate: String? = null
    ) : FoodLogMutationEvent
}

@Singleton
class FoodLogMutationBus @Inject constructor() {
    private val _events = MutableSharedFlow<FoodLogMutationEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<FoodLogMutationEvent> = _events.asSharedFlow()

    fun publishUpserted(
        env: FoodLogEnvelopeDto,
        previewUri: String? = null,
        timeText: String? = null,
        moveToTop: Boolean = false
    ) {
        _events.tryEmit(
            FoodLogMutationEvent.Upserted(
                env = env,
                previewUri = previewUri,
                timeText = timeText,
                moveToTop = moveToTop
            )
        )
    }

    fun publishDeleted(
        foodLogId: String,
        capturedLocalDate: String? = null
    ) {
        _events.tryEmit(
            FoodLogMutationEvent.Deleted(
                foodLogId = foodLogId,
                capturedLocalDate = capturedLocalDate
            )
        )
    }
}
