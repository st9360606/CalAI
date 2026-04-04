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
import com.calai.bitecal.data.foodlog.repo.MultipartParts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import okhttp3.RequestBody
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
        val apiError: ApiErrorDto? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var pollingJob: Job? = null
    private var pollingGeneration: Long = 0L

    fun reset() {
        pollingGeneration++
        pollingJob?.cancel()
        pollingJob = null
        _state.value = UiState()
    }

    fun stopPolling() {
        pollingGeneration++
        pollingJob?.cancel()
        pollingJob = null
        _state.value = _state.value.copy(loading = false)
    }

    private fun stopPollingSilently() {
        pollingGeneration++
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        pollingGeneration++
        pollingJob?.cancel()
        pollingJob = null
        super.onCleared()
    }

    fun submitAlbum(
        ctx: Context,
        uri: Uri,
        onCreated: (FoodLogEnvelopeDto) -> Unit
    ) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val jpegBytes = withContext(Dispatchers.IO) {
                    ImageCompressUtil.compressUriToJpegBytes(
                        ctx = ctx,
                        uri = uri,
                        maxSide = 1600,
                        quality = 82
                    )
                }

                val part = MultipartParts.jpegImagePart(
                    filename = "album.jpg",
                    jpegBytes = jpegBytes
                )

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
                _state.value = UiState(
                    loading = false,
                    error = t.message ?: "submit album failed"
                )
            }
        }
    }

    fun submitBarcode(
        barcode: String,
        onResult: (FoodLogEnvelopeDto) -> Unit
    ) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val env = repo.submitBarcode(barcode)
                _state.value = UiState(loading = false, envelope = env)
                onResult(env)

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
                _state.value = UiState(
                    loading = false,
                    error = t.message ?: "submit barcode failed"
                )
            }
        }
    }

    fun startPolling(foodLogId: String) {
        pollingJob?.cancel()
        val generation = ++pollingGeneration

        pollingJob = viewModelScope.launch {
            try {
                val sameFoodLog = _state.value.envelope?.foodLogId == foodLogId

                _state.value = if (sameFoodLog) {
                    _state.value.copy(
                        loading = true,
                        cooldown = null,
                        refused = null,
                        apiError = null,
                        error = null
                    )
                } else {
                    UiState(loading = true)
                }

                val env = repo.pollUntilTerminal(foodLogId)

                if (!isActive || generation != pollingGeneration) return@launch

                _state.value = UiState(
                    loading = false,
                    envelope = env
                )

            } catch (e: FoodLogApiException.CooldownActive) {
                if (!isActive || generation != pollingGeneration) return@launch
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                if (!isActive || generation != pollingGeneration) return@launch
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (e: FoodLogApiException.BusinessError) {
                if (!isActive || generation != pollingGeneration) return@launch
                _state.value = UiState(
                    loading = false,
                    apiError = e.dto.toApiErrorDto(),
                    error = e.dto.message ?: e.dto.normalizedCode()
                )

            } catch (ce: CancellationException) {
                throw ce

            } catch (t: Throwable) {
                if (!isActive || generation != pollingGeneration) return@launch
                _state.value = UiState(
                    loading = false,
                    error = t.message ?: "poll failed"
                )
            }
        }
    }

    fun save(foodLogId: String) {
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

                val env = repo.save(foodLogId)

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

            } catch (e: FoodLogApiException.BusinessError) {
                _state.value = UiState(
                    loading = false,
                    apiError = e.dto.toApiErrorDto(),
                    error = e.dto.message ?: e.dto.normalizedCode()
                )

            } catch (ce: CancellationException) {
                throw ce

            } catch (t: Throwable) {
                _state.value = UiState(
                    loading = false,
                    error = t.message ?: "save failed"
                )
            }
        }
    }

    fun delete(
        foodLogId: String,
        onSuccess: () -> Unit
    ) {
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

                repo.delete(foodLogId)

                _state.value = _state.value.copy(loading = false)
                onSuccess()

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
                _state.value = UiState(
                    loading = false,
                    error = t.message ?: "delete failed"
                )
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

            } catch (e: FoodLogApiException.BusinessError) {
                _state.value = UiState(
                    loading = false,
                    apiError = e.dto.toApiErrorDto(),
                    error = e.dto.message ?: e.dto.normalizedCode()
                )

            } catch (ce: CancellationException) {
                throw ce

            } catch (t: Throwable) {
                _state.value = UiState(
                    loading = false,
                    error = t.message ?: "retry failed"
                )
            }
        }
    }

    fun commitDetailChanges(
        foodLogId: String,
        baseEnv: FoodLogEnvelopeDto,
        multiplier: Int,
        targetSaved: Boolean,
        onSuccess: (FoodLogEnvelopeDto) -> Unit = {}
    ) {
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

                var latest = applyMultiplierOverridesInternal(
                    foodLogId = foodLogId,
                    baseEnv = baseEnv,
                    multiplier = multiplier
                )

                val targetStatus = if (targetSaved) {
                    FoodLogStatus.SAVED
                } else {
                    FoodLogStatus.DRAFT
                }

                latest = when {
                    latest.status == targetStatus -> latest

                    latest.status == FoodLogStatus.DRAFT &&
                            targetStatus == FoodLogStatus.SAVED -> {
                        repo.save(foodLogId)
                    }

                    latest.status == FoodLogStatus.SAVED &&
                            targetStatus == FoodLogStatus.DRAFT -> {
                        repo.unsave(foodLogId)
                    }

                    else -> {
                        repo.getOne(foodLogId)
                    }
                }

                _state.value = UiState(
                    loading = false,
                    envelope = latest
                )

                onSuccess(latest)

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
                _state.value = UiState(
                    loading = false,
                    error = t.message ?: "commit detail changes failed"
                )
            }
        }
    }

    fun submitPhotoFile(
        file: File,
        onCreated: (FoodLogEnvelopeDto) -> Unit
    ) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val jpegBytes = withContext(Dispatchers.IO) {
                    ImageCompressUtil.compressFileToJpegBytes(
                        file = file,
                        maxSide = 1600,
                        quality = 82
                    )
                }

                val part = MultipartParts.jpegImagePart(
                    filename = "photo.jpg",
                    jpegBytes = jpegBytes
                )

                val env = repo.submitPhotoImage(
                    part = part,
                    deviceCapturedAtUtc = nowUtcPart()
                )

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
                _state.value = UiState(
                    loading = false,
                    error = t.message ?: "submit photo failed"
                )

            } finally {
                runCatching {
                    if (file.exists()) file.delete()
                }
            }
        }
    }

    fun submitLabelFile(
        file: File,
        onCreated: (FoodLogEnvelopeDto) -> Unit
    ) {
        viewModelScope.launch {
            try {
                stopPollingSilently()
                _state.value = UiState(loading = true)

                val jpegBytes = withContext(Dispatchers.IO) {
                    ImageCompressUtil.compressFileToJpegBytes(
                        file = file,
                        maxSide = 1600,
                        quality = 82
                    )
                }

                val part = MultipartParts.jpegImagePart(
                    filename = "label.jpg",
                    jpegBytes = jpegBytes
                )

                val env = repo.submitLabelImage(
                    part = part,
                    deviceCapturedAtUtc = nowUtcPart()
                )

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
                _state.value = UiState(
                    loading = false,
                    error = t.message ?: "submit label failed"
                )

            } finally {
                runCatching {
                    if (file.exists()) file.delete()
                }
            }
        }
    }

    private fun nowUtcPart(): RequestBody =
        Instant.now().toString().toRequestBody(null)

    fun clearTransient() {
        _state.value = _state.value.copy(
            loading = false,
            cooldown = null,
            refused = null,
            apiError = null,
            error = null
        )
    }

    private suspend fun applyMultiplierOverridesInternal(
        foodLogId: String,
        baseEnv: FoodLogEnvelopeDto,
        multiplier: Int
    ): FoodLogEnvelopeDto {
        val currentMultiplier = baseEnv.portionMultiplier.coerceAtLeast(1)
        if (multiplier == currentMultiplier) return baseEnv

        return repo.applyPortionMultiplier(
            id = foodLogId,
            multiplier = multiplier,
            reason = "RECENT_UPLOAD_MULTIPLIER_X$multiplier"
        )
    }
}
