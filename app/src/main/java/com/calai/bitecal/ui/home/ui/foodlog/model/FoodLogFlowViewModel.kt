package com.calai.bitecal.ui.home.ui.foodlog.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.repo.FoodLogsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class FoodLogFlowViewModel @Inject constructor(
    private val repo: FoodLogsRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val envelope: FoodLogEnvelopeDto? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun submitAlbum(ctx: Context, uri: Uri, onCreated: (foodLogId: String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                _state.value = UiState(loading = true)

                val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                val reqBody = bytes.toRequestBody("image/*".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "album.jpg", reqBody)

                val env = repo.submitAlbumImage(part)
                _state.value = UiState(loading = false, envelope = env)
                onCreated(env.foodLogId)
            }.onFailure {
                _state.value = UiState(loading = false, error = it.message ?: "submit failed")
            }
        }
    }

    fun startPolling(foodLogId: String) {
        viewModelScope.launch {
            runCatching {
                _state.value = _state.value.copy(loading = true)
                val env = repo.pollUntilTerminal(foodLogId)
                _state.value = UiState(loading = false, envelope = env)
            }.onFailure {
                _state.value = UiState(loading = false, error = it.message ?: "poll failed")
            }
        }
    }

    fun retry(foodLogId: String) {
        viewModelScope.launch {
            runCatching {
                _state.value = _state.value.copy(loading = true)
                val env = repo.retry(foodLogId)
                _state.value = UiState(loading = false, envelope = env)
            }.onFailure {
                _state.value = UiState(loading = false, error = it.message ?: "retry failed")
            }
        }
    }
}
