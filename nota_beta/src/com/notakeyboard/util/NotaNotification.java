package com.notakeyboard.util;

import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.Context;

import com.notakeyboard.R;

public class NotaNotification {
	public static NotificationManager NotifyManager;
	public static Builder NotiBuilder;
	public static final int TRAIN_NOTI_ID = 1;

	public static void notiInitialize(Context context) {
		NotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		NotiBuilder = new Builder(context);
		NotiBuilder.setContentTitle(context.getString(R.string.NOTAKeyboard));
		NotiBuilder.setSmallIcon(R.drawable.icon);
	}
}
