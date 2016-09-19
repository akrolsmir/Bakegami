package com.akrolsmir.bakegami;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import com.akrolsmir.bakegami.settings.QueryActivity;
import com.akrolsmir.bakegami.settings.SettingsActivity;
import com.akrolsmir.bakegami.settings.SortPreference;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

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
		String uri = file.toURI() + "";
		String name = uri.substring(uri.lastIndexOf('/') + 1);
		String last = settings.getString(name + "_url", uri) + "|" + name + " ";
		setHistory(last + getHistory());
		getCurrentWallpaper().setAsBackground();

		// Notify MainActivity and the widget to update their views
		LocalBroadcastManager.getInstance(context).sendBroadcast(
				new Intent(MainActivity.NEXT));
		WallpaperControlWidgetProvider.updateViews(context);
	}

	public void nextWallpaper() {
		if (!getQueue().contains(" ")) {
			if (ConnectReceiver.hasInternet(context)) {
				// No images in queue
				advanceCurrent();
				Toast.makeText(
						context,
						"Out of unique images. Try changing settings to avoid this issue.",
						Toast.LENGTH_LONG).show();
			} else {
				// No internet
				Toast.makeText(context,
						"Connect to the Internet and try again.",
						Toast.LENGTH_LONG).show();
			}
			return;
		}
		if (!getNextWallpaper().imageInCache()) {
			resetQueue();
			Toast.makeText(
					context,
					"Finding more images. Wait a few seconds and try again.",
					Toast.LENGTH_LONG).show();
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

	public void resetQueue() {
		setQueue("");
		for (File file : context.getExternalCacheDir().listFiles())
			if (!file.equals(getCurrentWallpaper().getCacheFile()))
				file.delete();
		fetchNextUrls();
	}

	public void removeFavorite(File f) {
		String canonicalPath = "";
		try {
			canonicalPath = f.getCanonicalPath();
		} catch (IOException e) {
			canonicalPath = f.getAbsolutePath();
		}
		if (!getCurrentWallpaperURL().contains(canonicalPath.substring(canonicalPath.lastIndexOf("/") + 1)))
			removeInfo(canonicalPath.substring(canonicalPath.lastIndexOf("/") + 1));
		final Uri uri = MediaStore.Files.getContentUri("external");
		final int result = context.getContentResolver().delete(uri,
				MediaStore.Files.FileColumns.DATA + "=?",
				new String[]{canonicalPath});
		if (result == 0) {
			final String absolutePath = f.getAbsolutePath();
			if (!absolutePath.equals(canonicalPath)) {
				if (context.getContentResolver().delete(uri,
						MediaStore.Files.FileColumns.DATA + "=?",
						new String[]{absolutePath}) == 0)
					f.delete();
			} else
				f.delete();
		}
	}

	/* Private helper methods */
	public interface RedditService {
		@GET("r/{subreddit}/{sort}.json")
		Call<Listing> fromSubreddit(
				@Path("subreddit") String subreddit,
				@Path("sort") String sort,
				@QueryMap Map<String, String> options);

		// TODO ensure this accurately matches
		@GET("search.json")
		Call<Listing> fromKeyword(
				@Query("q") String keyword,
				@Query("sort") String sort,
				@QueryMap Map<String, String> options);
	}

	class Listing {
		ListingData data;

		class ListingData {
			List<Link> children;
			String after;

			class Link {
				LinkData data;

				class LinkData {
					boolean over_18;
					String url, permalink, subreddit, title;
				}
			}
		}
	}

	public void fetchNextUrls() {
		String[] offsets = new String[QueryActivity.numQueries(context)];
		for (int i = 0; i < QueryActivity.numQueries(context); i++) {
			offsets[i] = "";
		}
		fetchNextUrls(offsets);
	}

	private void fetchNextUrls(final String[] offsets) {
		if (!ConnectReceiver.hasInternet(context))
			return;

		if (getQueueLength() >= 3)
			return;

		// Set up retrofit adapter for
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("http://www.reddit.com/")
				.addConverterFactory(GsonConverterFactory.create())
				.build();

		RedditService service = retrofit.create(RedditService.class);

		final int sr = (int) (QueryActivity.numQueries(context) * Math.random());
		String sort = SortPreference.getValues(context)[0];
		String subreddit = getSubreddit(sr).substring(1);
		String keyword;
		try {
			keyword = URLEncoder.encode(subreddit, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Directly use the entered string. Hacky.
			keyword = subreddit;
		}

		Map<String, String> options = new HashMap<String, String>();
		options.put("limit", "100");
		options.put("after", offsets[sr]);
		if (SortPreference.getValues(context).length > 1) {
			String time = SortPreference.getValues(context)[1];
			options.put("t", time);
		}

		// Callback to be invoked after Retrofit finishes parsing Reddit's response
		Callback<Listing> callback = new Callback<Listing>() {
			@Override
			public void onResponse(Call<Listing> call, Response<Listing> response) {
				Listing listing = response.body();
				if (!parse(listing)) {
					offsets[sr] = listing.data.after;
				}
				// Recursively continue
				fetchNextUrls(offsets);
			}

			@Override
			public void onFailure(Call<Listing> call, Throwable t) {
				Log.e("fetchNextUrls", "Retrofit Error:", t);
			}
		};


		if (getSubreddit(sr).startsWith("r")) {
			service.fromSubreddit(subreddit, sort, options).enqueue(callback);
		} else {
			service.fromKeyword(keyword, sort, options);
		}
	}

	private int getQueueLength() {
		return getQueue().length() - getQueue().replace(" ", "").length();
	}

	private String getSubreddit(int index) {
		return QueryActivity.getSubreddit(context, index);
	}

	private boolean parse(Listing listing) {
		for (Listing.ListingData.Link child : listing.data.children) {
			String url = child.data.url;

			// Reformat particular imgur links
			if (url.contains("imgur.com") && !url.contains("i.imgur.com")
					&& !url.contains("imgur.com/gallery/")
					&& !url.contains("imgur.com/a/"))
				url += ".jpg";


			// Add one link if it's valid and safe for work enough
			if (validImageUrl(url) && (!child.data.over_18 || SettingsActivity.showNSFW(context))) {
				String perma = child.data.permalink;
				String postURL = "http://m.reddit.com" + perma;
				perma = perma.substring(0, perma.lastIndexOf('/'));
				perma = perma.substring(perma.lastIndexOf('/') + 1)
						+ url.substring(url.lastIndexOf('.'));
				enqueueURL(url, perma);
				addInfo(perma, child.data.subreddit, child.data.title, postURL, url);
				return true;
			}
		}

		// None of the links were valid, try next page
		return false;
	}

	// Returns true if URL has no spaces, ends in .jpg/.png and is not enqueued
	private boolean validImageUrl(String imageURL) {
		return !imageURL.contains(" ")
				&& imageURL.matches("https?://.*\\.(jpg|png)$")
				&& isNew(imageURL);
	}

	private boolean isNew(String imageURL) {
		return !(getHistory() + getQueue()).contains(imageURL);
	}

	private void enqueueURL(String imageURL, String imageName) {
		// Log.d("enqueueURL", imageURL + "|" + imageName);
		setQueue(getQueue() + imageURL + "|" + imageName + " ");
		if (setNextWallpaperAsBG) { // || settings.getString(QUEUE, "").length == 0
			nextWallpaper();
			setNextWallpaperAsBG = false;
		} else {
			new Wallpaper(context, imageURL + "|" + imageName).cache();
		}
	}


	private void addInfo(String name, String sr, String title, String postURL, String url) {
		// Log.d("AddInfo", name);
		settings.edit().putString(name + "_sr", sr).apply();
		settings.edit().putString(name + "_title", title).apply();
		settings.edit().putString(name + "_postURL", postURL).apply();
		settings.edit().putString(name + "_url", url).apply();
	}

	public void removeInfo(String name) {
		// Log.d("RemoveInfo ", name);
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
		// Log.d("DisplayInfo", name);
		final String[] rawInfo = {settings.getString(name + "_sr", "N/A"),
				settings.getString(name + "_postURL", "N/A"),
				settings.getString(name + "_url", "N/A"),
				settings.getString(name + "_title", "N/A")};
		Spanned[] info = {
				Html.fromHtml("<b>Subreddit:</b><br/>" + rawInfo[0]),
				Html.fromHtml("<b>Post Title:</b><br/>" + rawInfo[3]),
				Html.fromHtml("<b>Image URL:</b><br/>" + rawInfo[2])};
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Details")
				.setItems(info, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (!rawInfo[which].equals("N/A")) {
							Intent browserIntent;
							if (which == 0)
								browserIntent = new Intent(Intent.ACTION_VIEW,
										Uri.parse("http://m.reddit.com/r/"
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
		builder.show();
	}

	public void dequeue(String imageURL) {
		setQueue(getQueue().replace(imageURL + " ", ""));
		setHistory(getHistory() + imageURL + " ");
		new Wallpaper(context, imageURL).uncache();
		fetchNextUrls();
		// Log.d("AFTER DEQUEUE",getQueue());
	}

	// The current wallpaper is at the top of the history stack
	private String DEFAULT_URL = "http://pixabay.com/get/9a1e9044203f49270547/1414394710/spring-179584_1280.jpg|default0.jpg";

	public String getCurrentWallpaperURL() {
		String url = getHistory().split(" ")[0];
		return url.contains("/") ? url : DEFAULT_URL;
	}

	public String getNextWallpaperURL() {
		String url = getQueue().split(" ")[0];
		return url.contains("/") ? url : DEFAULT_URL;
	}

	// Push the head of the queue onto history, which marks it as current
	private void advanceCurrent() {
		setHistory(getQueue().split(" ")[0] + " " + getHistory());
		setQueue(getQueue().substring(getQueue().indexOf(" ") + 1));
		// Log.d("HISTORY", getHistory());
		// Log.d("QUEUE", getQueue());

		fetchNextUrls();
	}

	private String HISTORY = "history", QUEUE = "queue";

	private String getHistory() {
		return settings.getString(HISTORY, DEFAULT_URL + " ");
	}

	private String getQueue() {
		return settings.getString(QUEUE, DEFAULT_URL + " ");
	}

	private void setHistory(String history) {
		settings.edit().putString(HISTORY, history).apply();
	}

	private void setQueue(String queue) {
		settings.edit().putString(QUEUE, queue).apply();
	}
}