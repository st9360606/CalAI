package com.calai.bitecal.ui.home.ui.foodlog.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.foodlog.model.ApiErrorDto
import com.calai.bitecal.data.foodlog.model.CooldownActiveDto
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.data.foodlog.model.ModelRefusedDto
import com.calai.bitecal.data.foodlog.repo.FoodLogApiException
import com.calai.bitecal.data.foodlog.repo.FoodLogsRepository
import com.calai.bitecal.data.foodlog.repo.ImageCompressUtil
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

@HiltViewModel
class FoodLogFlowViewModel @Inject constructor(
    private val repo: FoodLogsRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val envelope: FoodLogEnvelopeDto? = null,
        val cooldown: CooldownActiveDto? = null,
        val refused: ModelRefusedDto? = null,
        val apiError: ApiErrorDto? = null, // ✅ NEW
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

    fun submitAlbum(ctx: Context, uri: Uri, onCreated: (FoodLogEnvelopeDto) -> Unit) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val bytes = withContext(Dispatchers.IO) {
                    ImageCompressUtil.compressUriToJpegBytes(
                        ctx = ctx,
                        uri = uri,
                        maxSide = 1600,
                        quality = 82
                    )
                }
                val reqBody = bytes.toRequestBody("image/jpeg".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "album.jpg", reqBody)

                val env = repo.submitAlbumImage(part)
                _state.value = UiState(loading = false, envelope = env)
                onCreated(env)

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (e: FoodLogApiException.BusinessError) {
                _state.value = UiState(
                    loading = false,
                    apiError = e.dto.toApiErrorDto(),
                    error = e.dto.message ?: e.dto.normalizedCode()
                )

            } catch (ce: CancellationException) {
                throw ce

            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit album failed")
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

            } catch (e: FoodLogApiException.BusinessError) {
                _state.value = UiState(
                    loading = false,
                    apiError = e.dto.toApiErrorDto(),
                    error = e.dto.message ?: e.dto.normalizedCode()
                )

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
                    apiError = null,
                    error = null
                )

                val env = repo.pollUntilTerminal(foodLogId)
                _state.value = UiState(loading = false, envelope = env)

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (e: FoodLogApiException.BusinessError) { // ✅ 補這個
                _state.value = UiState(
                    loading = false,
                    apiError = e.dto.toApiErrorDto(),
                    error = e.dto.message ?: e.dto.normalizedCode()
                )

            } catch (ce: CancellationException) {
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
                    apiError = null,
                    error = null
                )

                val env = repo.retry(foodLogId)

                _state.value = UiState(
                    loading = (env.status == FoodLogStatus.PENDING),
                    envelope = env
                )

                if (env.status == FoodLogStatus.PENDING) {
                    startPolling(foodLogId)
                }

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (e: FoodLogApiException.BusinessError) { // ✅ 補這個
                _state.value = UiState(
                    loading = false,
                    apiError = e.dto.toApiErrorDto(),
                    error = e.dto.message ?: e.dto.normalizedCode()
                )

            } catch (ce: CancellationException) {
                throw ce

            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "retry failed")
            }
        }
    }

    fun submitPhotoFile(file: File, onCreated: (FoodLogEnvelopeDto) -> Unit) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val bytes = withContext(Dispatchers.IO) {
                    ImageCompressUtil.compressFileToJpegBytes(
                        file = file,
                        maxSide = 1600,
                        quality = 82
                    )
                }

                val reqBody = bytes.toRequestBody("image/jpeg".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "photo.jpg", reqBody)
                val env = repo.submitPhotoImage(part, deviceCapturedAtUtc = nowUtcPart())

                _state.value = UiState(loading = false, envelope = env)
                onCreated(env)

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (e: FoodLogApiException.BusinessError) {
                _state.value = UiState(
                    loading = false,
                    apiError = e.dto.toApiErrorDto(),
                    error = e.dto.message ?: e.dto.normalizedCode()
                )

            } catch (ce: CancellationException) {
                throw ce

            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit photo failed")

            } finally {
                runCatching { if (file.exists()) file.delete() }
            }
        }
    }

    fun submitLabelFile(file: File, onCreated: (FoodLogEnvelopeDto) -> Unit) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val bytes = withContext(Dispatchers.IO) {
                    ImageCompressUtil.compressFileToJpegBytes(
                        file = file,
                        maxSide = 1600,
                        quality = 82
                    )
                }

                val reqBody = bytes.toRequestBody("image/jpeg".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "label.jpg", reqBody)
                val env = repo.submitLabelImage(part, deviceCapturedAtUtc = nowUtcPart())

                _state.value = UiState(loading = false, envelope = env)
                onCreated(env)

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (e: FoodLogApiException.BusinessError) {
                _state.value = UiState(
                    loading = false,
                    apiError = e.dto.toApiErrorDto(),
                    error = e.dto.message ?: e.dto.normalizedCode()
                )

            } catch (ce: CancellationException) {
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
            apiError = null,
            error = null
        )
    }
}
