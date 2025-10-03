package com.calai.app.ui.onboarding.weight

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
class WeightSelectionViewModel @Inject constructor(
    private val usr: UserProfileStore
) : ViewModel() {

    // 預設 65.0 kg
    val weightKgState = usr.weightKgFlow
        .map { it ?: 65.0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 65.0f)

    // 體重單位（預設 KG）
    val weightUnitState = usr.weightUnitFlow
        .map { it ?: WeightUnit.KG }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.KG)

    fun saveWeightKg(kg: Float) = viewModelScope.launch {
        usr.setWeightKg(kg)
    }

    fun saveWeightUnit(u: WeightUnit) = viewModelScope.launch {
        usr.setWeightUnit(u)
    }
}
