package com.akrolsmir.bakegami;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ConnectReceiver extends BroadcastReceiver{
	private static boolean firstConnect = true;
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
		    	ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo activeNetworkInfo;
				if(SettingsActivity.allowData(context))
					activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
				else
					activeNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				Log.d("nonet",""+intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));
				if(activeNetworkInfo != null && activeNetworkInfo.isConnected())
				{
					if(firstConnect){
						firstConnect = false;
						WallpaperManager.with(context).fetchNextUrls();
					}	
				}
				else
					firstConnect = true;
		}
	}

}
