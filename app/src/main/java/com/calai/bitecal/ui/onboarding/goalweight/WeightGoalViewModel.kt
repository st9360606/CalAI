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
     * ✅ 你要的「是否有 user_profiles」：
     * 用 DataStore 的 HAS_SERVER_PROFILE 當作是否存在 profile 的旗標。
     * - true  => user_profiles 存在（可用 unit_preference）
     * - false => user_profiles 不存在（初始一律顯示 LBS）
     */
    val hasProfileState = usr.hasServerProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * ✅ 注意：不要用 65.0f 當預設，否則 UI 會永遠以為有 kg，
     * 你 154.0 lbs 的預設分支就永遠不會被走到。
     */
    val weightKgState = usr.goalWeightKgFlow
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    /**
     * ✅ 沒值時預設 LBS（更符合「沒 profile 一律顯示 LBS」的需求）
     * 有 profile 時會由 Screen 用 hasProfileState + savedUnit 來決定。
     */
    val weightUnitState = usr.goalWeightUnitFlow
        .map { it ?: WeightUnit.LBS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.LBS)

    // 新增：目標體重 lbs 狀態（null → 0f，視為沒設定）
    val weightLbsState = usr.goalWeightLbsFlow
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    fun saveWeightKg(kg: Float) = viewModelScope.launch { usr.setGoalWeightKg(kg) }
    fun saveWeightUnit(u: WeightUnit) = viewModelScope.launch { usr.setGoalWeightUnit(u) }

    fun saveGoalWeightLbs(lbs: Float) = viewModelScope.launch { usr.setGoalWeightLbs(lbs) }
    fun clearGoalWeightLbs() = viewModelScope.launch { usr.clearGoalWeightLbs() }
}
