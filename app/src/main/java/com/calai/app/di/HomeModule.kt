package com.calai.app.di

import com.calai.app.data.meals.api.MealApi
import com.calai.app.data.meals.repo.MealRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeModule {

    @Provides
    @Singleton
    fun provideMealApi(
        @Named("apiRetrofit") retrofit: Retrofit   // ★ 關鍵：使用你 NetworkModule 提供的具名 Retrofit
    ): MealApi = retrofit.create(MealApi::class.java)

    @Provides
    @Singleton
    fun provideMealRepo(api: MealApi): MealRepository = MealRepository(api)
}

