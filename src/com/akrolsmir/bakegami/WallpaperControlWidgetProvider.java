package com.akrolsmir.bakegami;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

public class WallpaperControlWidgetProvider extends AppWidgetProvider {

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d("TAG", "onUpdate");
		context.startService(new Intent(context, NextWallpaperService.class));

		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int appWidgetId : appWidgetIds) {

			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.wallpaper_control_widget);

			// Create an Intent to start the Service
			// Get the layout for the App Widget and attach an on-click listener
			// to the button
			Intent intent = new Intent(context, NextWallpaperService.class);
			PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
			views.setOnClickPendingIntent(R.id.skipButton, pendingIntent);

			intent = new Intent(context, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
			views.setOnClickPendingIntent(R.id.manageButton, pendingIntent);

			intent = new Intent(context, FavoriteWallpaperService.class);
			pendingIntent = PendingIntent.getService(context, 0, intent, 0);
			views.setOnClickPendingIntent(R.id.favButton, pendingIntent);

			// Tell the AppWidgetManager to perform an update on the current app widget
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

	public static class FavoriteWallpaperService extends IntentService {

		public FavoriteWallpaperService() {
			super("FavoriteWallpaperService");
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			new WallpaperManager(this).getCurrentWallpaper().favorite();
		}
	}

	public static class NextWallpaperService extends IntentService {

		public NextWallpaperService() {
			super("NextWallpaperService");
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			Log.d("Changing wallpaper", "...");
			new WallpaperManager(this).nextWallpaper();
		}
	}

	public static class RefreshService extends IntentService {
		
		public RefreshService() {
			super("RefreshService");
		}

		@Override
		protected void onHandleIntent(Intent intent) {

			Intent intent2 = new Intent(this, NextWallpaperService.class);
			PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent2, 0);

			AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

			long period = getRefreshTime() * 1000;
			alarmManager.cancel(pendingIntent); //Cancels any past refresh

			Log.d("intent.getAction()", intent.getAction());
			if (intent.getAction().equals("boot")) {
				alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,
						SystemClock.elapsedRealtime() + period / 2, period, pendingIntent);
			} else if (intent.getAction().equals("stop")) {
				// do nothing
			} else if (intent.getAction().equals("start")) {
				alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 0, period, pendingIntent);
			}

		}

		private long getRefreshTime() {
			return getSharedPreferences("com.akrolsmir.bakegami", 0).getLong("refreshTime", 20);
		}
	}

	// Restart alarm on boot
	public static class BootBroadcastReceiver extends BroadcastReceiver {
		
		public BootBroadcastReceiver() {
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			context.startService(new Intent(context, RefreshService.class).setAction("boot"));
		}
	}
}
