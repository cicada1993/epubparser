package com.chineseall.epubparser.lib.downloader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import com.chineseall.epubparser.lib.R;
import com.chineseall.epubparser.lib.util.AppUtil;

import java.io.File;

class DldNotifier {
    private static volatile DldNotifier instance;
    private NotificationManager notificationManager;
    private Context context;

    private DldNotifier() {
        this.context = DldManager.context;
        this.notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
    }

    public static DldNotifier get() {
        if (instance == null) {
            synchronized (DldNotifier.class) {
                if (instance == null) {
                    instance = new DldNotifier();
                }
            }
        }
        return instance;
    }

    /**
     * 显示通知
     *
     * @param taskId       任务id
     * @param fileName     文件名
     * @param progress     下载进度
     * @param state        下载状态描述
     * @param isApkInstall apk自动安装
     */
    public void show(int taskId, String fileName, float progress, String state, boolean isApkInstall, String filePath) {
        Notification.Builder builder = new Notification.Builder(context);
        NotificationChannel channel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = String.valueOf(taskId);
            channel = new NotificationChannel(channelId, "download", NotificationManager.IMPORTANCE_HIGH);
            channel.setVibrationPattern(new long[]{0});
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }
        builder.setContentTitle(fileName);
        builder.setProgress(100, (int) progress, false);
        builder.setSmallIcon(R.mipmap.ic_notification);
        builder.setAutoCancel(true);
        builder.setVibrate(new long[]{0});
        builder.setSound(null);
        if (isApkInstall && progress >= 100) {
            builder.setContentIntent(
                    PendingIntent.getActivity(
                            context,
                            0,
                            AppUtil.getInstallIntent(context, new File(filePath)),
                            0
                    )
            );
            builder.setContentText(state + "，点击安装");
        } else {
            builder.setContentText(state);
        }
        notificationManager.notify(taskId, builder.build());
    }

    /**
     * 从通知栏移除
     *
     * @param taskId
     */
    public void remove(int taskId) {
        notificationManager.cancel(taskId);
    }
}
