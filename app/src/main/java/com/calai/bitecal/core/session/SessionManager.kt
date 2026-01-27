package com.calai.bitecal.core.session

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@Singleton
class SessionManager @Inject constructor() {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired

    fun onSessionExpired() {
        _expired.tryEmit(Unit)
    }
}
