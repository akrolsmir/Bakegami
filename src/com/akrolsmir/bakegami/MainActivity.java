package com.akrolsmir.bakegami;

import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.akrolsmir.bakegami.WallpaperControlWidgetProvider.RefreshService;
import com.squareup.picasso.Picasso;

public class MainActivity extends Activity {

	private Context context;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		setContentView(R.layout.activity_main);
		context = this;
		
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
				WallpaperManager.with(MainActivity.this).cropWallpaper();
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
		if(requestCode == Wallpaper.PIC_CROP)
		{
			android.app.WallpaperManager wpm = android.app.WallpaperManager.getInstance(context);
			Uri selectedImage = data.getData();
	        String[] filePathColumn = {MediaStore.Images.Media.DATA};
	        Cursor cursor = getContentResolver().query(
	                           selectedImage, filePathColumn, null, null, null);
	        cursor.moveToFirst();
	        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	        String filePath = cursor.getString(columnIndex);
	        cursor.close();
	        Bitmap thePic= BitmapFactory.decodeFile(filePath);
			try {
				wpm.setBitmap(thePic);
			} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
