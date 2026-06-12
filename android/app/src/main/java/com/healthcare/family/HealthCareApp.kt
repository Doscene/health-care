package com.healthcare.family

import android.app.Application
import cn.jpush.android.api.JPushInterface
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HealthCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initJPush()
    }

    private fun initJPush() {
        JPushInterface.setDebugMode(BuildConfig.DEBUG)
        JPushInterface.init(this)
    }
}
