package com.calai.bitecal.ui.home.ui.savedfood.model

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.core.time.UtcTimeFormatter
import com.calai.bitecal.data.foodlog.event.FoodLogMutationBus
import com.calai.bitecal.data.foodlog.event.FoodLogMutationEvent
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.data.foodlog.repo.FoodLogsRepository
import com.calai.bitecal.ui.home.ui.foodlog.FoodLogTimeResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

data class SavedFoodsUiState(
    val loading: Boolean = false,
    val items: List<SavedFoodCardUi> = emptyList(),
    val error: String? = null
)

data class SavedFoodCardUi(
    val foodLogId: String,
    val displayTitle: String,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val timeText: String,
    val savedAtUtc: String?,
    val previewUri: String?
)

@HiltViewModel
class SavedFoodsViewModel @Inject constructor(
    private val repo: FoodLogsRepository,
    private val zoneId: ZoneId,
    private val foodLogMutationBus: FoodLogMutationBus,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _ui = MutableStateFlow(SavedFoodsUiState(loading = true))
    val ui: StateFlow<SavedFoodsUiState> = _ui.asStateFlow()

    private var loaded = false

    private companion object {
        const val TAG = "SavedFoodsVm"
        const val LOOK_BACK_DAYS = 15
        const val PAGE_SIZE = 100
    }

    init {
        observeFoodLogMutations()
    }

    private fun observeFoodLogMutations() {
        viewModelScope.launch {
            foodLogMutationBus.events.collect { event ->
                when (event) {
                    is FoodLogMutationEvent.Upserted -> {
                        applyFoodLogMutation(
                            env = event.env,
                            previewUri = event.previewUri
                        )
                    }

                    is FoodLogMutationEvent.Deleted -> {
                        applyFoodLogDeleted(event.foodLogId)
                    }
                }
            }
        }
    }

    fun loadIfNeeded() {
        if (loaded || _ui.value.items.isNotEmpty()) return
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }

            runCatching {
                prunePreviewCache()
                fetchSavedFoods()
            }.onSuccess { items ->
                loaded = true
                _ui.value = SavedFoodsUiState(
                    loading = false,
                    items = items,
                    error = null
                )
            }.onFailure { t ->
                Log.w(TAG, "refresh failed: ${t.javaClass.simpleName}: ${t.message}", t)
                _ui.value = SavedFoodsUiState(
                    loading = false,
                    items = emptyList(),
                    error = t.message ?: "load saved foods failed"
                )
            }
        }
    }

    fun unsave(
        foodLogId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            val previewUri = _ui.value.items
                .firstOrNull { it.foodLogId == foodLogId }
                ?.previewUri

            runCatching {
                repo.unsave(foodLogId)
            }.onSuccess { env ->
                foodLogMutationBus.publishUpserted(
                    env = env,
                    previewUri = previewUri
                )
                onSuccess()
            }.onFailure { t ->
                Log.w(TAG, "unsave failed id=$foodLogId: ${t.javaClass.simpleName}: ${t.message}", t)
                _ui.update { it.copy(error = t.message ?: "unsave failed") }
                onFailure(t)
            }
        }
    }

    private suspend fun fetchSavedFoods(): List<SavedFoodCardUi> {
        val response = repo.listSavedRecent(
            lookBackDays = LOOK_BACK_DAYS,
            size = PAGE_SIZE
        )

        return buildList {
            for (item in response.items) {
                if (item.status != FoodLogStatus.SAVED) continue

                val previewUri = cacheSavedPreview(item.foodLogId)

                add(
                    SavedFoodCardUi(
                        foodLogId = item.foodLogId,
                        displayTitle = item.nutrition?.foodName
                            ?.trim()
                            .takeUnless { it.isNullOrBlank() }
                            ?: "Food",
                        kcal = (item.nutrition?.kcal ?: 0.0).roundToInt(),
                        proteinG = (item.nutrition?.protein ?: 0.0).roundToInt(),
                        carbsG = (item.nutrition?.carbs ?: 0.0).roundToInt(),
                        fatG = (item.nutrition?.fat ?: 0.0).roundToInt(),
                        timeText = FoodLogTimeResolver.resolveDisplayTimeText(
                            zoneId = zoneId,
                            createdAtUtc = item.createdAtUtc,
                            serverReceivedAtUtc = item.serverReceivedAtUtc,
                            capturedAtUtc = item.capturedAtUtc,
                            capturedLocalDate = item.capturedLocalDate
                        ),
                        savedAtUtc = item.savedAtUtc,
                        previewUri = previewUri
                    )
                )
            }
        }.sortBySavedAtDescending()
    }

    private fun applyFoodLogMutation(
        env: FoodLogEnvelopeDto,
        previewUri: String?
    ) {
        if (!loaded && _ui.value.items.isEmpty()) return

        when (env.status) {
            FoodLogStatus.SAVED -> {
                val result = env.nutritionResult
                val nutrients = result?.nutrients

                val existingItem = _ui.value.items
                    .firstOrNull { it.foodLogId == env.foodLogId }

                val updated = SavedFoodCardUi(
                    foodLogId = env.foodLogId,
                    displayTitle = result?.foodName
                        ?.trim()
                        .takeUnless { it.isNullOrBlank() }
                        ?: "Food",
                    kcal = (nutrients?.kcal ?: 0.0).roundToInt(),
                    proteinG = (nutrients?.protein ?: 0.0).roundToInt(),
                    carbsG = (nutrients?.carbs ?: 0.0).roundToInt(),
                    fatG = (nutrients?.fat ?: 0.0).roundToInt(),
                    timeText = FoodLogTimeResolver.resolveDisplayTimeText(
                        zoneId = zoneId,
                        createdAtUtc = env.createdAtUtc,
                        serverReceivedAtUtc = env.serverReceivedAtUtc,
                        capturedAtUtc = env.capturedAtUtc,
                        capturedLocalDate = env.capturedLocalDate
                    ).ifBlank { existingItem?.timeText.orEmpty() },
                    savedAtUtc = env.savedAtUtc ?: existingItem?.savedAtUtc,
                    previewUri = previewUri ?: existingItem?.previewUri
                )

                _ui.update { st ->
                    st.copy(
                        items = upsertSavedFoodBySavedAt(
                            current = st.items,
                            updated = updated
                        ),
                        error = null
                    )
                }
            }

            FoodLogStatus.DRAFT,
            FoodLogStatus.FAILED -> {
                _ui.update { st ->
                    st.copy(
                        items = st.items.filterNot { it.foodLogId == env.foodLogId },
                        error = null
                    )
                }
            }

            FoodLogStatus.DELETED -> {
                deletePreviewCache(env.foodLogId)

                _ui.update { st ->
                    st.copy(
                        items = st.items.filterNot { it.foodLogId == env.foodLogId },
                        error = null
                    )
                }
            }

            FoodLogStatus.PENDING -> Unit
        }
    }

    private fun upsertSavedFoodBySavedAt(
        current: List<SavedFoodCardUi>,
        updated: SavedFoodCardUi
    ): List<SavedFoodCardUi> {
        val merged = buildList {
            add(updated)
            current
                .filterNot { it.foodLogId == updated.foodLogId }
                .forEach(::add)
        }

        return merged
            .sortBySavedAtDescending(previousOrder = current)
            .take(PAGE_SIZE)
    }

    private fun List<SavedFoodCardUi>.sortBySavedAtDescending(
        previousOrder: List<SavedFoodCardUi> = this
    ): List<SavedFoodCardUi> {
        val previousIndexById = previousOrder
            .withIndex()
            .associate { it.value.foodLogId to it.index }

        return sortedWith { left, right ->
            val leftSavedAt = left.savedAtEpochMillis()
            val rightSavedAt = right.savedAtEpochMillis()

            when {
                leftSavedAt != null && rightSavedAt != null && leftSavedAt != rightSavedAt ->
                    rightSavedAt.compareTo(leftSavedAt)

                leftSavedAt != null && rightSavedAt == null -> -1
                leftSavedAt == null && rightSavedAt != null -> 1

                else -> {
                    val leftIndex = previousIndexById[left.foodLogId] ?: Int.MAX_VALUE
                    val rightIndex = previousIndexById[right.foodLogId] ?: Int.MAX_VALUE
                    leftIndex.compareTo(rightIndex)
                }
            }
        }
    }

    private fun SavedFoodCardUi.savedAtEpochMillis(): Long? {
        return UtcTimeFormatter
            .parseBackendUtcInstantOrNull(savedAtUtc)
            ?.toEpochMilli()
    }

    private fun applyFoodLogDeleted(foodLogId: String) {
        if (!loaded && _ui.value.items.isEmpty()) return

        deletePreviewCache(foodLogId)
        _ui.update { st ->
            st.copy(
                items = st.items.filterNot { it.foodLogId == foodLogId },
                error = null
            )
        }
    }

    private suspend fun cacheSavedPreview(foodLogId: String): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = repo.downloadImageBytes(foodLogId)
            if (bytes.isEmpty()) return@withContext null

            val dir = File(appContext.cacheDir, "foodlog_saved_preview").apply { mkdirs() }
            val file = File(dir, "saved_food_$foodLogId.img")

            file.writeBytes(bytes)
            file.setLastModified(System.currentTimeMillis())

            Uri.fromFile(file).toString()
        } catch (t: Throwable) {
            Log.w(
                TAG,
                "cacheSavedPreview failed id=$foodLogId: ${t.javaClass.simpleName}: ${t.message}",
                t
            )
            null
        }
    }

    private fun deletePreviewCache(foodLogId: String) {
        runCatching {
            val dir = File(appContext.cacheDir, "foodlog_saved_preview")
            val file = File(dir, "saved_food_$foodLogId.img")
            if (file.exists()) {
                file.delete()
            }
        }.onFailure { t ->
            Log.w(
                TAG,
                "deletePreviewCache failed id=$foodLogId: ${t.javaClass.simpleName}: ${t.message}",
                t
            )
        }
    }

    private fun prunePreviewCache() {
        val dir = File(appContext.cacheDir, "foodlog_saved_preview")
        if (!dir.exists() || !dir.isDirectory) return

        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(16)
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                runCatching { file.delete() }
            }
        }
    }
}
