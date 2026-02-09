package com.calai.bitecal.ui.home.ui.foodlog.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.foodlog.model.CooldownActiveDto
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.ModelRefusedDto
import com.calai.bitecal.data.foodlog.repo.FoodLogApiException
import com.calai.bitecal.data.foodlog.repo.FoodLogsRepository
import com.calai.bitecal.data.foodlog.repo.MultipartParts
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun submitAlbum(ctx: Context, uri: Uri, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            try {
                _state.value = UiState(loading = true)
                val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                val reqBody = bytes.toRequestBody("image/*".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "album.jpg", reqBody)

                val env = repo.submitAlbumImage(part)
                _state.value = UiState(loading = false, envelope = env)
                onCreated(env.foodLogId)
            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)
            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)
            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit album failed")
            }
        }
    }

    fun submitLabel(ctx: Context, uri: Uri, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            try {
                _state.value = UiState(loading = true)
                val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                val reqBody = bytes.toRequestBody("image/*".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "label.jpg", reqBody)

                val env = repo.submitLabelImage(part)
                _state.value = UiState(loading = false, envelope = env)
                onCreated(env.foodLogId)
            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)
            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)
            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit label failed")
            }
        }
    }

    fun submitBarcode(barcode: String, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            try {
                _state.value = UiState(loading = true)
                val env = repo.submitBarcode(barcode)
                _state.value = UiState(loading = false, envelope = env)
                onCreated(env.foodLogId)
            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)
            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)
            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit barcode failed")
            }
        }
    }

    fun startPolling(foodLogId: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(loading = true, cooldown = null, refused = null, error = null)
                val env = repo.pollUntilTerminal(foodLogId)
                _state.value = UiState(loading = false, envelope = env)
            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)
            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "poll failed")
            }
        }
    }

    fun retry(foodLogId: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(loading = true, cooldown = null, refused = null, error = null)
                val env = repo.retry(foodLogId)
                _state.value = UiState(loading = false, envelope = env)

            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)

            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)

            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "retry failed")
            }
        }
    }

    fun submitPhotoFile(file: File, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            try {
                _state.value = UiState(loading = true)

                val part = MultipartParts.imagePartFromFile("file", "photo.jpg", file)
                val env = repo.submitPhotoImage(part)

                _state.value = UiState(loading = false, envelope = env)
                onCreated(env.foodLogId)
            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)
            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)
            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit photo failed")
            } finally {
                runCatching { file.delete() } // ✅ 用 cache temp file：用完就清
            }
        }
    }

    fun submitLabelFile(file: File, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            try {
                _state.value = UiState(loading = true)

                val part = MultipartParts.imagePartFromFile("file", "label.jpg", file)
                val env = repo.submitLabelImage(part)

                _state.value = UiState(loading = false, envelope = env)
                onCreated(env.foodLogId)
            } catch (e: FoodLogApiException.CooldownActive) {
                _state.value = UiState(loading = false, cooldown = e.dto)
            } catch (e: FoodLogApiException.ModelRefused) {
                _state.value = UiState(loading = false, refused = e.dto)
            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = t.message ?: "submit label failed")
            } finally {
                runCatching { file.delete() }
            }
        }
    }
}
