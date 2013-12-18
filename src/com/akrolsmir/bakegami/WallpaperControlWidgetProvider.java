package com.akrolsmir.bakegami;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

public class WallpaperControlWidgetProvider extends AppWidgetProvider {

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d("TAG", "onUpdate");
		context.startService(new Intent(context, ExampleIntentService.class));
		final int N = appWidgetIds.length;

		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];

			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.wallpaper_control_widget);

			// Create an Intent to start the Service
			// Get the layout for the App Widget and attach an on-click listener
			// to the button
			Intent intent = new Intent(context, ExampleIntentService.class);
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
			new WallpaperManager(this).favoriteWallpaper();
		}

	}

	public static class ExampleIntentService extends IntentService {
		SharedPreferences settings;

		public ExampleIntentService() {
			super("ExampleIntentService");
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			settings = getSharedPreferences("com.akrolsmir.bakegami", 0);
			Log.d("Changing wallpaper", "...");
			new WallpaperManager(this).nextWallpaper();
			//			if(intent.getBooleanExtra("changeWallpaper", true))
			//			Wallpaper wallpaper = new Wallpaper(this, settings.getString("next", "http://cdn.awwni.me/maav.jpg"));
			//			wallpaper.setAsBackground();
			//			wallpaper.favorite();
			//			fetchNextUrl();
		}

		//		private void fetchNextUrl() {
		//			new Thread(new Runnable() {
		//
		//				@Override
		//				public void run() {
		//					RestTemplate restTemplate = new RestTemplate();
		//					restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
		//					String rawJson = restTemplate.getForObject("http://www.reddit.com/r/"
		//							+ getSubreddit() + "/top.json?t=week", String.class);
		//					String nextUrl = parseUrlFromReddit(rawJson);
		//					Log.d("next", nextUrl);
		//					settings.edit().putString("next", nextUrl).commit();
		//				}
		//			}).start();
		//		}
		//
		//		private String getSubreddit() {
		//			return settings.getString("subreddit", "awwnime");
		//		}
		//
		//		private String parseUrlFromReddit(String rawJson) {
		//			JsonElement object = new JsonParser().parse(rawJson);
		//			JsonArray children = object.getAsJsonObject().get("data").getAsJsonObject().get(
		//					"children").getAsJsonArray();
		//			for (JsonElement child : children) {
		//				String url = child.getAsJsonObject().get("data").getAsJsonObject().get("url").getAsString();
		//				if (validImageUrl(url))
		//					return url;
		//			}
		//			// Placeholder. TODO remove
		//			return "http://i.imgur.com/iCQxSJZ.jpg";
		//		}
		//
		//		// Returns true if url ends in .jpg/.png and not used before
		//		private boolean validImageUrl(String imageUrl) {
		//			return imageUrl.matches("http://.*\\.(jpg|png)$") && unusedUrl(imageUrl);
		//		}
		//
		//		//TODO replace with a more reliable history
		//		private boolean unusedUrl(String imageUrl) {
		//			String history = settings.getString("history", "");
		//			//Log.d("history", history);
		//			boolean usedUrl = history.contains(imageUrl);
		//			settings.edit().putString("history", history + imageUrl).commit();
		//			return !usedUrl;
		//		}
	}

	public static class RefreshService extends IntentService {

		public RefreshService() {
			super("RefreshService");
		}

		@Override
		protected void onHandleIntent(Intent intent) {

			Intent intent2 = new Intent(this, ExampleIntentService.class);
			PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent2, 0);

			AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			alarmManager.cancel(pendingIntent); //Cancels any past refresh

			if (!("stop").equals(intent.getAction()))
				alarmManager.setInexactRepeating(AlarmManager.RTC, 0, getRefreshTime() * 1000,
						pendingIntent);
		}

		private long getRefreshTime() {
			return getSharedPreferences("com.akrolsmir.bakegami", 0).getLong("refreshTime", 20);
		}
	}

	public class BootBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			context.startService(new Intent(context, RefreshService.class));
		}
	}

	//	public static class ChangeBackgroundService extends IntentService {
	//
	//		public ChangeBackgroundService(String name) {
	//			super("ChangeBackgroundService");
	//		}
	//
	//		@Override
	//		public void onHandleIntent(Intent intent) {
	//			Log.d("TAG", "onHandleIntent");
	//
	//			ComponentName me = new ComponentName(this, WallpaperControlWidgetProvider.class);
	//			AppWidgetManager mgr = AppWidgetManager.getInstance(this);
	//
	//			mgr.updateAppWidget(me, buildUpdate(this));
	//		}
	//
	//		@Override
	//		public void onCreate() {
	//			Log.d("TAG", "onCreate");
	//			super.onCreate();
	//
	//		}
	//
	//		private RemoteViews buildUpdate(Context context) {
	//			RemoteViews updateViews = new RemoteViews(context.getPackageName(),
	//					R.layout.wallpaper_control_widget);
	//
	//			Intent i = new Intent(this, WallpaperControlWidgetProvider.class);
	//			PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
	//			updateViews.setOnClickPendingIntent(R.id.button, pi);
	//
	//			return (updateViews);
	//		}
	//
	//	}
}
