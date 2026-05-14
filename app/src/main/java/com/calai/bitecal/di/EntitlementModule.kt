package com.calai.bitecal.di

import android.app.Application
import com.calai.bitecal.data.billing.BillingGateway
import com.calai.bitecal.data.billing.FakeBillingGateway
import com.calai.bitecal.data.billing.PlayBillingGateway
import com.calai.bitecal.data.entitlement.EntitlementSyncer
import com.calai.bitecal.data.entitlement.api.EntitlementApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EntitlementModule {

    @Provides
    @Singleton
    fun provideBillingGateway(
        app: Application
    ): BillingGateway {
        val packageName = app.packageName

        val useFakeBilling =
            packageName.endsWith(".dev") ||
                    packageName.endsWith(".devwifi") ||
                    packageName.endsWith(".devusb")

        return if (useFakeBilling) {
            FakeBillingGateway()
        } else {
            PlayBillingGateway(app)
        }
    }

    @Provides
    @Singleton
    fun provideEntitlementSyncer(
        billing: BillingGateway,
        api: EntitlementApi
    ): EntitlementSyncer {
        return EntitlementSyncer(
            billing = billing,
            api = api
        )
    }
}
