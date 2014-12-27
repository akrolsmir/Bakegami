package com.akrolsmir.bakegami;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.akrolsmir.bakegami.settings.SettingsActivity;

/**
 * Gets notified when the phone connects to the internet
 */
public class ConnectReceiver extends BroadcastReceiver {
	private static boolean firstConnect = true;

	/**
	 * If this is the first time going online, fetch urls into queue
	 */
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (ConnectReceiver.hasInternet(context)) {
				if (firstConnect) {
					firstConnect = false;
					WallpaperManager.with(context).fetchNextUrls();
				}
			} else {
				firstConnect = true;
			}
		}
	}

	/**
	 * Checks if internet is enabled
	 */
	public static boolean hasInternet(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo;
		if (SettingsActivity.allowData(context))
			activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		else
			activeNetworkInfo = connectivityManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
}
