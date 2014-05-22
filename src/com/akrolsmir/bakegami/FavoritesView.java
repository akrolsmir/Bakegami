package com.akrolsmir.bakegami;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

public class FavoritesView extends GridView {

	public FavoritesView(final Context context, AttributeSet attrs) {
		super(context, attrs);

		//		final GridView gv = (GridView) findViewById(R.id.favorites);
		final GridViewAdapter gva = new GridViewAdapter(context);
		this.setAdapter(gva);
		this.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(
						Uri.parse("file://" + Wallpaper.getFavorites().get(position)),
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
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				SparseBooleanArray checked = getCheckedItemPositions();
				// Respond to clicks on the actions in the CAB
				switch (item.getItemId()) {
				case R.id.menu_item_delete:
					//TODO confirm discard
					for (int i = 0; i < getAdapter().getCount(); i++) {
						if (checked.get(i)) {
							WallpaperManager.removeFavorite(i);
						}
					}
					Toast.makeText(context, getCheckedItemCount() + " unfavorited.",
							Toast.LENGTH_LONG).show();
					gva.notifyDataSetChanged();
					mode.finish();
					return true;
				case R.id.menu_item_share:
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_SEND_MULTIPLE);
					intent.setType("image/*");

					ArrayList<Uri> files = new ArrayList<Uri>();
					for (int i = 0; i < getAdapter().getCount(); i++) {
						if (checked.get(i)) {
							files.add(Uri.fromFile(new File(Wallpaper.getFavorites().get(i))));
						}
					}

					intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
					context.startActivity(intent);
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
}
