package com.calai.bitecal.data.billing

import android.app.Application
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlayBillingGateway(
    private val app: Application
) : BillingGateway {

    private val client: BillingClient by lazy {
        BillingClient.newBuilder(app)
            // ✅ 這個 listener 是「購買流程」用；我們現在只做查詢，所以 no-op
            .setListener { _: BillingResult, _: MutableList<Purchase>? -> }
            // ✅ Billing 8.x：必須傳 PendingPurchasesParams
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()
    }

    override suspend fun queryActiveSubscriptions(): List<ActiveSub> {
        val ok = startConnectionIfNeeded()
        if (!ok) {
            println("DEBUG_BILLING: 連線失敗")
            return emptyList()
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val (br, purchases) = queryPurchasesAsyncSuspend(params)

        // ✅ 加入這行觀察抓到了幾個訂閱
        println("DEBUG_BILLING: 抓到 ${purchases.size} 個原始購得項目")

        if (br.responseCode != BillingClient.BillingResponseCode.OK) return emptyList()

        return purchases
            .asSequence()
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .flatMap { p ->
                // ✅ 觀察具體的 ProductId 和 Token
                println("DEBUG_BILLING: 處理中的產品 ID: ${p.products}")
                val token = p.purchaseToken
                p.products.asSequence().map { prodId ->
                    ActiveSub(productId = prodId, purchaseToken = token)
                }
            }
            .toList()
    }

    private suspend fun startConnectionIfNeeded(): Boolean {
        if (client.isReady) return true

        return suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {

                override fun onBillingSetupFinished(result: BillingResult) {
                    if (!cont.isActive) return
                    // ✅ Kotlin2 / coroutines：resume 要提供 onCancellation
                    cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK) { _ -> }
                }

                override fun onBillingServiceDisconnected() {
                    if (!cont.isActive) return
                    cont.resume(false) { _ -> }
                }
            })
        }
    }

    private suspend fun queryPurchasesAsyncSuspend(
        params: QueryPurchasesParams
    ): Pair<BillingResult, List<Purchase>> {
        return suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(params) { br, list ->
                if (!cont.isActive) return@queryPurchasesAsync
                cont.resume(br to (list ?: emptyList())) { _ -> }
            }
        }
    }
}
