package com.calai.app.ui.home.ui.personal.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.api.UserProfileDto
import com.calai.app.data.profile.repo.ProfileRepository
import com.calai.app.data.users.repo.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PersonalViewModel @Inject constructor(
    private val usersRepo: UsersRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val name: String? = null,
        val pictureUrl: String? = null,
        val profile: UserProfileDto? = null,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }

        try {
            // ✅ 關鍵：supervisorScope 讓其中一個 async 爆掉不會連坐另一個
            val (me, profile) = supervisorScope {
                val meDeferred = async { usersRepo.meOrNull() }  // 你這個本來就安全
                val profileDeferred = async {
                    // ★ 就算你的 getServerProfileOrNull 其實「會丟例外」，也不會炸掉 app
                    runCatching { profileRepo.getServerProfileOrNull() }.getOrNull()
                }
                meDeferred.await() to profileDeferred.await()
            }

            Log.d("PersonalVM", "me.name=${me?.name}, me.picture=${me?.picture}, profile.age=${profile?.age}")

            _ui.update {
                it.copy(
                    loading = false,
                    name = me?.name,
                    pictureUrl = me?.picture,
                    profile = profile,
                    error = null
                )
            }
        } catch (t: Throwable) {
            Log.e("PersonalVM", "refresh failed", t)
            _ui.update {
                it.copy(
                    loading = false,
                    error = t.message ?: t.javaClass.simpleName
                )
            }
        }
    }
}
