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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

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

    private fun readBytesOrThrow(ctx: Context, uri: Uri): ByteArray {
        val inStream = ctx.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("openInputStream returned null: $uri")
        return inStream.use { it.readBytes() }
    }

    fun submitAlbum(ctx: Context, uri: Uri, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val bytes = readBytesOrThrow(ctx, uri)
                val reqBody = bytes.toRequestBody("image/*".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "album.jpg", reqBody)

                val env = repo.submitAlbumImage(part)
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

                val env = repo.submitLabelImage(part)
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
            }
        }
    }

    fun submitBarcode(barcode: String, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val env = repo.submitBarcode(barcode)
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
                val env = repo.submitPhotoImage(part)

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
                val env = repo.submitLabelImage(part)

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
