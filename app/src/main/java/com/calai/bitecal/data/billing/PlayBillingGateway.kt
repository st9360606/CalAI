package com.calai.bitecal.data.billing

import android.app.Activity
import android.app.Application
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicReference

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlayBillingGateway(
    private val app: Application
) : BillingGateway {

    private companion object {
        const val TAG = "PlayBillingGateway"
    }

    private val pendingPurchaseResult =
        AtomicReference<CompletableDeferred<BillingPurchaseResult>?>(null)

    private val client: BillingClient by lazy {
        BillingClient.newBuilder(app)
            .setListener { billingResult: BillingResult, purchases: MutableList<Purchase>? ->
                handlePurchaseUpdated(billingResult, purchases)
            }
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
        if (!ok) return emptyList()

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val (billingResult, purchases) = queryPurchasesAsyncSuspend(params)

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(
                TAG,
                "queryActiveSubscriptions failed. code=${billingResult.responseCode}, msg=${billingResult.debugMessage}"
            )
            return emptyList()
        }

        return purchases
            .asSequence()
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .flatMap { purchase ->
                val token = purchase.purchaseToken
                purchase.products.asSequence().map { productId ->
                    ActiveSub(
                        productId = productId,
                        purchaseToken = token,
                        acknowledged = purchase.isAcknowledged
                    )
                }
            }
            .toList()
    }

    override suspend fun launchSubscriptionPurchase(
        activity: Activity,
        productId: String,
        offerTag: String?
    ): BillingPurchaseResult {
        val ok = startConnectionIfNeeded()
        if (!ok) {
            return BillingPurchaseResult.Error("Billing service is not ready")
        }

        Log.d(
            TAG,
            "launchSubscriptionPurchase package=${app.packageName}, productId=$productId, offerTag=$offerTag"
        )

        val productDetails = querySubscriptionProductDetails(productId)
            ?: return BillingPurchaseResult.Error(
                "Subscription product not found. package=${app.packageName}, productId=$productId. " +
                        "請確認你正在跑 prodDebug，且 Play Console 有建立這個 subscription product。"
            )

        val offerDetails = productDetails.subscriptionOfferDetails.orEmpty()

        Log.d(
            TAG,
            "available offers for productId=$productId => ${describeOffers(offerDetails)}"
        )

        val matchedOffer = pickOffer(
            offerDetails = offerDetails,
            requestedOfferTag = offerTag
        ) ?: return BillingPurchaseResult.Error(
            "No subscription offer found. package=${app.packageName}, productId=$productId, requestedOfferTag=$offerTag, " +
                    "available=${describeOffers(offerDetails)}"
        )

        val offerToken = matchedOffer.offerToken

        val deferred = CompletableDeferred<BillingPurchaseResult>()

        if (!pendingPurchaseResult.compareAndSet(null, deferred)) {
            return BillingPurchaseResult.Error("Another purchase is already in progress")
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val launchResult = client.launchBillingFlow(activity, flowParams)

        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchaseResult.compareAndSet(deferred, null)

            return BillingPurchaseResult.Error(
                "Failed to launch billing flow. code=${launchResult.responseCode}, msg=${launchResult.debugMessage}"
            )
        }

        return deferred.await()
    }

    override suspend fun acknowledgePurchase(purchaseToken: String): Boolean {
        val ok = startConnectionIfNeeded()
        if (!ok) return false

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        return suspendCancellableCoroutine { cont ->
            client.acknowledgePurchase(params) { result ->
                if (!cont.isActive) return@acknowledgePurchase
                cont.resume(
                    result.responseCode == BillingClient.BillingResponseCode.OK
                ) { _ -> }
            }
        }
    }

    private fun pickOffer(
        offerDetails: List<ProductDetails.SubscriptionOfferDetails>,
        requestedOfferTag: String?
    ): ProductDetails.SubscriptionOfferDetails? {
        if (offerDetails.isEmpty()) return null

        /**
         * 圖2：原價訂閱 sheet
         *
         * 不指定 offerTag 時，優先挑 regular base plan。
         * regular base plan 的 offerId 通常是 null。
         */
        if (requestedOfferTag.isNullOrBlank()) {
            return offerDetails.firstOrNull { details ->
                details.offerId.isNullOrBlank()
            } ?: offerDetails.firstOrNull()
        }

        /**
         * 圖6：discount / trial offer
         *
         * Play Console 可能用：
         * - offerTags
         * - offerId
         * - basePlanId
         *
         * 所以三種都支援，避免只靠 offerTags 找不到。
         */
        return offerDetails.firstOrNull { details ->
            details.offerTags.contains(requestedOfferTag) ||
                    details.offerId == requestedOfferTag ||
                    details.basePlanId == requestedOfferTag
        }
    }

    private fun describeOffers(
        offerDetails: List<ProductDetails.SubscriptionOfferDetails>
    ): String {
        if (offerDetails.isEmpty()) return "[]"

        return offerDetails.joinToString(
            prefix = "[",
            postfix = "]",
            separator = " | "
        ) { details ->
            "basePlanId=${details.basePlanId}, offerId=${details.offerId}, tags=${details.offerTags}"
        }
    }

    private fun handlePurchaseUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        val deferred = pendingPurchaseResult.getAndSet(null) ?: return

        Log.d(
            TAG,
            "purchase updated. code=${billingResult.responseCode}, msg=${billingResult.debugMessage}, purchases=${purchases?.size ?: 0}"
        )

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchased = purchases
                    ?.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }

                if (purchased != null) {
                    val productId = purchased.products.firstOrNull()

                    if (productId.isNullOrBlank()) {
                        deferred.complete(
                            BillingPurchaseResult.Error("Purchased product is empty")
                        )
                        return
                    }

                    deferred.complete(
                        BillingPurchaseResult.Success(
                            ActiveSub(
                                productId = productId,
                                purchaseToken = purchased.purchaseToken,
                                acknowledged = purchased.isAcknowledged
                            )
                        )
                    )
                    return
                }

                val pending = purchases
                    ?.any { it.purchaseState == Purchase.PurchaseState.PENDING }
                    ?: false

                if (pending) {
                    deferred.complete(BillingPurchaseResult.Pending)
                } else {
                    deferred.complete(
                        BillingPurchaseResult.Error("No purchased subscription returned")
                    )
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                deferred.complete(BillingPurchaseResult.Cancelled)
            }

            else -> {
                deferred.complete(
                    BillingPurchaseResult.Error(
                        billingResult.debugMessage.ifBlank {
                            "Billing failed: ${billingResult.responseCode}"
                        }
                    )
                )
            }
        }
    }

    private suspend fun querySubscriptionProductDetails(productId: String): ProductDetails? {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        val resultPair =
            suspendCancellableCoroutine<Pair<BillingResult, List<ProductDetails>>> { cont ->
                client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    if (!cont.isActive) return@queryProductDetailsAsync
                    cont.resume(
                        billingResult to productDetailsList
                    ) { _ -> }
                }
            }

        val billingResult = resultPair.first
        val products = resultPair.second

        Log.d(
            TAG,
            "queryProductDetails productId=$productId, package=${app.packageName}, code=${billingResult.responseCode}, msg=${billingResult.debugMessage}, count=${products.size}"
        )

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return null
        }

        return products.firstOrNull { it.productId == productId }
    }

    private suspend fun startConnectionIfNeeded(): Boolean {
        if (client.isReady) return true

        return suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (!cont.isActive) return

                    Log.d(
                        TAG,
                        "Billing setup finished. code=${result.responseCode}, msg=${result.debugMessage}"
                    )

                    cont.resume(
                        result.responseCode == BillingClient.BillingResponseCode.OK
                    ) { _ -> }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "Billing service disconnected")
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
            client.queryPurchasesAsync(params) { result, list ->
                if (!cont.isActive) return@queryPurchasesAsync
                cont.resume(result to (list ?: emptyList())) { _ -> }
            }
        }
    }
}
