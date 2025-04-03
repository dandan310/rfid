package com.uhf.scanlable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "设备已启动，准备启动应用...");

            // 启动目标应用的主 Activity
            Intent serviceIntent = new Intent(context, AutoStartService.class);
            context.startForegroundService(serviceIntent);
        }
    }
}
