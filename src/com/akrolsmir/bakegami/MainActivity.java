package com.akrolsmir.bakegami;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final EditText subredditText = (EditText) findViewById(R.id.subredditText);
		final EditText refreshTimeText = (EditText) findViewById(R.id.refreshTimeText);

		ImageButton nextButton = (ImageButton) findViewById(R.id.startButton);
		nextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				MainActivity.this.startService(new Intent(MainActivity.this,
						WallpaperControlWidgetProvider.RefreshService.class).setAction("start"));

				getSharedPreferences("com.akrolsmir.bakegami", 0).edit().putString("subreddit",
						subredditText.getText().toString()).commit();

				getSharedPreferences("com.akrolsmir.bakegami", 0).edit().putLong("refreshTime",
						Long.parseLong(refreshTimeText.getText().toString())).commit();

				Toast.makeText(MainActivity.this, "Cycling through r/" + subredditText.getText()
						+ " every " + refreshTimeText.getText() + " sec", Toast.LENGTH_LONG).show();
			}
		});

		ImageButton clearButton = (ImageButton) findViewById(R.id.clearButton);
		clearButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//stops refreshing
				Intent intent = new Intent(MainActivity.this,
						WallpaperControlWidgetProvider.RefreshService.class);
				MainActivity.this.startService(intent.setAction("stop"));//TODO unhardcode

				//clears history
				getSharedPreferences("com.akrolsmir.bakegami", 0).edit().clear().commit();
				
				// loads from current stuff
				getSharedPreferences("com.akrolsmir.bakegami", 0).edit().putString("subreddit",
						subredditText.getText().toString()).commit();

				getSharedPreferences("com.akrolsmir.bakegami", 0).edit().putLong("refreshTime",
						Long.parseLong(refreshTimeText.getText().toString())).commit();
				
				// clears cache
//				for(File file : getExternalCacheDir().listFiles())
//					file.delete();
				
				// Loads the first 5:
				new WallpaperManager(MainActivity.this);
				
				Toast.makeText(MainActivity.this, "Stopped cycling", Toast.LENGTH_SHORT).show();
			}
		});
		
		clearButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				// clears cache
				for(File file : getExternalCacheDir().listFiles())
					file.delete();
				
				Toast.makeText(MainActivity.this, "Cleared cache", Toast.LENGTH_SHORT).show();
				return false;
			}
		});
		
		Button buttonButton = (Button) findViewById(R.id.buttonbutton);
		buttonButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				new WallpaperManager(MainActivity.this).tweakWallpaper();

			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
//	public void updateConnectedFlags() {
//        ConnectivityManager connMgr = (ConnectivityManager) 
//                getSystemService(Context.CONNECTIVITY_SERVICE);
//        
//        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
//        if (activeInfo != null && activeInfo.isConnected()) {
//            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
//            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
//        } else {
//            wifiConnected = false;
//            mobileConnected = false;
//        }  
//    }

}
