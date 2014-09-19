package com.akrolsmir.bakegami;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images.Media;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.akrolsmir.bakegami.WallpaperControlWidgetProvider.RefreshService;
import com.squareup.picasso.Picasso;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		setContentView(R.layout.activity_main);
		
		findViewById(R.id.favButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				WallpaperManager.with(MainActivity.this).toggleFavorite();
			}
		});

		ImageButton nextButton = (ImageButton) findViewById(R.id.nextButton);
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				WallpaperManager.with(MainActivity.this).nextWallpaper();
			}
		});

		final ImageButton playPauseButton = (ImageButton) findViewById(R.id.pausePlayButton);
		playPauseButton.setImageResource(
				RefreshService.isCycling(this) ?
					android.R.drawable.ic_media_pause :
					android.R.drawable.ic_media_play
		);
		playPauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playPauseButton.setImageResource(
						RefreshService.isCycling(MainActivity.this) ?
							android.R.drawable.ic_media_play :
							android.R.drawable.ic_media_pause
				);
				Intent intent = new Intent(MainActivity.this, RefreshService.class);
				startService(intent.setAction(RefreshService.TOGGLE));
			}
		});
		
		ImageButton cropButton = (ImageButton) findViewById(R.id.cropButton);
		cropButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Try using WPM.getCropAndSetWallpaperIntent on sdk 19 and higher
				Intent cropIntent = new Intent("com.android.camera.action.CROP");
				cropIntent.setDataAndType(
						Uri.fromFile(WallpaperManager.with(MainActivity.this)
								.getCurrentWallpaper().getCacheFile()), "image/*");
				cropIntent.setClassName("com.google.android.apps.plus", 
						"com.google.android.apps.photoeditor.fragments.PlusCropActivityAlias");
				cropIntent.putExtra("crop","true");
				android.app.WallpaperManager wpm = android.app.WallpaperManager.getInstance(MainActivity.this);
				cropIntent.putExtra("aspectX", wpm.getDesiredMinimumWidth());
				cropIntent.putExtra("aspectY", wpm.getDesiredMinimumHeight());
				cropIntent.putExtra("outputX", wpm.getDesiredMinimumWidth());
				cropIntent.putExtra("outputY", wpm.getDesiredMinimumHeight());
				cropIntent.putExtra("return-data", true);
				try {
					startActivityForResult(cropIntent, 1);
				} catch (ActivityNotFoundException anfe) {
					try{
						cropIntent.setClassName("com.google.android.apps.plus", 
								"com.google.android.apps.photoeditor.fragments.PlusCropActivity");
						startActivityForResult(cropIntent, 1);
					} catch (ActivityNotFoundException anfe2) {
						Toast.makeText(MainActivity.this, "Cropping requires the latest Google+.", Toast.LENGTH_LONG).show();
					}
				}
			}
		});

		//TODO remove backdoor
		playPauseButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				WallpaperManager.with(MainActivity.this).resetQueueAndHistory();
				return false;
			}
		});
		
		ImageButton infoButton = (ImageButton) findViewById(R.id.infoButton);
		infoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				WallpaperManager.with(MainActivity.this).displayInfo();
			}
		});
		
		findViewById(R.id.currentBG).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(
						Uri.fromFile(WallpaperManager.with(MainActivity.this)
								.getCurrentWallpaper().getCacheFile()), "image/*");
				startActivity(intent);
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		menu.findItem(R.id.action_settings).setIntent(new Intent(this, SettingsActivity.class));
		return true;
	}
	
	/* Listen for changes made by services/the widget */
	
	public final static String NEXT = "com.akrolsmir.bakegami.NEXT";
	public final static String FAVORITE = "com.akrolsmir.bakegami.FAVORITE";
	
	@Override
	protected void onResume() {
		onNextBG();
		
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
		manager.registerReceiver(updateReceiver, new IntentFilter(NEXT));
		manager.registerReceiver(updateReceiver, new IntentFilter(FAVORITE));
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
		manager.unregisterReceiver(updateReceiver);
		super.onPause();
	}
	
	private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(NEXT)) {
				onNextBG();
			} else if (intent.getAction().equals(FAVORITE)) {
				onFavorite();
			}
		}
	};
	
	private void onNextBG() {
		ImageView currentBG = (ImageView) findViewById(R.id.currentBG);
		Picasso.with(this).load(
				WallpaperManager.with(this).getCurrentWallpaper().getCacheFile())
				.fit().centerInside().into(currentBG);
		onFavorite();
	}
	
	private void onFavorite() {
		((FavoritesView) findViewById(R.id.favorites)).onFavorite();
		
		((ImageButton) findViewById(R.id.favButton)).setImageResource(
				WallpaperManager.with(this).getCurrentWallpaper().imageInFavorites() ?
					android.R.drawable.star_big_on :
					android.R.drawable.star_big_off
		);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if (requestCode == 1 && resultCode == RESULT_OK) {
			android.app.WallpaperManager wpm = android.app.WallpaperManager.getInstance(MainActivity.this);
			Uri selectedImage = data.getData();
			try {
				Bitmap pic = Media.getBitmap(this.getContentResolver(), selectedImage);
				wpm.setBitmap(pic);
				getContentResolver().delete(selectedImage, null, null);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
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
