package com.calai.bitecal.ui.home.ui.savedfood.model

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.foodlog.model.FoodLogListItemDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.data.foodlog.repo.FoodLogsRepository
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    val previewUri: String?
)

@HiltViewModel
class SavedFoodsViewModel @Inject constructor(
    private val repo: FoodLogsRepository,
    private val zoneId: ZoneId,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _ui = MutableStateFlow(SavedFoodsUiState(loading = true))
    val ui: StateFlow<SavedFoodsUiState> = _ui.asStateFlow()

    private var loaded = false
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private companion object {
        const val TAG = "SavedFoodsVm"
        const val LOOK_BACK_DAYS = 15
        const val PAGE_SIZE = 100
    }

    fun loadIfNeeded() {
        if (loaded) return
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

    fun unsave(foodLogId: String) {
        viewModelScope.launch {
            runCatching {
                repo.unsave(foodLogId)
            }.onSuccess {
                deletePreviewCache(foodLogId)
                _ui.update { st ->
                    st.copy(
                        items = st.items.filterNot { it.foodLogId == foodLogId },
                        error = null
                    )
                }
            }.onFailure { t ->
                Log.w(TAG, "unsave failed id=$foodLogId: ${t.javaClass.simpleName}: ${t.message}", t)
                _ui.update { it.copy(error = t.message ?: "unsave failed") }
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
                        timeText = formatTimeText(item),
                        previewUri = previewUri
                    )
                )
            }
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

    private fun formatTimeText(item: FoodLogListItemDto): String {
        parseUtcToLocalTime(item.serverReceivedAtUtc)?.let { return it }
        parseUtcToLocalTime(item.capturedAtUtc)?.let { return it }
        return item.capturedLocalDate.orEmpty()
    }

    private fun parseUtcToLocalTime(raw: String?): String? {
        val value = raw?.trim()
        if (value.isNullOrBlank()) return null

        return runCatching {
            Instant.parse(value)
                .atZone(zoneId)
                .toLocalTime()
                .format(timeFormatter)
        }.getOrNull()
    }
}
