package com.calai.bitecal.ui.home.ui.settings.details.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonalDetailsViewModel @Inject constructor(
    private val store: UserProfileStore
) : ViewModel() {

    // ✅ 沒設定時給預設（跟你 WeightViewModel 預設一致）
    val unit: StateFlow<UserProfileStore.WeightUnit> =
        store.weightUnitFlow
            .map { it ?: UserProfileStore.WeightUnit.LBS }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfileStore.WeightUnit.LBS)

    /**
     * ✅ 保存成功後呼叫：用「實際保存的 unit」覆蓋顯示偏好
     * （符合：Change Goal/修改 Current Weight 以 KG/LBS 保存就切換顯示）
     */
    fun setPreferredUnitAfterSave(unit: UserProfileStore.WeightUnit) {
        viewModelScope.launch { store.setWeightUnit(unit) }
    }
}
