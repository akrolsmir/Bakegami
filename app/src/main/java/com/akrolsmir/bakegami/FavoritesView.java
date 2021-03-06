package com.akrolsmir.bakegami;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavoritesView extends GridView {

	private SharedPreferences settings;
	private Context context;
	private GridViewAdapter gva;
	private List<File> files;

	public FavoritesView(final Context context, AttributeSet attrs) {
		super(context, attrs);
		this.settings = context.getSharedPreferences(
				"com.akrolsmir.bakegami.WallpaperManager", 0);
		this.context = context;
		gva = new GridViewAdapter(context);
		files = Wallpaper.getFavorites();
		this.setAdapter(gva);
		this.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, final int position, long id) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
				builder.setMessage("Set this image as wallpaper?");
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						WallpaperManager.with(context).setWallpaper(files.get(position));
					}
				});
				builder.setNegativeButton("Cancel", null);
				builder.show();
			}
		});
		this.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
		this.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
												  boolean checked) {
				// Here you can do something when items are selected/de-selected,
				// such as update the title in the CAB
				if (getCheckedItemCount() > 1) {
					mode.getMenu().findItem(R.id.menu_item_view).setVisible(false);
					mode.getMenu().findItem(R.id.menu_item_info).setVisible(false);
				} else if (getCheckedItemCount() == 1) {
					mode.getMenu().findItem(R.id.menu_item_view).setVisible(true);
					mode.getMenu().findItem(R.id.menu_item_info).setVisible(true);
				}
			}

			@Override
			public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
				final SparseBooleanArray checked = getCheckedItemPositions();
				Intent intent = new Intent();
				// Respond to clicks on the actions in the CAB
				switch (item.getItemId()) {
					case R.id.menu_item_view:
						intent.setAction(Intent.ACTION_VIEW);
						intent.setDataAndType(
								Uri.fromFile(files.get(
										checked.keyAt(0))), "image/*");
						context.startActivity(intent);
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
										WallpaperManager.with(context).removeFavorite(files.get(i));
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
						intent.setAction(getCheckedItemCount() == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
						intent.setType("image/*");

						ArrayList<Uri> fils = new ArrayList<Uri>();
						for (int i = 0; i < getAdapter().getCount(); i++) {
							if (checked.get(i)) {
								fils.add(Uri.fromFile(files.get(i)));
								if (fils.size() == getCheckedItemCount())
									break;
							}
						}
						intent.setType("image/*");
						if (getCheckedItemCount() > 1)
							intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fils);
						else
							intent.putExtra(Intent.EXTRA_STREAM, fils.get(0));
						context.startActivity(Intent.createChooser(intent, "Share via"));
						mode.finish();
						return true;
					case R.id.menu_item_info:
						String path = files.get(checked.keyAt(0)).getAbsolutePath();
						WallpaperManager.with(context).displayInfo(path.substring(path.lastIndexOf("/") + 1), context);
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
				if (getCheckedItemCount() > 1) {
					menu.findItem(R.id.menu_item_view).setVisible(false);
					menu.findItem(R.id.menu_item_info).setVisible(false);
				} else if (getCheckedItemCount() == 1) {
					menu.findItem(R.id.menu_item_view).setVisible(true);
					menu.findItem(R.id.menu_item_info).setVisible(true);
				}
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
		gva = new GridViewAdapter(context);
		setAdapter(gva);
		files = Wallpaper.getFavorites();
		((GridViewAdapter) this.getAdapter()).notifyDataSetChanged();
	}

	public void filter(String query) {
		files = new ArrayList<File>();
		for (File file : Wallpaper.getFavorites()) {
			String name = file.getName();
			String subreddit = settings.getString(name + "_sr", "").toLowerCase();
			String title = settings.getString(name + "_title", "").toLowerCase();
			query = query.toLowerCase(Locale.getDefault());
			if (subreddit.contains(query) || title.contains(query)) {
				files.add(file);
			}
		}

		gva = new GridViewAdapter(context, files);
		setAdapter(gva);
	}

	public void unfilter() {
		gva = new GridViewAdapter(context);
		setAdapter(gva);
		files = Wallpaper.getFavorites();
	}
}
