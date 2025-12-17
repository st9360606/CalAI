package com.calai.app.ui.onboarding.weight

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
class WeightSelectionViewModel @Inject constructor(
    private val usr: UserProfileStore
) : ViewModel() {

    val weightKgState = usr.weightKgFlow
        .map { it ?: 65.0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 65.0f)

    val weightUnitState = usr.weightUnitFlow
        .map { it ?: WeightUnit.KG }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.KG)

    // lbs 狀態（null → 0f 當作沒資料）
    val weightLbsState = usr.weightLbsFlow
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    fun saveWeightKg(kg: Float) = viewModelScope.launch { usr.setWeightKg(kg) }

    fun saveWeightUnit(u: WeightUnit) = viewModelScope.launch { usr.setWeightUnit(u) }

    // lbs = 0.1 精度，呼叫者已處理
    fun saveWeightLbs(lbs: Float) = viewModelScope.launch { usr.setWeightLbs(lbs) }
    fun clearWeightLbs() = viewModelScope.launch { usr.clearWeightLbs() }
}
