package com.akrolsmir.bakegami;

import java.io.File;
import java.net.SocketException;
import java.util.regex.Pattern;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class WallpaperManager {

	private SharedPreferences settings;
	private Context context;
	private static WallpaperManager instance;
	private boolean setNextWallpaperAsBG = false;

	private WallpaperManager(Context context) {
		this.settings = context.getSharedPreferences(
				"com.akrolsmir.bakegami.WallpaperManager", 0);
		this.context = context;
		fetchNextUrls();
	}

	public static WallpaperManager with(Context context) {
		return instance == null ? instance = new WallpaperManager(context)
				: instance;
	}

	public void setWallpaper(File file) {
		getCurrentWallpaper().uncache();
		String history = settings.getString(HISTORY, "");
		settings.edit()
				.putString(
						HISTORY,
						file.toURI()
								+ "|"
								+ (file.toURI() + "").substring((file.toURI() + "")
										.lastIndexOf('/') + 1) + " " + history)
				.apply();
		getCurrentWallpaper().setAsBackground();
		// Notify MainActivity and the widget to update their views
		LocalBroadcastManager.getInstance(context).sendBroadcast(
				new Intent(MainActivity.NEXT));
		WallpaperControlWidgetProvider.updateViews(context);
	}

	public void nextWallpaper() {
		if (!settings.getString(QUEUE, "").contains(" ")) {
			ConnectivityManager connectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetworkInfo;
			if(SettingsActivity.allowData(context))
				activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
			else
				activeNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (activeNetworkInfo == null || !activeNetworkInfo.isConnected())
				Toast.makeText(context,
						"Connect to the Internet and try again.",
						Toast.LENGTH_LONG).show();
			else {
				Toast.makeText(
						context,
						"Out of unique images. Try adding more subreddits or increasing Cycle Time",
						Toast.LENGTH_LONG).show();
			}
			return;
		}
		if(!getNextWallpaper().imageInCache())
		{
			resetQueue();
			return;
		}
		getCurrentWallpaper().uncache();
		advanceCurrent();
		getCurrentWallpaper().setAsBackground();
		// Notify MainActivity and the widget to update their views
		LocalBroadcastManager.getInstance(context).sendBroadcast(
				new Intent(MainActivity.NEXT));
		WallpaperControlWidgetProvider.updateViews(context);
	}

	public void toggleFavorite() {
		getCurrentWallpaper().toggleFavorite();
		// Notify MainActivity and the widget to update their views
		LocalBroadcastManager.getInstance(context).sendBroadcast(
				new Intent(MainActivity.FAVORITE));
		WallpaperControlWidgetProvider.updateViews(context);
	}

	public Wallpaper getCurrentWallpaper() {
		return new Wallpaper(context, getCurrentWallpaperURL());
	}
	
	public Wallpaper getNextWallpaper() {
		return new Wallpaper(context, getNextWallpaperURL());
	}

	public void resetQueueAndHistory() {
		settings.edit().clear().apply();
		fetchNextUrls();
	}

	public void resetQueue() {
		settings.edit().putString(QUEUE, "").apply();
		for (File file : context.getExternalCacheDir().listFiles())
			if (!file.equals(getCurrentWallpaper().getCacheFile()))
				file.delete();
		fetchNextUrls();
	}

	public void tweakWallpaper() {
		// TODO allow user to adjust wallpaper
	}

	/* Private helper methods */

	public void fetchNextUrls() {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo;
		if(SettingsActivity.allowData(context))
			activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		else
			activeNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (activeNetworkInfo == null || !activeNetworkInfo.isConnected())
			return;
		int index = settings.getInt("index", 0);
		int total = StringUtils.countOccurrencesOf(
				settings.getString(QUEUE, ""), " ");
		fetchNextUrls(3 + index - total);
	}

	private void fetchNextUrls(final int numToFetch) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				RestTemplate restTemplate = new RestTemplate();
				restTemplate.getMessageConverters().add(
						new StringHttpMessageConverter());
				String rawJson = "";
				for (int i = 0; i < numToFetch; i++) {
					int sr = (int) (10 * Math.random());
					if (getSubreddit(sr).length() == 0)
						i--;
					else {
						try{
						rawJson = restTemplate.getForObject(
								"http://www.reddit.com/r/" + getSubreddit(sr)
										+ "/"+SortPreference.getValues(context)[0]+".json?"+
										( SortPreference.getValues(context).length <= 1? "" : "t="+SortPreference.getValues(context)[1]+"&")+"limit=100",
								String.class);
						}
						catch(ResourceAccessException e)
						{
							break;
						}
						if (!parseUrlFromReddit(rawJson))
							i--;
					}
				}
			}
		}).start();
	}

	private String getSubreddit(int index) {
		return SettingsActivity.getSubreddit(context, index);
	}

	private boolean parseUrlFromReddit(String rawJson) {
		JsonElement object = new JsonParser().parse(rawJson);
		JsonArray children = object.getAsJsonObject().get("data")
				.getAsJsonObject().get("children").getAsJsonArray();
		for (JsonElement child : children) {
			String url = child.getAsJsonObject().get("data").getAsJsonObject()
					.get("url").getAsString();
			boolean nsfw = child.getAsJsonObject().get("data")
					.getAsJsonObject().get("over_18").getAsBoolean();
			if (url.contains("imgur.com") && !url.contains("i.imgur.com")
					&& !url.contains("imgur.com/gallery/")
					&& !url.contains("imgur.com/a/"))
				url += ".jpg";
			if (validImageUrl(url)
					&& (!nsfw || SettingsActivity.showNSFW(context))) {
				String perma = child.getAsJsonObject().get("data")
						.getAsJsonObject().get("permalink").getAsString();
				String subreddit = child.getAsJsonObject().get("data")
						.getAsJsonObject().get("subreddit").getAsString();
				String title = child.getAsJsonObject().get("data")
						.getAsJsonObject().get("title").getAsString();
				String postURL = "http://www.i.reddit.com" + perma;
				perma = perma.substring(0, perma.lastIndexOf('/'));
				perma = perma.substring(perma.lastIndexOf('/') + 1)
						+ url.substring(url.lastIndexOf('.'));
				enqueueURL(url, perma);
				addInfo(perma, subreddit, title, postURL, url);
				return true;
			}
		}
		return false;
		// TODO handle case when out of images
		// return "http://i.imgur.com/iCQxSJZ.jpg";
	}

	// Returns true if URL has no spaces, ends in .jpg/.png and is not enqueued
	private boolean validImageUrl(String imageURL) {
		return !imageURL.contains(" ")
				&& imageURL.matches("https?://.*\\.(jpg|png)$")
				&& isNew(imageURL);
	}

	private String HISTORY = "history", QUEUE = "queue";

	private boolean isNew(String imageURL) {
		return !((settings.getString(HISTORY, "") + settings.getString(QUEUE,
				"")).contains(imageURL));
	}

	private void enqueueURL(String imageURL, String imageName) {
		Log.d("enqueueURL", imageURL + "|" + imageName);
		String queue = settings.getString(QUEUE, "");
		settings.edit()
				.putString(QUEUE, queue + imageURL + "|" + imageName + " ")
				.apply();
		if (setNextWallpaperAsBG) { // || settings.getString(QUEUE, "").length
									// == 0
			nextWallpaper();
			setNextWallpaperAsBG = false;
		} else {
			new Wallpaper(context, imageURL + "|" + imageName).cache();
		}
	}
	

	private void addInfo(String name, String sr, String title, String postURL,
			String url) {
		Log.d("AddInfo", name);
		settings.edit().putString(name + "_sr", sr).apply();
		settings.edit().putString(name + "_title", title).apply();
		settings.edit().putString(name + "_postURL", postURL).apply();
		settings.edit().putString(name + "_url", url).apply();
	}

	public void removeInfo(String name) {
		Log.d("RemoveInfo ", name);
		settings.edit().remove(name + "_sr").commit();
		settings.edit().remove(name + "_title").commit();
		settings.edit().remove(name + "_postURL").commit();
		settings.edit().remove(name + "_url").commit();
	}

	public void displayInfo(Context context) {
		displayInfo(getCurrentWallpaperURL().split(Pattern.quote("|"))[1],
				context);
	}

	public void displayInfo(String name, final Context context) {
		Log.d("DisplayInfo", name);
		final String[] rawInfo = { settings.getString(name + "_sr", "N/A"),
				settings.getString(name + "_postURL", "N/A"),
				settings.getString(name + "_url", "N/A"),
				settings.getString(name + "_title", "N/A") };
		Spanned[] info = {
				Html.fromHtml("<b>Subreddit:</b><br/>" + rawInfo[0]),
				Html.fromHtml("<b>Post Title:</b><br/>" + rawInfo[3]),
				Html.fromHtml("<b>Image URL:</b><br/>" + rawInfo[2]) };
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Info")
				.setItems(info, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (!rawInfo[which].equals("N/A")) {
							Intent browserIntent;
							if (which == 0)
								browserIntent = new Intent(Intent.ACTION_VIEW,
										Uri.parse("http://www.i.reddit.com/r/"
												+ rawInfo[which]));
							else
								browserIntent = new Intent(Intent.ACTION_VIEW,
										Uri.parse(rawInfo[which]));
							context.startActivity(browserIntent);
						}

					}
				})
				.setPositiveButton("Done",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
							}
						});
		builder.create().show();
	}

	// The current wallpaper is at the top of the history stack
	private String DEFAULT_URL = "http://cdn.awwni.me/maav.jpg|maav.jpg";

	public String getCurrentWallpaperURL() {
		String url = settings.getString(HISTORY, DEFAULT_URL + " ").split(" ")[0];
		return url.contains("/") ? url : DEFAULT_URL;
	}
	
	public String getNextWallpaperURL() {
		String url = settings.getString(QUEUE, DEFAULT_URL + " ").split(" ")[0];
		return url.contains("/") ? url : DEFAULT_URL;
	}

	// Push the head of the queue onto history, which marks it as current
	private void advanceCurrent() {
		String queue = settings.getString(QUEUE, "");
		settings.edit()
				.putString(QUEUE, queue.substring(queue.indexOf(" ") + 1))
				.apply();
		String history = settings.getString(HISTORY, "");
		settings.edit().putString(HISTORY, queue.split(" ")[0] + " " + history)
				.apply();
		Log.d("HISTORY", settings.getString(HISTORY, ""));
		Log.d("QUEUE", settings.getString(QUEUE, ""));

		fetchNextUrls();
	}
}
