package com.calai.bitecal.ui.onboarding.goalweight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.profile.repo.UserProfileStore
import com.calai.bitecal.data.profile.repo.UserProfileStore.WeightUnit
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

    /**
     * 是否已有 user_profiles：
     * - true  => user_profiles 存在（可用 unit_preference）
     * - false => user_profiles 不存在（初始一律顯示 LBS）
     */
    val hasProfileState = usr.hasServerProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * 目標體重 kg
     * null -> 0f，代表未設定
     */
    val weightKgState = usr.goalWeightKgFlow
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    /**
     * 目標體重單位
     * null -> LBS，符合「沒 profile 一律顯示 LBS」的需求
     */
    val weightUnitState = usr.goalWeightUnitFlow
        .map { it ?: WeightUnit.LBS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.LBS)

    /**
     * 目標體重 lbs
     * null -> 0f，代表未設定
     */
    val weightLbsState = usr.goalWeightLbsFlow
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    /**
     * 給 Continue 用：
     * - kg 一律存（SSOT）
     * - useMetric=true  => unit=KG、清 lbs
     * - useMetric=false => unit=LBS、存 lbs
     */
    suspend fun saveAll(
        kgToSave: Float,
        useMetric: Boolean,
        lbsToSaveOrNull: Float?
    ) {
        usr.setGoalWeightKg(kgToSave)

        if (useMetric) {
            usr.setGoalWeightUnit(WeightUnit.KG)
            usr.clearGoalWeightLbs()
        } else {
            usr.setGoalWeightUnit(WeightUnit.LBS)
            usr.setGoalWeightLbs(lbsToSaveOrNull ?: 0f)
        }
    }

    // 保留舊 API，避免其他地方壞掉
    fun saveWeightKg(kg: Float) = viewModelScope.launch {
        usr.setGoalWeightKg(kg)
    }

    fun saveWeightUnit(u: WeightUnit) = viewModelScope.launch {
        usr.setGoalWeightUnit(u)
    }

    fun saveGoalWeightLbs(lbs: Float) = viewModelScope.launch {
        usr.setGoalWeightLbs(lbs)
    }

    fun clearGoalWeightLbs() = viewModelScope.launch {
        usr.clearGoalWeightLbs()
    }
}
