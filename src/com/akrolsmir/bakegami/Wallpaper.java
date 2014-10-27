package com.akrolsmir.bakegami;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

public class Wallpaper {

	private Context context;
	private String imageURL;
	private String imageName;

	private File CACHE_DIR;
	private static File PIC_DIR = new File(
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
			"reddit-starred"); // TODO replace with name of app

	public Wallpaper(Context context, String imageURL) {
		this.context = context;
		this.imageURL = imageURL.split(Pattern.quote("|"))[0];
		this.imageName = imageURL.split(Pattern.quote("|"))[1];
		CACHE_DIR = context.getExternalCacheDir();
		CACHE_DIR.mkdirs();
		PIC_DIR.mkdirs();
	}
	
	public Wallpaper(Context context) {
		this.context = context;
		this.imageURL = "http://cdn.awwni.me/default0.jpg";
		this.imageName = "default0.jpg";
		CACHE_DIR = context.getExternalCacheDir();
		CACHE_DIR.mkdirs();
		PIC_DIR.mkdirs();
		try {
			InputStream in = context.getResources().openRawResource(R.raw.default0);
			transfer(in, new FileOutputStream(getCacheFile()));
			toggleFavorite();
		} catch (Exception e) {
			// TODO handle?
			e.printStackTrace();
		}
	}

	public void cache() {
		if (!imageInCache()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// Download stuff to cache folder
						if (imageInFavorites())
							copyFile(getFavoriteFile(), getCacheFile());
						else
							downloadFile(imageURL, getCacheFile());
					} catch (Exception e) {
						com.akrolsmir.bakegami.WallpaperManager.with(context).resetQueue();
						// TODO handle?
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	public void uncache() {
		if (imageInCache()) {
			if (!imageInFavorites())
				com.akrolsmir.bakegami.WallpaperManager.with(context)
						.removeInfo(imageName);
			getCacheFile().delete();
		}
	}

	public void setAsBackground() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// Grab the picture from cache
					WallpaperManager wpm = WallpaperManager.getInstance(context);
					if (!imageInCache()) {
						if (imageInFavorites()) // TODO this is duplicating cache...
							copyFile(getFavoriteFile(), getCacheFile());
						else
							downloadFile(imageURL, getCacheFile());
					}   
					FileInputStream fis = new FileInputStream(getCacheFile());
					wpm.setStream(fis);
					Log.d("Changed wallpaper", imageURL);
				} catch (FileNotFoundException e) {
					// TODO handle?
					e.printStackTrace();
				} catch (IOException e) {
					// TODO handle?
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	interface ReloadCallback {
		void onFinish();
	}
	
	public void reload(final ReloadCallback callback) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					downloadFile(imageURL, getCacheFile());
					FileInputStream fis = new FileInputStream(getCacheFile());
					WallpaperManager.getInstance(context).setStream(fis);
					if (imageInFavorites()) { // Refresh favorite by toggling twice
						getFavoriteFile().delete();
						toggleFavorite();
					}
				} catch (Exception e) {
					// TODO handle?
					e.printStackTrace();
				}
				return null;
			}
			
			protected void onPostExecute(Void result) {
				callback.onFinish();
			}
			
		}.execute();
	}

	public boolean imageInCache() {
		return getCacheFile().exists();
	}

	public boolean imageInFavorites() {
		return getFavoriteFile().exists();
	}

	private void downloadFile(String url, File dst)
			throws MalformedURLException, IOException {
		transfer(new URL(url).openStream(), new FileOutputStream(dst));
	}

	private void copyFile(File src, File dst) throws IOException {
		transfer(new FileInputStream(src), new FileOutputStream(dst));
	}

	private void transfer(InputStream in, OutputStream out) throws IOException {
		try {
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			in.close();
			out.close();
		}
	}

	public void toggleFavorite() {
		if (imageInFavorites()) {
			com.akrolsmir.bakegami.WallpaperManager.with(context).removeFavorite(getFavoriteFile());
		} else {
			try {
				copyFile(getCacheFile(), getFavoriteFile());
				context.sendBroadcast(new Intent(
						Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
								.fromFile(getFavoriteFile())));
			} catch (IOException e) {
				// TODO handle?
				e.printStackTrace();
			}
		}
	}

	public File getCacheFile() {
		return new File(CACHE_DIR, imageName);
	}

	public File getFavoriteFile() {
		return new File(PIC_DIR, imageName);
	}

	public static List<File> getFavorites() {
		File[] files = PIC_DIR.listFiles();

		if (files == null)
			files = new File[0];

		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return Long.valueOf(f2.lastModified()).compareTo(
						f1.lastModified());
			}
		});

		return Arrays.asList(files);
	}
}
