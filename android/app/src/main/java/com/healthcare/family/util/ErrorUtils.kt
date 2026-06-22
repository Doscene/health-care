package com.healthcare.family.util

import org.json.JSONObject
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Exception.toUserFriendlyMessage(): String {
  return when (this) {
    is HttpException -> {
      try {
        val errorBody = response()?.errorBody()?.string()
        if (errorBody != null) {
          val json = JSONObject(errorBody)
          val msg = json.optString("message", null)
          if (!msg.isNullOrBlank()) msg else "操作失败，请稍后重试"
        } else {
          when (code()) {
            400 -> "请求参数有误，请检查输入"
            401 -> "登录已过期，请重新登录"
            403 -> "没有权限执行此操作"
            404 -> "请求的资源不存在"
            500 -> "服务器内部错误，请稍后重试"
            else -> "操作失败（错误码：${code()}）"
          }
        }
      } catch (_: Exception) {
        "操作失败，请稍后重试"
      }
    }
    is UnknownHostException, is ConnectException -> "网络连接失败，请检查网络设置"
    is SocketTimeoutException -> "请求超时，请稍后重试"
    else -> this.message ?: "操作失败，请稍后重试"
  }
}
