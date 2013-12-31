package com.akrolsmir.bakegami;

import java.util.Arrays;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class WallpaperManager {

	private SharedPreferences settings;
	private Context context;

	//	private List<String> urls = new ArrayList<String>(
	//			Arrays.asList(new String[] { "http://i.imgur.com/iCQxSJZ.jpg" }));

	public WallpaperManager(Context context) {
		this.settings = context.getSharedPreferences("com.akrolsmir.bakegami", 0);
		this.context = context;
		fetchNextUrls();
	}

	public void nextWallpaper() {
		advanceIndex();
		getCurrentWallpaper().setAsBackground();
	}

	public void favoriteWallpaper() {
		getCurrentWallpaper().favorite();
		Log.d("Favorited", getCurrentWallpaper().getCacheFile().toString());
	}
	
	public void tweakWallpaper(){
		Intent intent = new Intent(Intent.ACTION_CHOOSER);
		intent.setType("*/*");
		Uri uri = Uri.fromFile(getCurrentWallpaper().getCacheFile());
		intent.setData(uri);
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        context.startActivity(Intent.createChooser(intent, "Custom Heading..."));
//		intent.setClipData(new clip);
	}

	private void fetchNextUrls() {
		int index = settings.getInt("index", 0);
		int total = StringUtils.countOccurrencesOf(settings.getString("history", ""), " ");
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
		return settings.getString("subreddit", "awwnime");
	}

	private void parseUrlFromReddit(String rawJson, int numToFetch) {
		//		urls.clear();
		JsonElement object = new JsonParser().parse(rawJson);
		JsonArray children = object.getAsJsonObject().get("data").getAsJsonObject().get("children").getAsJsonArray();
		for (JsonElement child : children) {
			String url = child.getAsJsonObject().get("data").getAsJsonObject().get("url").getAsString();
			if (numToFetch > 0 && validImageUrl(url)) {
				enqueueURL(url);
				numToFetch--;
			}
		}
		// Placeholder. TODO remove
		//		return "http://i.imgur.com/iCQxSJZ.jpg";
	}

	// Returns true if URL has no spaces, ends in .jpg/.png and is not enqueued
	private boolean validImageUrl(String imageURL) {
		return !imageURL.contains(" ") && imageURL.matches("http://.*\\.(jpg|png)$")
				&& !enqueued(imageURL);
	}

	//TODO replace with a more reliable history
	private boolean enqueued(String imageURL) {
		return settings.getString("history", "").contains(imageURL);
	}

	private void enqueueURL(String imageURL) {
		Log.d("enqueueURL", imageURL);
		new Wallpaper(context, imageURL).cache(); //caches this URL
		String URLS = settings.getString("history", "");
		settings.edit().putString("history", URLS + imageURL + " ").commit();
	}

	private Wallpaper getCurrentWallpaper() {
		int index = settings.getInt("index", 0);
		String[] parts = settings.getString("history", "http://cdn.awwni.me/maav.jpg ").split(" ");
		Log.d("getCurrentURL", "parts: " + Arrays.toString(parts) + "\nindex: " + index);
		return new Wallpaper(context, parts[index]);
	}

	private void advanceIndex() {
		settings.edit().putInt("index", settings.getInt("index", 0) + 1).commit();
		fetchNextUrls(1);
	}
}
