package com.akrolsmir.bakegami;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

//TODO rename to BackgroundManager, rename Wallpaper to Background
public class WallpaperManager {

	private SharedPreferences settings;
	private Context context;

	public WallpaperManager(Context context) {
		this.settings = context.getSharedPreferences("com.akrolsmir.bakegami", 0);
		this.context = context;
		fetchNextUrls();
	}

	public void nextWallpaper() {
		advanceCurrent();
		getCurrentWallpaper().setAsBackground();
	}

	public void favoriteWallpaper() {
		getCurrentWallpaper().favorite();
		Log.d("Favorited", getCurrentWallpaper().getCacheFile().toString());
	}
	
	public static void removeFavorite(int i) {
		Log.d("DELETING...", ""+ new File(getFavorites().get(i)).delete());
	}
	
	public static List<String> getFavorites() {
		List<String> result = new ArrayList<String>();

		File PIC_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		PIC_DIR = new File(PIC_DIR, "bakegami"); //TODO replace with app name
		PIC_DIR.mkdirs();

		File[] files = PIC_DIR.listFiles();
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
			}
		});

		for (File file : files) {
			result.add(file.getPath());
		}

		return result;
	}
	
	public void tweakWallpaper(){
		// TODO allow user to adjust wallpaper
	}

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
		return settings.getString("subreddit", "awwnime");
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
		// Placeholder. TODO remove
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

	public Wallpaper getCurrentWallpaper() {
		return new Wallpaper(context, getCurrentWallpaperURL());
	}
	
	private String DEFAULT_URL = "http://cdn.awwni.me/maav.jpg ";
	private String getCurrentWallpaperURL(){
		return settings.getString(QUEUE, DEFAULT_URL).split(" ")[0];
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
