package com.akrolsmir.bakegami;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

public class FavoritesView extends GridView {
	
	public FavoritesView(final Context context, AttributeSet attrs) {
		super(context, attrs);

		final GridViewAdapter gva = new GridViewAdapter(context);
		this.setAdapter(gva);
		this.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(
						Uri.fromFile(Wallpaper.getFavorites().get(position)),
						"image/*");
				context.startActivity(intent);
			}
		});
		this.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
		this.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
					boolean checked) {
				// Here you can do something when items are selected/de-selected,
				// such as update the title in the CAB
			}

			@Override
			public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
				final SparseBooleanArray checked = getCheckedItemPositions();
				// Respond to clicks on the actions in the CAB
				switch (item.getItemId()) {
				case R.id.menu_item_set:
					for (int i = 0; i < getAdapter().getCount(); i++) {
						if (checked.get(i)) {
							WallpaperManager.with(context).setWallpaper(Wallpaper.getFavorites().get(i));
							break;
						}
					}
					mode.finish();
					return true;
				case R.id.menu_item_delete:
					AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
					builder.setMessage("Unfavorite " + (getCheckedItemCount() == 1 ? 
							"this image?" : "these " + getCheckedItemCount() + " images?"));
					builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Count backwards to ensure correct index is used
							for (int i = getAdapter().getCount() - 1; i >= 0; i--) {
								if (checked.get(i)) {
									Wallpaper.removeFavorite(i);
								}
							}
							LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.FAVORITE));
							mode.finish();
						}
					});
					builder.setNegativeButton("Cancel", null);
					builder.show();
					
					return true;
				case R.id.menu_item_share:
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_SEND_MULTIPLE);
					intent.setType("image/*");

					ArrayList<Uri> files = new ArrayList<Uri>();
					for (int i = 0; i < getAdapter().getCount(); i++) {
						if (checked.get(i)) {
							files.add(Uri.fromFile(Wallpaper.getFavorites().get(i)));
						}
					}

					intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
					context.startActivity(Intent.createChooser(intent, "Share via"));
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
	
	public void onFavorite() {
		((GridViewAdapter)this.getAdapter()).notifyDataSetChanged();
	}
}
