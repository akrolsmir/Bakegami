package com.akrolsmir.bakegami.dummy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.R;
import android.widget.TextView;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent {

	/**
	 * An array of sample (dummy) items.
	 */
	public static List<DummyItem> ITEMS = new ArrayList<DummyItem>();

	/**
	 * A map of sample (dummy) items, by ID.
	 */
	public static Map<String, DummyItem> ITEM_MAP = new HashMap<String, DummyItem>();

	static {
		addItem(new DummyItem("1", "Manage Subreddits",0,"Info about subs"));
		addItem(new DummyItem("2", "Sort Type",0,"Info about sort"));
		addItem(new DummyItem("3", "Wallpaper Change Frequency",0,"Info about wcf"));
		addItem(new DummyItem("4", "Allow Data Usage",0,"Info about data usage"));
		addItem(new DummyItem("5", "Show NSFW content",0,"Info about nsfw"));
		addItem(new DummyItem("6", "Button",android.R.drawable.btn_star_big_off,"Info about favorite"));
		addItem(new DummyItem("7", "Button",android.R.drawable.ic_media_play,"Info about play"));
		addItem(new DummyItem("8", "Button",android.R.drawable.ic_media_pause,"Info about pause"));
		addItem(new DummyItem("9", "Button",android.R.drawable.ic_media_next,"Info about skip"));
		addItem(new DummyItem("10", "Button",android.R.drawable.ic_menu_crop,"Info about crop"));
		addItem(new DummyItem("11", "Button",com.akrolsmir.bakegami.R.drawable.ic_action_about,"Info about info"));
	}

	private static void addItem(DummyItem item) {
		ITEMS.add(item);
		ITEM_MAP.put(item.id, item);
	}

	/**
	 * A dummy item representing a piece of content.
	 */
	public static class DummyItem {
		public String id;
		public String content;
		public int image;
		public String info;
		
		public DummyItem(String id, String content, int image, String info) {
			this.id = id;
			this.content = content;
			this.image = image;
			this.info = info;
		}

		@Override
		public String toString() {
			return content;
		}
	}
}
