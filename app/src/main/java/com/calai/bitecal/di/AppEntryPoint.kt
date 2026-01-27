package com.calai.bitecal.di

import com.calai.bitecal.data.account.repo.AccountRepository
import com.calai.bitecal.data.auth.repo.AuthRepository
import com.calai.bitecal.data.auth.repo.TokenStore
import com.calai.bitecal.data.auth.state.AuthState
import com.calai.bitecal.data.entitlement.EntitlementSyncer
import com.calai.bitecal.data.foodlog.repo.FoodLogsRepository
import com.calai.bitecal.data.profile.repo.AutoGoalsRepository
import com.calai.bitecal.data.profile.repo.UserProfileStore
import com.calai.bitecal.data.profile.repo.ProfileRepository
import com.calai.bitecal.data.weight.repo.WeightRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 讓非 ViewModel / 非 @AndroidEntryPoint 的類別，也能從 Hilt 取出單例。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun authRepository(): AuthRepository
    fun authState(): AuthState
    fun tokenStore(): TokenStore
    fun profileRepository(): ProfileRepository
    fun userProfileStore(): UserProfileStore
    fun weightRepository(): WeightRepository
    fun autoGoalsRepository(): AutoGoalsRepository
    fun foodLogsRepository(): FoodLogsRepository
    fun accountRepository(): AccountRepository
    fun entitlementSyncer(): EntitlementSyncer
}
