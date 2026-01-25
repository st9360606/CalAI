package com.calai.app.di

import android.app.Application
import android.content.Context
import com.calai.app.data.billing.BillingGateway
import com.calai.app.data.billing.PlayBillingGateway
import com.calai.app.data.entitlement.EntitlementSyncer
import com.calai.app.data.entitlement.api.EntitlementApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EntitlementModule {

    @Provides
    @Singleton
    fun provideBillingGateway(@ApplicationContext ctx: Context): BillingGateway {
        val app = ctx.applicationContext as Application
        return PlayBillingGateway(app)
    }

    @Provides
    @Singleton
    fun provideEntitlementSyncer(
        billing: BillingGateway,
        api: EntitlementApi
    ): EntitlementSyncer = EntitlementSyncer(billing, api)
}
