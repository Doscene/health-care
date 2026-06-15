package com.healthcare.family.receiver;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.healthcare.family.MainActivity;

import cn.jpush.android.api.NotificationMessage;
import cn.jpush.android.service.JPushMessageReceiver;

public class JMessageReceiver extends JPushMessageReceiver {


    // 通知栏消息到达（系统已展示）
    @Override
    public void onNotifyMessageArrived(Context context, NotificationMessage message) {
        // 可在此记录日志或触发埋点
        Log.d("JPush", "通知到达: " + message.notificationTitle);
    }

    // 用户点击通知栏
    @Override
    public void onNotifyMessageOpened(Context context, NotificationMessage message) {
        // 跳转指定页面（需处理Activity启动模式）
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("extra", message.notificationContent);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

//    // 透传消息到达（不展示通知栏，需自行处理UI）
//    @Override
//    public void onMessageReceived(Context context, CustomMessage message) {
//        // 解析自定义字段（如JSON格式）
//        String extra = message.extra;
//        // 执行业务逻辑（如刷新数据、弹窗提示）
//    }
}
