// app/src/main/java/com/calai/app/ui/onboarding/age/AgeSelectionViewModel.kt
package com.calai.app.ui.onboarding.age

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgeSelectionViewModel @Inject constructor(
    private val usr: UserProfileStore
) : ViewModel() {

    // 直接給預設 25 歲，UI 不用處理 null
    val ageState = usr.ageFlow
        .map { it ?: 25 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 25)

    fun saveAge(years: Int) {
        viewModelScope.launch { usr.setAge(years) }
    }
}
