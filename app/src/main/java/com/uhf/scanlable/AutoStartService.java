package com.uhf.scanlable;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class AutoStartService extends Service {
    private static final String CHANNEL_ID = "AutoStartServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification());
        Log.d("AutoStartService", "前台服务已启动...");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("AutoStartService", "准备启动 MainActivity...");

        // 启动 MainActivity
        Intent activityIntent = new Intent(this, SelectActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(activityIntent);

        stopSelf(); // 任务完成后停止服务
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Auto Start Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AutoStart Service")
                .setContentText("应用正在自动启动...")
                .build();
    }
}
