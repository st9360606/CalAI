package com.calai.bitecal.ui.home.ui.foodlog.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.foodlog.model.CooldownActiveDto
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.data.foodlog.model.ModelRefusedDto
import com.calai.bitecal.data.foodlog.repo.FoodLogApiException
import com.calai.bitecal.data.foodlog.repo.FoodLogsRepository
import com.calai.bitecal.data.foodlog.repo.MultipartParts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import javax.inject.Inject
import java.io.IOException

@HiltViewModel
class FoodLogFlowViewModel @Inject constructor(
    private val repo: FoodLogsRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val envelope: FoodLogEnvelopeDto? = null,
        val cooldown: CooldownActiveDto? = null,
        val refused: ModelRefusedDto? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var pollingJob: Job? = null

    fun reset() {
        pollingJob?.cancel()
        pollingJob = null
        _state.value = UiState()
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _state.value = _state.value.copy(loading = false)
    }

    // ✅ 共用：submit 前先停掉舊 polling（避免舊 polling 回來覆寫 state）
    private fun stopPollingSilently() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun readBytesOrThrow(ctx: Context, uri: Uri): ByteArray =
        withContext(Dispatchers.IO) {
            val inStream = ctx.contentResolver.openInputStream(uri)
                ?: throw IOException("openInputStream returned null: $uri")
            inStream.use { it.readBytes() }
        }

    fun submitAlbum(ctx: Context, uri: Uri, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val bytes = readBytesOrThrow(ctx, uri)
                val reqBody = bytes.toRequestBody("image/*".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "album.jpg", reqBody)

                // ✅ ALBUM：不傳 deviceCapturedAtUtc
                val env = repo.submitAlbumImage(part)
                _state.value = UiState(loading = false, envelope = env)
                onCreated(env.foodLogId)

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)
            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit album failed")
            }
        }
    }

    fun submitLabel(ctx: Context, uri: Uri, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val bytes = readBytesOrThrow(ctx, uri)
                val reqBody = bytes.toRequestBody("image/*".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "label.jpg", reqBody)

                val env = repo.submitLabelImage(part, deviceCapturedAtUtc = nowUtcPart())
                _state.value = UiState(loading = false, envelope = env)
                onCreated(env.foodLogId)

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)
            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit label failed")
            }
        }
    }

    fun submitBarcode(barcode: String, onResult: (FoodLogEnvelopeDto) -> Unit) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val env = repo.submitBarcode(barcode)
                _state.value = UiState(loading = false, envelope = env)
                onResult(env) // ✅ 改成整包給 UI 決策

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (ce: CancellationException) {
                // ✅ 被 reset/stop/submit 切換取消 → 不更新 state
                throw ce

            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit barcode failed")
            }
        }
    }

    fun startPolling(foodLogId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    loading = true,
                    cooldown = null,
                    refused = null,
                    error = null
                )

                val env = repo.pollUntilTerminal(foodLogId)
                _state.value = UiState(loading = false, envelope = env)

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (ce: CancellationException) {
                // ✅ 被 stop/reset/submit/retry 取消 → 不更新 state
                throw ce

            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "poll failed")
            }
        }
    }

    fun retry(foodLogId: String) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = _state.value.copy(
                    loading = true,
                    cooldown = null,
                    refused = null,
                    error = null
                )

                val env = repo.retry(foodLogId)

                // ✅ 小優化：回 PENDING 先顯示 loading，不要先 loading=false 再 startPolling 造成閃爍
                _state.value = UiState(
                    loading = (env.status == FoodLogStatus.PENDING),
                    envelope = env
                )

                // ✅ 關鍵：如果 retry 後回 PENDING，要重新輪詢
                if (env.status == FoodLogStatus.PENDING) {
                    startPolling(foodLogId)
                }

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (ce: CancellationException) {
                // ✅ 被 reset/stop/submit/retry 切換取消 → 不更新 state
                throw ce

            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "retry failed")
            }
        }
    }

    fun submitPhotoFile(file: File, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val part = MultipartParts.imagePartFromFile("file", "photo.jpg", file)
                val env = repo.submitPhotoImage(part, deviceCapturedAtUtc = nowUtcPart())

                _state.value = UiState(loading = false, envelope = env)
                onCreated(env.foodLogId)

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (ce: CancellationException) {
                // ✅ 被 reset/stop/submit 切換取消 → 不更新 state
                throw ce

            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit photo failed")

            } finally {
                runCatching { if (file.exists()) file.delete() }
            }
        }
    }

    fun submitLabelFile(file: File, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val part = MultipartParts.imagePartFromFile("file", "label.jpg", file)
                val env = repo.submitLabelImage(part, deviceCapturedAtUtc = nowUtcPart())

                _state.value = UiState(loading = false, envelope = env)
                onCreated(env.foodLogId)

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (ce: CancellationException) {
                // ✅ 被 reset/stop/submit 切換取消 → 不更新 state
                throw ce

            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit label failed")

            } finally {
                runCatching { if (file.exists()) file.delete() }
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        pollingJob = null
        super.onCleared()
    }

    private fun nowUtcPart(): okhttp3.RequestBody =
        Instant.now().toString().toRequestBody("text/plain".toMediaType())

    fun clearTransient() {
        // ✅ 只清掉 toast 類的 transient state，不動 envelope
        _state.value = _state.value.copy(
            loading = false,
            cooldown = null,
            refused = null,
            error = null
        )
    }
}
