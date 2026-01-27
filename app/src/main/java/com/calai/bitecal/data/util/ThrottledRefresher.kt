package com.calai.bitecal.data.util

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * 節流執行器：
 * - N ms 內重複呼叫 -> 不執行（除非 force=true）
 * - 同一時間只允許一個 in-flight job
 * - 只有 block 成功完成才會更新 lastSuccessAt
 */
class ThrottledRefresher(
    private val minIntervalMs: Long
) {
    private val lastSuccessAtMs = AtomicLong(0L)
    @Volatile private var job: Job? = null

    fun launch(
        scope: CoroutineScope,
        force: Boolean = false,
        block: suspend () -> Unit
    ) {
        val now = SystemClock.elapsedRealtime()
        val last = lastSuccessAtMs.get()
        val tooSoon = (now - last) < minIntervalMs

        if (!force && tooSoon) return
        if (job?.isActive == true) return

        job = scope.launch {
            // block 自己負責 error state；若 throw 就不更新 lastSuccessAt
            block()
            lastSuccessAtMs.set(SystemClock.elapsedRealtime())
        }
    }

    /** 讓下一次 call 直接通過節流（通常用在 invalidate 時） */
    fun invalidate() {
        lastSuccessAtMs.set(0L)
    }
}
