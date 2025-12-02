package com.calai.app.ui.home.ui.personal.details.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.repo.ProfileRepository
import com.calai.app.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditAgeViewModel @Inject constructor(
    private val store: UserProfileStore,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    data class UiState(
        val initializing: Boolean = true,
        val saving: Boolean = false,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    // ✅ 給 UI 用的「初始化後的確定年齡」（DB 優先，否則 fallback）
    private val _initialAge = MutableStateFlow(25)
    val initialAge: StateFlow<Int> = _initialAge.asStateFlow()

    private var initOnce = false

    fun initIfNeeded() {
        if (initOnce) return
        initOnce = true

        viewModelScope.launch {
            _ui.update { it.copy(initializing = true, error = null) }

            val localFallback = runCatching { store.ageFlow.first() ?: 25 }
                .getOrElse { 25 }

            val resolved = runCatching {
                // ✅ 直接用你現成的全量同步（最不怕欄位名不一致）
                profileRepo.syncServerProfileToStore()
                store.ageFlow.first() ?: localFallback
            }.getOrElse { e ->
                Log.w("EditAgeVM", "initIfNeeded failed: ${e.message}", e)
                localFallback
            }

            _initialAge.value = resolved
            _ui.update { it.copy(initializing = false) }
        }
    }

    fun saveAndSyncAge(ageYears: Int, onSuccess: () -> Unit) {
        if (_ui.value.saving) return

        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }

            runCatching { store.setAge(ageYears) }

            val result = profileRepo.upsertFromLocal()
            result.onSuccess {
                _ui.update { it.copy(saving = false, error = null) }
                onSuccess()

                viewModelScope.launch {
                    runCatching { profileRepo.syncServerProfileToStore() }
                        .onFailure { e ->
                            Log.w("EditAgeVM", "syncServerProfileToStore failed: ${e.message}", e)
                        }
                }
            }.onFailure { e ->
                val msg = e.message?.takeIf { it.isNotBlank() }
                    ?: "Network error. Saved locally, but failed to sync."
                _ui.update { it.copy(saving = false, error = msg) }
            }
        }
    }
}
