package com.akrolsmir.bakegami;

import java.io.File;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class WallpaperManager {

	private SharedPreferences settings;
	private Context context;
	private static WallpaperManager instance;
	private boolean setNextWallpaperAsBG = false;

	private WallpaperManager(Context context) {
		this.settings = context.getSharedPreferences("com.akrolsmir.bakegami.WallpaperManager", 0);
		this.context = context;
		fetchNextUrls();
	}
	
	public static WallpaperManager with(Context context) {
		return instance == null ? instance = new WallpaperManager(context) : instance; 
	}
	
	public void setWallpaper(File file) {
		getCurrentWallpaper().uncache();
		String history = settings.getString(HISTORY, "");
		settings.edit().putString(HISTORY, file.toURI()+"|"+(file.toURI()+"").substring((file.toURI()+"").lastIndexOf('/')+1) + " " + history).apply();
		getCurrentWallpaper().setAsBackground();
		// Notify MainActivity and the widget to update their views
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.NEXT));
		WallpaperControlWidgetProvider.updateViews(context);
	}

	public void nextWallpaper() {
		getCurrentWallpaper().uncache();
		advanceCurrent();
		getCurrentWallpaper().setAsBackground();
		// Notify MainActivity and the widget to update their views
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.NEXT));
		WallpaperControlWidgetProvider.updateViews(context);
	}
	
	public void toggleFavorite() {
		getCurrentWallpaper().toggleFavorite();
		// Notify MainActivity and the widget to update their views
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.FAVORITE));
		WallpaperControlWidgetProvider.updateViews(context);
	}
	
	public Wallpaper getCurrentWallpaper() {
		return new Wallpaper(context, getCurrentWallpaperURL());
	}
	
	public void resetQueueAndHistory() {
		settings.edit().clear().apply();
		fetchNextUrls();
	}
	
	public void resetQueue() {
		settings.edit().putString(QUEUE, "").apply();
		setNextWallpaperAsBG = true;
		fetchNextUrls();
	}
	
	public void tweakWallpaper(){
		// TODO allow user to adjust wallpaper
	}
	
	/* Private helper methods */

	private void fetchNextUrls() {
		int index = settings.getInt("index", 0);
		int total = StringUtils.countOccurrencesOf(settings.getString(QUEUE, ""), " ");
		fetchNextUrls(3 + index - total);
	}

	private void fetchNextUrls(final int numToFetch) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				RestTemplate restTemplate = new RestTemplate();
				restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
				String rawJson = "";
				for(int i = 0; i < numToFetch; i++)
				{
					int sr = (int)(10*Math.random());
					if(getSubreddit(sr).length() == 0)
						i--;
					else
					{
						rawJson = restTemplate.getForObject("http://www.reddit.com/r/"
								+ getSubreddit(sr) + "/top.json?t=week&limit=100", 
								String.class);
					
						if(!parseUrlFromReddit(rawJson))
							i--;
					}
				}
			}
		}).start();
	}

	private String getSubreddit( int index ) {
		return SettingsActivity.getSubreddit(context,index);
	}

	private boolean parseUrlFromReddit(String rawJson) {
		JsonElement object = new JsonParser().parse(rawJson);
		JsonArray children = object.getAsJsonObject().get("data").getAsJsonObject().get("children").getAsJsonArray();
		for (JsonElement child : children) {
			String url = child.getAsJsonObject().get("data").getAsJsonObject().get("url").getAsString();
			boolean nsfw = child.getAsJsonObject().get("data").getAsJsonObject().get("over_18").getAsBoolean();
			if(url.contains("imgur.com") && !url.contains("imgur.com/a/") && !url.contains("i.imgur.com"))
				url+=".jpg";
			if (validImageUrl(url) && (!nsfw || SettingsActivity.showNSFW(context))) {
				String perma = child.getAsJsonObject().get("data").getAsJsonObject().get("permalink").getAsString();
				perma = perma.substring(0,perma.lastIndexOf('/')-1);
				enqueueURL(url,perma.substring(perma.lastIndexOf('/')+1)+url.substring(url.lastIndexOf('.')));
				return true;
			}
		}
		return false;
		// TODO handle case when out of images
		//		return "http://i.imgur.com/iCQxSJZ.jpg";
	}

	// Returns true if URL has no spaces, ends in .jpg/.png and is not enqueued
	private boolean validImageUrl(String imageURL) {
		return !imageURL.contains(" ") && imageURL.matches("https?://.*\\.(jpg|png)$")
				&& isNew(imageURL);
	}
	
	private String HISTORY = "history", QUEUE = "queue";
	private boolean isNew(String imageURL){
		return !((settings.getString(HISTORY, "") + settings.getString(QUEUE, "")).contains(imageURL));
	}

	private void enqueueURL(String imageURL, String imageName) {
		Log.d("enqueueURL", imageURL+"|"+imageName);
		String queue = settings.getString(QUEUE, "");
		settings.edit().putString(QUEUE, queue + imageURL + "|" + imageName + " ").apply();
		if (setNextWallpaperAsBG) { // || settings.getString(QUEUE, "").length == 0
			nextWallpaper();
			setNextWallpaperAsBG = false;
		} else {
			new Wallpaper(context, imageURL+"|"+imageName).cache();
		}
	}
	
	// The current wallpaper is at the top of the history stack
	private String DEFAULT_URL = "http://cdn.awwni.me/maav.jpg|maav.jpg";
	private String getCurrentWallpaperURL(){
		String url = settings.getString(HISTORY, DEFAULT_URL + " ").split(" ")[0];
		return url.contains("/") ? url : DEFAULT_URL;
	}

	// Push the head of the queue onto history, which marks it as current
	private void advanceCurrent() {
		String queue = settings.getString(QUEUE, "");
		settings.edit().putString(QUEUE, queue.substring(queue.indexOf(" ") + 1)).apply();
		String history = settings.getString(HISTORY, "");
		settings.edit().putString(HISTORY, queue.split(" ")[0] + " " + history).apply();
		Log.d("HISTORY", settings.getString(HISTORY, ""));
		Log.d("QUEUE", settings.getString(QUEUE, ""));
		
		fetchNextUrls(1);
	}
}
