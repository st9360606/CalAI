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
import android.util.Log

@HiltViewModel
class EditHeightViewModel @Inject constructor(
    private val store: UserProfileStore,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    data class UiState(
        val saving: Boolean = false,
        val error: String? = null,
        val toastMessage: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    val heightCmState: StateFlow<Float> =
        store.heightCmFlow
            .map { it ?: 170f }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 170f)

    val heightUnitState: StateFlow<UserProfileStore.HeightUnit> =
        store.heightUnitFlow
            .map { it ?: UserProfileStore.HeightUnit.CM }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                UserProfileStore.HeightUnit.CM
            )

    fun saveAndSyncHeight(
        useMetric: Boolean,
        cmVal: Double,
        feet: Int,
        inches: Int,
        onSuccess: () -> Unit
    ) {
        if (_ui.value.saving) return

        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null, toastMessage = null) }

            val cmToSave = roundCm1(cmVal).toFloat()
            runCatching { store.setHeightCm(cmToSave) }

            if (useMetric) {
                runCatching { store.setHeightUnit(UserProfileStore.HeightUnit.CM) }
                runCatching { store.clearHeightImperial() }
            } else {
                runCatching { store.setHeightUnit(UserProfileStore.HeightUnit.FT_IN) }
                runCatching { store.setHeightImperial(feet, inches) }
            }

            val result = profileRepo.upsertFromLocal()
            result.onSuccess {
                //先結束 loading、先讓 UI 回上一頁（體感速度會快很多）
                _ui.update {
                    it.copy(
                        saving = false,
                        error = null,
                        toastMessage = "Saved successfully!"
                    )
                }
                onSuccess()
                // 把「回寫校正」放到背景做，不要卡住 UI
                viewModelScope.launch {
                    runCatching { profileRepo.syncServerProfileToStore() }
                        .onFailure { e ->
                            Log.w("EditHeightVM", "syncServerProfileToStore failed: ${e.message}", e)
                        }
                }

            }.onFailure { e ->
                val msg = e.message?.takeIf { it.isNotBlank() }
                    ?: "Network error. Saved locally, but failed to sync."

                _ui.update {
                    it.copy(
                        saving = false,
                        error = msg,
                        toastMessage = msg
                    )
                }
            }
        }
    }
}
