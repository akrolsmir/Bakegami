package com.akrolsmir.bakegami;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
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
		
//		Button buttonButton = (Button) findViewById(R.id.buttonbutton);
//		buttonButton.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View arg0) {
//				new WallpaperManager(MainActivity.this).tweakWallpaper();
//
//			}
//		});
		
		// Set up the favorites GridView
		
		final GridView gv = (GridView) findViewById(R.id.favorites);
		final GridViewAdapter gva = new GridViewAdapter(this);
		gv.setAdapter(gva);
		gv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse("file://" + WallpaperManager.getFavorites().get(position)), "image/*");
				startActivity(intent);
			}
		});
		gv.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
		gv.setMultiChoiceModeListener(new MultiChoiceModeListener() {

		    @Override
		    public void onItemCheckedStateChanged(ActionMode mode, int position,
		                                          long id, boolean checked) {
		        // Here you can do something when items are selected/de-selected,
		        // such as update the title in the CAB
		    }

		    @Override
		    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		    	SparseBooleanArray checked = gv.getCheckedItemPositions();
		        // Respond to clicks on the actions in the CAB
		        switch (item.getItemId()) {
		            case R.id.menu_item_delete:
		            	for(int i = 0; i < gv.getAdapter().getCount(); i++) {
		            		if(checked.get(i)) {
		            			WallpaperManager.removeFavorite(i);
		            		}
		            	}
		            	Toast.makeText(MainActivity.this, gv.getCheckedItemCount() + " unfavorited.", Toast.LENGTH_LONG).show();
		                mode.finish(); // Action picked, so close the CAB
		                gva.notifyDataSetChanged();
		                return true;
		            case R.id.menu_item_share:
		            	Intent intent = new Intent();
		            	intent.setAction(Intent.ACTION_SEND_MULTIPLE);
		            	intent.setType("image/*"); /* This example is sharing jpeg images. */
		            	
		            	ArrayList<Uri> files = new ArrayList<Uri>();
		            	for(int i = 0; i < gv.getAdapter().getCount(); i++) {
		            		if(checked.get(i)) {
		            			files.add(Uri.fromFile(new File(WallpaperManager.getFavorites().get(i))));
		            		}
		            	}
		            	
		            	intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
		            	startActivity(intent);
		            	mode.finish();
		            	return true;
		            default:
		                return false;
		        }
		    }

		    @Override
		    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		        // Inflate the menu for the CAB
		        MenuInflater inflater = mode.getMenuInflater();
		        inflater.inflate(R.menu.gridview_menu, menu);
		        return true;
		    }

		    @Override
		    public void onDestroyActionMode(ActionMode mode) {
		        // Here you can make any necessary updates to the activity when
		        // the CAB is removed. By default, selected items are deselected/unchecked.
		    }

		    @Override
		    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		        // Here you can perform updates to the CAB due to
		        // an invalidate() request
		        return false;
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
