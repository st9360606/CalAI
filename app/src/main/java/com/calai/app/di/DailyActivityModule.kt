package com.calai.app.di

import com.calai.app.data.activity.sync.DailyReader
import com.calai.app.data.activity.sync.HealthConnectDailyReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DailyActivityModule {
    @Binds
    @Singleton
    abstract fun bindDailyReader(impl: HealthConnectDailyReader): DailyReader
}