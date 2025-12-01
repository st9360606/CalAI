package com.calai.app.ui.onboarding.goalweight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.profile.repo.UserProfileStore.WeightUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeightGoalViewModel @Inject constructor(
    private val usr: UserProfileStore
) : ViewModel() {

    val weightKgState = usr.goalWeightKgFlow
        .map { it ?: 65.0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 65.0f)

    val weightUnitState = usr.goalWeightUnitFlow
        .map { it ?: WeightUnit.KG }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.KG)

    // 新增：目標體重 lbs 狀態（null → 0f，視為沒設定）
    val weightLbsState = usr.goalWeightLbsFlow
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    fun saveWeightKg(kg: Float) = viewModelScope.launch { usr.setGoalWeightKg(kg) }
    fun saveWeightUnit(u: WeightUnit) = viewModelScope.launch { usr.setGoalWeightUnit(u) }

    fun saveGoalWeightLbs(lbs: Float) = viewModelScope.launch { usr.setGoalWeightLbs(lbs) }
    fun clearGoalWeightLbs() = viewModelScope.launch { usr.clearGoalWeightLbs() }
}
