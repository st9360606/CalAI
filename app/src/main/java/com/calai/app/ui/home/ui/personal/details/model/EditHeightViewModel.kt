package com.calai.app.ui.home.ui.personal.details.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.repo.ProfileRepository
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.profile.repo.roundCm1
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditHeightViewModel @Inject constructor(
    private val store: UserProfileStore,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    data class UiState(
        val saving: Boolean = false,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    // ✅ 這兩個讓畫面初始化用（跟你 onboarding HeightSelectionScreen 用法一致）
    val heightCmState: StateFlow<Float> =
        store.heightCmFlow
            .map { it ?: 170f } // 沒值就給一個安全預設
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 170f)

    val heightUnitState: StateFlow<UserProfileStore.HeightUnit> =
        store.heightUnitFlow
            .map { it ?: UserProfileStore.HeightUnit.CM }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfileStore.HeightUnit.CM)

    /**
     * ✅ Continue：1) 存本機 2) upsert 同步後端 3) 成功就 onSuccess()
     */
    fun saveAndSyncHeight(
        useMetric: Boolean,
        cmVal: Double,
        feet: Int,
        inches: Int,
        onSuccess: () -> Unit
    ) {
        if (_ui.value.saving) return

        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }

            // 1) 本機先存（cm 為 SSOT，一位小數 floor）
            val cmToSave = roundCm1(cmVal).toFloat()
            runCatching { store.setHeightCm(cmToSave) }

            if (useMetric) {
                runCatching { store.setHeightUnit(UserProfileStore.HeightUnit.CM) }
                runCatching { store.clearHeightImperial() }
            } else {
                runCatching { store.setHeightUnit(UserProfileStore.HeightUnit.FT_IN) }
                runCatching { store.setHeightImperial(feet, inches) }
            }

            // 2) 同步後端（沿用你既有 upsert 策略：只送本機有的欄位；server 端 non-null 才覆寫）
            val result = profileRepo.upsertFromLocal()

            result.onSuccess {
                // 可選：用 server 回寫 store (避免 clamp 後不一致)
                runCatching { profileRepo.syncServerProfileToStore() }
                _ui.update { it.copy(saving = false, error = null) }
                onSuccess()
            }.onFailure { e ->
                _ui.update {
                    it.copy(
                        saving = false,
                        error = "Network error. Saved locally, but failed to sync."
                    )
                }
            }
        }
    }
}
