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
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

public class Wallpaper {

	private Context context;
	private String imageURL;
	private String imageName;

	private File CACHE_DIR;
	private static File PIC_DIR = new File(
			Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
			"bakegami"); // TODO replace with name of app

	public Wallpaper(Context context, String imageURL) {
		this.context = context;
		this.imageURL = imageURL.split(Pattern.quote("|"))[0];
		this.imageName = imageURL.split(Pattern.quote("|"))[1];
		CACHE_DIR = context.getExternalCacheDir();
		CACHE_DIR.mkdirs();
		PIC_DIR.mkdirs();
	}

	public void cache() {
		if (!imageInCache()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// Download stuff to cache folder
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
					WallpaperManager wpm = WallpaperManager
							.getInstance(context);
					if (!imageInCache() && imageInFavorites())
			            copyFile(getFavoriteFile(), getCacheFile());
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
			removeFavorite(getFavoriteFile(), context);
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

	public static void removeFavorite(File f, Context cont) {
		String canonicalPath = "";
		try {
			canonicalPath = f.getCanonicalPath();
		} catch (IOException e) {
			canonicalPath = f.getAbsolutePath();
		}
		if (!com.akrolsmir.bakegami.WallpaperManager
				.with(cont)
				.getCurrentWallpaperURL()
				.contains(
						canonicalPath.substring(canonicalPath.lastIndexOf("/") + 1)))
			com.akrolsmir.bakegami.WallpaperManager.with(cont)
					.removeInfo(
							canonicalPath.substring(canonicalPath
									.lastIndexOf("/") + 1));
		final Uri uri = MediaStore.Files.getContentUri("external");
		final int result = cont.getContentResolver().delete(uri,
				MediaStore.Files.FileColumns.DATA + "=?",
				new String[] { canonicalPath });
		if (result == 0) {
			final String absolutePath = f.getAbsolutePath();
			if (!absolutePath.equals(canonicalPath)) {
				cont.getContentResolver().delete(uri,
						MediaStore.Files.FileColumns.DATA + "=?",
						new String[] { absolutePath });
			}
		}
	}
}
