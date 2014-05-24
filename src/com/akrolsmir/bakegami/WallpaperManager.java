package com.akrolsmir.bakegami;

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

	private WallpaperManager(Context context) {
		this.settings = context.getSharedPreferences("com.akrolsmir.bakegami.WallpaperManager", 0);
		this.context = context;
		fetchNextUrls();
	}
	
	public static WallpaperManager with(Context context) {
		return instance == null ? instance = new WallpaperManager(context) : instance; 
	}

	public void nextWallpaper() {
		advanceCurrent();
		getCurrentWallpaper().setAsBackground();
		// Notify MainActivity to update its views
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.NEXT));
		
	}
	
	public Wallpaper getCurrentWallpaper() {
		return new Wallpaper(context, getCurrentWallpaperURL());
	}
	
	public void clearHistory() {
		settings.edit().clear().commit();
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
				String rawJson = restTemplate.getForObject("http://www.reddit.com/r/"
						+ getSubreddit() + "/top.json?t=week", String.class);
				parseUrlFromReddit(rawJson, numToFetch);
			}
		}).start();
	}

	private String getSubreddit() {
		return SettingsActivity.with(context).getString(SettingsActivity.KEY_PREF_SUBREDDIT, "");
	}

	private void parseUrlFromReddit(String rawJson, int numToFetch) {
		JsonElement object = new JsonParser().parse(rawJson);
		JsonArray children = object.getAsJsonObject().get("data").getAsJsonObject().get("children").getAsJsonArray();
		for (JsonElement child : children) {
			String url = child.getAsJsonObject().get("data").getAsJsonObject().get("url").getAsString();
			if (numToFetch > 0 && validImageUrl(url)) {
				enqueueURL(url);
				numToFetch--;
			}
		}
		// TODO handle case when out of images
		//		return "http://i.imgur.com/iCQxSJZ.jpg";
	}

	// Returns true if URL has no spaces, ends in .jpg/.png and is not enqueued
	private boolean validImageUrl(String imageURL) {
		return !imageURL.contains(" ") && imageURL.matches("http://.*\\.(jpg|png)$")
				&& isNew(imageURL);
	}
	
	private String HISTORY = "history", QUEUE = "queue";
	private boolean isNew(String imageURL){
		return !((settings.getString(HISTORY, "") + settings.getString(QUEUE, "")).contains(imageURL));
	}


	private void enqueueURL(String imageURL) {
		Log.d("enqueueURL", imageURL);
		new Wallpaper(context, imageURL).cache();
		String queue = settings.getString(QUEUE, "");
		settings.edit().putString(QUEUE, queue + imageURL + " ").commit();
	}
	
	private String DEFAULT_URL = "http://cdn.awwni.me/maav.jpg";
	private String getCurrentWallpaperURL(){
		String url = settings.getString(QUEUE, DEFAULT_URL + " ").split(" ")[0];
		Log.d("URLURLURL", "url: " + url);
		return url.contains("/") ? url : DEFAULT_URL;
	}

	private void advanceCurrent() {
		// move current from queue to history
		String history = settings.getString(HISTORY, "");
		settings.edit().putString(HISTORY, history + getCurrentWallpaperURL() + " ").commit();
		String queue = settings.getString(QUEUE, "");
		settings.edit().putString(QUEUE, queue.substring(queue.indexOf(" ") + 1)).commit();
		Log.d("HISTORY", settings.getString(HISTORY, ""));
		Log.d("QUEUE", settings.getString(QUEUE, ""));
		
		fetchNextUrls(1);
	}
}
