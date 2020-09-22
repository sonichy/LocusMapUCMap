//解决后台无定位问题：https://blog.csdn.net/doris_d/article/details/102854998

package com.hty.LocusMapUCMap;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class ServiceNotification extends Service {

	public static String EXTRA_NOTIFICATION_CONTENT = "notification_content";
	private String CHANNEL_ID = this.getClass().getName();
	private String CHANNEL_NAME = "Default Channel";

	private NotificationUtil notificationUtil;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			return START_NOT_STICKY;
		}
		String content = intent.getStringExtra(EXTRA_NOTIFICATION_CONTENT);
		notificationUtil = new NotificationUtil(MainApplication.context, R.drawable.ic_launcher, getString(R.string.app_name), content, CHANNEL_ID, CHANNEL_NAME);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForeground(NotificationUtil.NOTIFICATION_ID, notificationUtil.getNotification());
		} else {
			notificationUtil.showNotification();
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (notificationUtil != null) {
			notificationUtil.cancelNotification();
			notificationUtil = null;
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}