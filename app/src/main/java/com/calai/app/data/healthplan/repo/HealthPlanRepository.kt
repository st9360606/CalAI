package com.calai.app.data.healthplan.repo

import com.calai.app.data.healthplan.api.HealthPlanApi
import com.calai.app.data.healthplan.api.SaveHealthPlanRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthPlanRepository @Inject constructor(
    private val api: HealthPlanApi,
    private val pending: PendingHealthPlanStore
) {
    /**
     * ✅ 先嘗試存到後端；失敗(含 401/網路錯)就降級存 pending，流程不中斷。
     */
    suspend fun upsertBestEffort(req: SaveHealthPlanRequest) {
        runCatching {
            api.upsert(req)
        }.onSuccess {
            // 成功就清掉 pending（避免重送）
            runCatching { pending.clearPending() }
        }.onFailure {
            // 失敗就存 pending
            runCatching { pending.setPending(req) }
        }
    }

    /**
     * ✅ 有 pending 就補送一次；成功後清掉。
     * 失敗就吞掉（下次再送），不影響登入/跳頁。
     */
    suspend fun flushPendingBestEffort() {
        val req = pending.getPending() ?: return
        runCatching { api.upsert(req) }
            .onSuccess { runCatching { pending.clearPending() } }
    }
}
