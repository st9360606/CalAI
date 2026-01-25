package com.calai.app.data.entitlement

import com.calai.app.data.billing.BillingGateway
import com.calai.app.data.entitlement.api.EntitlementApi
import com.calai.app.data.entitlement.api.EntitlementSyncRequest
import com.calai.app.data.entitlement.api.PurchaseTokenPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EntitlementSyncer(
    private val billing: BillingGateway,
    private val api: EntitlementApi,
) {
    /**
     * ✅ 不阻塞登入流程：失敗就吞掉（你不想有 Restore 按鈕，也不想擋登入）
     */
    suspend fun syncAfterLoginSilently() = withContext(Dispatchers.IO) {
        runCatching {
            // 1. 取得 Google Play 當前的所有有效收據
            val subs = billing.queryActiveSubscriptions()
            if (subs.isEmpty()) return@runCatching

            // 2. 轉換格式
            val payload = subs.map { PurchaseTokenPayload(it.productId, it.purchaseToken) }

            // 3. 送往後端驗證
            val response = api.sync(EntitlementSyncRequest(purchases = payload))

            // 4. (選做) 成功後將後端回傳的權限狀態更新到本地資料庫
            // entitlementDao.update(response.permissions)
        }.onFailure { e ->
            // 僅紀錄 Log，不丟出 Exception 以免崩潰或擋住 UI
            // Log.e("EntitlementSyncer", "Silent sync failed", e)
        }
    }
}
