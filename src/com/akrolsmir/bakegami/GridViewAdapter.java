package com.akrolsmir.bakegami;

import static android.widget.ImageView.ScaleType.CENTER_CROP;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

final class GridViewAdapter extends BaseAdapter {
	private final Context context;
	private List<String> favorites;

	public GridViewAdapter(Context context) {
		this.context = context;
		this.favorites = WallpaperManager.getFavorites();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SquaredImageView view = (SquaredImageView) convertView;
		if (view == null) {
			view = new SquaredImageView(context);
			view.setScaleType(CENTER_CROP);
		}

		// Get the image URL for the current position.
		String uri = getItem(position);

		// Trigger the download of the URL asynchronously into the image view.
		Picasso.with(context) //
		.load(new File(uri)) //
		//.placeholder(R.drawable.placeholder) //
		//.error(R.drawable.error) //
		.fit().centerCrop().into(view);

		view.setBackgroundResource(R.drawable.gridview_background);

		return view;
	}

	@Override
	public void notifyDataSetChanged() {
		favorites = WallpaperManager.getFavorites();
		super.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return favorites.size();
	}

	@Override
	public String getItem(int position) {
		return favorites.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	/** An image view which always remains square with respect to its width. */
	final class SquaredImageView extends ImageView implements Checkable {
		public SquaredImageView(Context context) {
			super(context);
		}

		public SquaredImageView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
		}

		// Makes this ImageView checkable
		private boolean checked = false;
		private final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

		@Override
		public int[] onCreateDrawableState(int extraSpace) {
			final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
			if (isChecked())
				mergeDrawableStates(drawableState, CHECKED_STATE_SET);
			return drawableState;
		}

		@Override
		public void setChecked(boolean checked) {
			this.checked = checked;
			refreshDrawableState();
		}

		@Override
		public boolean isChecked() {
			return checked;
		}

		@Override
		public void toggle() {
			setChecked(!checked);
		}
	}
}
