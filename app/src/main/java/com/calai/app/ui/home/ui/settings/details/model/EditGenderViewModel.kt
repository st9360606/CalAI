package com.calai.app.ui.home.ui.settings.details.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.repo.ProfileRepository
import com.calai.app.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EditGenderViewModel @Inject constructor(
    private val store: UserProfileStore,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    enum class GenderKey { MALE, FEMALE, OTHER }

    data class UiState(
        val saving: Boolean = false,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var refreshedOnce = false

    // ✅ 關鍵：不要用 OTHER 當初始值，先用 null（UI 就不會先亮錯）
    val genderState: StateFlow<GenderKey?> =
        store.genderFlow
            .map { raw ->
                val g = raw?.trim()?.lowercase(Locale.US)
                if (g.isNullOrBlank()) return@map null
                when (g) {
                    "male", "m" -> GenderKey.MALE
                    "female", "f" -> GenderKey.FEMALE
                    "other" -> GenderKey.OTHER
                    else -> null
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refreshGenderFromServerIfNeeded() {
        if (refreshedOnce) return
        refreshedOnce = true

        viewModelScope.launch {
            runCatching {
                val p = profileRepo.getServerProfileOrNull()
                val gRaw = p?.gender?.trim()
                if (!gRaw.isNullOrBlank()) {
                    store.setGender(gRaw)
                }
            }.onFailure { e ->
                Log.w("EditGenderVM", "refreshGenderFromServerIfNeeded failed: ${e.message}", e)
            }
        }
    }

    private fun keyToServerValue(key: GenderKey): String = when (key) {
        GenderKey.MALE -> "Male"
        GenderKey.FEMALE -> "Female"
        GenderKey.OTHER -> "Other"
    }

    fun saveAndSyncGender(
        selected: GenderKey,
        onSuccess: () -> Unit
    ) {
        if (_ui.value.saving) return

        viewModelScope.launch {
            _ui.value = UiState(saving = true, error = null)

            val toSave = keyToServerValue(selected)

            // 1) 先本機保存（體感快）
            runCatching { store.setGender(toSave) }

            // 2) 同步 server（你已經有 profileRepo.updateGenderOnly）
            val result = profileRepo.updateGenderOnly(toSave)
            result.onSuccess {
                _ui.value = UiState(saving = false, error = null)
                onSuccess()

                // 3) 背景回寫校正
                viewModelScope.launch {
                    runCatching { profileRepo.syncServerProfileToStore() }
                        .onFailure { e ->
                            Log.w("EditGenderVM", "syncServerProfileToStore failed: ${e.message}", e)
                        }
                }
            }.onFailure { e ->
                val msg = e.message?.takeIf { it.isNotBlank() }
                    ?: "Network error. Saved locally, but failed to sync."
                _ui.value = UiState(saving = false, error = msg)
            }
        }
    }
}
