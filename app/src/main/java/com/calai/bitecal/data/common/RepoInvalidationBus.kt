package com.calai.bitecal.data.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全域 cache-invalidate bus：
 * - 任何「寫入成功」的地方 emit invalidate
 * - ViewModel 收到後做 force refresh（不被 throttle 擋）
 */
@Singleton
class RepoInvalidationBus @Inject constructor() {

    private val _profile = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val profile: SharedFlow<Unit> = _profile

    private val _weight = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val weight: SharedFlow<Unit> = _weight

    fun invalidateProfile() {
        _profile.tryEmit(Unit)
    }

    fun invalidateWeight() {
        _weight.tryEmit(Unit)
    }
}
