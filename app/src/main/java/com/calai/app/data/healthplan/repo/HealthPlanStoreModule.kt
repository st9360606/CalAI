package com.calai.app.data.healthplan.repo

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthPlanStoreModule {
    @Binds
    @Singleton
    abstract fun bindPendingHealthPlanStore(
        impl: HealthPlanLocalStore
    ): PendingHealthPlanStore
}
