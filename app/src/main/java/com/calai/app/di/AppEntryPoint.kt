package com.calai.app.di

import com.calai.app.data.auth.repo.AuthRepository
import com.calai.app.data.auth.repo.TokenStore
import com.calai.app.data.auth.state.AuthState
import com.calai.app.data.auth.store.UserProfileStore
import com.calai.app.data.profile.ProfileRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 讓非 ViewModel / 非 @AndroidEntryPoint 的類別，也能從 Hilt 取出單例。
 * 我們會在 Composable 裡從 Application 取出 AuthRepository。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun authRepository(): AuthRepository
    fun authState(): AuthState
    fun tokenStore(): TokenStore
    fun profileRepository(): ProfileRepository
    fun userProfileStore(): UserProfileStore       // ← ✅ 新增
}
