package com.hty.LocusMapUCMap;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class NotificationUtil {

	public static final int NOTIFICATION_ID = 1;
	private Context ctx;
	private NotificationManager notificationManager;
	private Notification notification;

	public Notification getNotification() {
		return notification;
	}

	private PendingIntent pendingIntent;

	public NotificationUtil(Context context, int icon, String title, String message, String channelId, String channelName) {
		this.ctx = context;
		notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			// importance: 通知重要性
			// IMPORTANCE_HIGH：紧急级别（发出通知声音并显示为提示通知）
			// IMPORTANCE_DEFAULT：高级别（发出通知声音并且通知栏有通知）
			// IMPORTANCE_LOW：中等级别（没有通知声音但通知栏有通知）
			// IMPORTANCE_MIN：低级别（没有通知声音也不会出现在状态栏）
			NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
			// 是否绕过请勿打扰模式
			channel.canBypassDnd();
			// 设置可绕过请勿打扰模式
			channel.setBypassDnd(true);

			// 锁屏显示通知
			channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
			// 桌面launcher的消息角标
			channel.canShowBadge();
			// 获取通知取到组
			channel.getGroup();

			// //闪光灯
			// channel.enableLights(true);
			// //闪关灯的灯光颜色
			// channel.setLightColor(Color.RED);
			// //是否会有灯光
			// channel.shouldShowLights();
			//
			// //是否允许震动
			// channel.enableVibration(true);
			// //获取系统通知响铃声音的配置
			// channel.getAudioAttributes();
			// //设置震动模式
			// channel.setVibrationPattern(new long[]{100, 100, 200});

			notificationManager.createNotificationChannel(channel);
		}

		// 定义Notification的各种属性
		Notification.Builder builder = new Notification.Builder(context).setContentTitle(title).setContentText(message).setSmallIcon(icon).setAutoCancel(true).setOngoing(true);

		// 设置通知的事件消息
		Intent notificationIntent = new Intent(ctx, ctx.getClass());
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		pendingIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);
		if (Build.VERSION.SDK_INT >= 26) {
			//解决 Bad notifacation forground
			builder.setChannelId(channelId);
		}
		notification = builder.build();
	}

	public void showNotification() {
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	public void cancelNotification() {
		notificationManager.cancel(NOTIFICATION_ID);
	}

}
