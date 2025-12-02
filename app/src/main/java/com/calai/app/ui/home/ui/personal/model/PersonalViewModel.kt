package com.calai.app.ui.home.ui.personal.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.api.UserProfileDto
import com.calai.app.data.profile.repo.ProfileRepository
import com.calai.app.data.users.repo.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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

    /** 原本全量刷新：Users(me) + Profile */
    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }

        try {
            val (me, profile) = supervisorScope {
                val meDeferred = async { usersRepo.meOrNull() }
                val profileDeferred = async {
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

    /**
     * ✅ NEW：只刷新 Profile（PersonalDetails 修改身高/年齡/性別…用這個）
     * - 不動 name/picture（避免 UI 抖動）
     * - loading 只表現在同一顆 state（你要不要用 loading overlay 自行決定）
     */
    fun refreshProfileOnly() = viewModelScope.launch {
        // 你如果不想畫面出現 loading，就把這行拿掉
        _ui.update { it.copy(loading = true, error = null) }

        val profile = runCatching { profileRepo.getServerProfileOrNull() }.getOrNull()

        _ui.update { cur ->
            cur.copy(
                loading = false,
                profile = profile ?: cur.profile, // 取不到就保留舊的
                error = null
            )
        }
    }

    /**
     * ✅ NEW（可選）：用「本機 DataStore 快照」先讓 UI 立即更新
     * 適合：EditHeight 按下 Continue -> 已存本機，但網路同步還沒回來 / 失敗
     */
    fun applyLocalProfileSnapshot(snapshot: UserProfileDto) {
        _ui.update { it.copy(profile = snapshot) }
    }
}
