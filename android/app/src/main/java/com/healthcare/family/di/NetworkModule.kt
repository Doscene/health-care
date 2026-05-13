package com.healthcare.family.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 网络层 Hilt 模块。
 * Phase 0 先创建骨架，Phase 1 填充 Retrofit/OkHttp 实例。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // TODO: Phase 1 提供 Retrofit 实例
}
