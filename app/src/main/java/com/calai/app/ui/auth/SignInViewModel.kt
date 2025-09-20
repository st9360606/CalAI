// app/src/main/java/com/calai/app/ui/auth/SignInViewModel.kt
package com.calai.app.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.auth.AuthRepository
import com.calai.app.data.auth.GoogleAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    sealed interface Event {
        data object Idle : Event
        data object Loading : Event
        data class Error(val message: String) : Event
        data object SignedIn : Event
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _events.send(Event.Loading)
            try {
                val idToken = GoogleAuthService(context).getIdToken()
                // 呼叫後端換取你們 App 的 session token（若後端尚未完成，先跳過這行）
                val session = repo.loginWithGoogle(idToken) //打完 google ，回傳後端
                // TODO: 保存 session.token 到 DataStore
                _events.send(Event.SignedIn)
            } catch (e: Exception) {
                _events.send(Event.Error(e.message ?: "Google 登入失敗"))
            }
        }
    }
}
