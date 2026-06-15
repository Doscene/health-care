package com.healthcare.family.di

import com.healthcare.family.BuildConfig
import com.healthcare.family.data.remote.api.AuthApi
import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.interceptor.AuthInterceptor
import com.healthcare.family.data.remote.interceptor.TokenAuthenticator
import com.healthcare.family.data.remote.interceptor.TokenRefreshProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L

    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor {
        return AuthInterceptor()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideHealthCareApi(
        retrofit: Retrofit,
    ): HealthCareApi {
        return retrofit.create(HealthCareApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApi(
        retrofit: Retrofit,
    ): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }
}

/**
 * 将 AuthRepository 绑定为 TokenRefreshProvider 实现。
 * 分离到独立 Module 避免循环依赖。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindModule {
    @Binds
    @Singleton
    abstract fun bindTokenRefreshProvider(
        impl: com.healthcare.family.data.repository.AuthRepository,
    ): TokenRefreshProvider
}
