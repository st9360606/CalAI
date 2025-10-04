package com.calai.app.ui.onboarding.targetweight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.auth.store.UserProfileStore
import com.calai.app.data.auth.store.UserProfileStore.WeightUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeightTargetViewModel @Inject constructor(
    private val usr: UserProfileStore
) : ViewModel() {

    // 使用「目標體重」欄位
    val weightKgState = usr.targetWeightKgFlow
        .map { it ?: 65.0f } // 預設 65.0 kg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 65.0f)

    val weightUnitState = usr.targetWeightUnitFlow
        .map { it ?: WeightUnit.KG } // 預設 KG
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.KG)

    fun saveWeightKg(kg: Float) = viewModelScope.launch {
        usr.setTargetWeightKg(kg)
    }

    fun saveWeightUnit(u: WeightUnit) = viewModelScope.launch {
        usr.setTargetWeightUnit(u)
    }
}
