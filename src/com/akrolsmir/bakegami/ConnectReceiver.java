package com.akrolsmir.bakegami;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ConnectReceiver extends BroadcastReceiver{
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
		    	ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo activeNetworkInfo = connectivityManager
						.getActiveNetworkInfo();
				Log.d("nonet",""+intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));
				while(activeNetworkInfo == null && !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false))
					activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		    	WallpaperManager.with(context).fetchNextUrls();
		}
	}

}
