package com.calai.app.ui.onboarding.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.store.UserProfileStore
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 70.0f)

    fun saveWeightKg(kg: Float) = viewModelScope.launch {
        usr.setWeightKg(kg)
    }
}

/** 換算 & 取一位小數 */
fun kgToLbs(v: Double): Double = v * 2.2046226218
fun lbsToKg(v: Double): Double = v / 2.2046226218
fun round1(v: Double): Double = kotlin.math.round(v * 10.0) / 10.0
