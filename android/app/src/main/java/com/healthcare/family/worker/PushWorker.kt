package com.healthcare.family.worker

import android.content.Context
import cn.jpush.android.api.JPushInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushWorker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getRegistrationId(): String? {
        return JPushInterface.getRegistrationID(context)
    }

    fun onResume() {
        JPushInterface.onResume(context)
    }

    fun onPause() {
        JPushInterface.onPause(context)
    }
}
