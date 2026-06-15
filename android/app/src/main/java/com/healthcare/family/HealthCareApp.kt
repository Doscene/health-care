package com.healthcare.family

import android.app.Application
import cn.jcore.client.android.BuildConfig
import cn.jpush.android.api.JPushInterface
import com.healthcare.family.data.repository.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class HealthCareApp : Application() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate() {
        super.onCreate()
        initJPush()
        // 同步恢复 Token，确保 UI 渲染前 AuthInterceptor 已有 token
        runBlocking {
            authRepository.restoreToken()
        }
    }

    private fun initJPush() {
        JPushInterface.setDebugMode(BuildConfig.DEBUG)
        JPushInterface.init(this)
    }
}
