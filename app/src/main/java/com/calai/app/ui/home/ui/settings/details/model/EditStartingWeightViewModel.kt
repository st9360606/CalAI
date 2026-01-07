package com.calai.app.ui.home.ui.settings.details.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.repo.ProfileRepository
import com.calai.app.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditStartingWeightViewModel @Inject constructor(
    private val store: UserProfileStore,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    data class UiState(
        val unit: UserProfileStore.WeightUnit = UserProfileStore.WeightUnit.LBS,
        val startingKg: Double? = null,
        val startingLbs: Double? = null,
        val saving: Boolean = false,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch {
            store.weightUnitFlow
                .distinctUntilChanged()
                .collect { u ->
                    _ui.update { it.copy(unit = u ?: UserProfileStore.WeightUnit.LBS) }
                }
        }
        viewModelScope.launch {
            store.weightKgFlow.distinctUntilChanged().collect { kg ->
                _ui.update { it.copy(startingKg = kg?.toDouble()) }
            }
        }
        viewModelScope.launch {
            store.weightLbsFlow.distinctUntilChanged().collect { lbs ->
                _ui.update { it.copy(startingLbs = lbs?.toDouble()) }
            }
        }
    }

    /**
     * ✅ 存 Starting Weight（user_profiles.weight_kg/weight_lbs）
     * - 成功後 commit unit（跟你 EditGoalWeight 的行為一致：用戶用什麼單位存，就切到該單位）
     */
    fun updateStartingWeight(
        value: Double,
        unit: UserProfileStore.WeightUnit,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }

            val res = profileRepo.updateStartingWeight(value, unit)

            res.onSuccess { resp ->
                // ✅ commit unit
                runCatching { store.setWeightUnit(unit) }

                // ✅ 再保險同步一次（repo 已做過，這裡可留可不留）
                runCatching {
                    resp.weightKg?.let { store.setWeightKg(it.toFloat()) }
                    resp.weightLbs?.let { store.setWeightLbs(it.toFloat()) }
                }

                _ui.update { it.copy(saving = false, error = null) }
                onResult(Result.success(Unit))
            }.onFailure { e ->
                _ui.update { it.copy(saving = false, error = e.message ?: "Save failed") }
                onResult(Result.failure(e))
            }
        }
    }
}
