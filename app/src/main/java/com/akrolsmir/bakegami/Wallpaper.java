package com.akrolsmir.bakegami;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

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

	/**
	 * Default wallpaper for the first run of the app.
	 */
	public Wallpaper(Context context) {
		this.context = context;
		this.imageURL = "http://pixabay.com/get/9a1e9044203f49270547/1414394710/spring-179584_1280.jpg";
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
						else {
							downloadFile(imageURL, getCacheFile());
							//resize();
						}
					} catch (IOException e) {
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
					// Log.d("Changed wallpaper", imageURL);
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
					//resize();
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
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.addRequestProperty("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.45 Safari/535.19");
		transfer(conn.getInputStream(), new FileOutputStream(dst));
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
				context.sendBroadcast(
						new Intent(
								Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
								Uri.fromFile(getFavoriteFile())));
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

	/*private void resize(){
		WallpaperManager wpm = WallpaperManager.getInstance(context);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		//Returns null, sizes are in the options variable
		BitmapFactory.decodeFile(getCacheFile().getAbsolutePath(), options);
		int width = options.outWidth;
		int height = options.outHeight;
		DisplayMetrics dm = new DisplayMetrics();
		((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenWidth = wpm.getDesiredMinimumWidth();
		int screenHeight = wpm.getDesiredMinimumHeight();
		if(screenWidth < 0 || screenHeight < 0)
		{
			screenWidth = dm.widthPixels;
			screenHeight = dm.heightPixels;
		}
		double screenWidthInInches = (double)screenWidth/(double)dm.densityDpi;
		double screenHeightInInches = (double)screenHeight/(double)dm.densityDpi;
		int maxDim = Math.max(screenWidth,screenHeight);
		screenWidth /= (double)dm.densityDpi;
		screenHeight /= (double)dm.densityDpi;
		if((Math.pow(width, 2)+Math.pow(height, 2))/(Math.pow(screenWidthInInches, 2)+Math.pow(screenHeightInInches,2)) < 10000){
			com.akrolsmir.bakegami.WallpaperManager.with(context).dequeue(imageURL+"|"+imageName);
		}
		else if( width > maxDim && height > maxDim){
			Bitmap bmp, scaledBitmap;
			try{
			options = new BitmapFactory.Options();
			options.inJustDecodeBounds = false;
			options.inDither = false;
			options.inSampleSize = Math.min(width/maxDim, height/maxDim);
			options.inScaled=false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			bmp = BitmapFactory.decodeFile(getCacheFile().getAbsolutePath(), options);
			width = bmp.getWidth();
			height = bmp.getHeight();
			Matrix matrix = new Matrix();
			matrix.postScale((float)maxDim/(float)Math.min(width,height), (float)maxDim/(float)Math.min(width,height));
			scaledBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
			bmp = null;
			}
			catch(Exception e){
				return;
			}
			FileOutputStream out = null;
			try {
				String filename = getCacheFile().getAbsolutePath();
				getCacheFile().delete();
				out = new FileOutputStream(filename);
				scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (out != null) {
						out.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}*/
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

	public void crop(Context cont) {
		WallpaperManager wpm = WallpaperManager.getInstance(context);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			// Try to use the built in crop for KitKat and up
			try {
				Uri contUri = Uri.parse(MediaStore.Images.Media.insertImage(
						cont.getContentResolver(),
						getCacheFile().getAbsolutePath(),
						null,
						null));
				Intent cropSetIntent = wpm.getCropAndSetWallpaperIntent(contUri);
				cropSetIntent.setDataAndType(contUri, "image/*");
				cont.startActivity(cropSetIntent);
			} catch (Exception e) {
				backupCrop(wpm, cont);
				e.printStackTrace();
			}
		} else {
			backupCrop(wpm, cont);
		}
	}

	private void backupCrop(WallpaperManager wpm, Context cont) {
		Intent cropIntent = new Intent("com.android.camera.action.CROP");
		cropIntent.setDataAndType(Uri.fromFile(getCacheFile()), "image/*");
		cropIntent.setClassName(
				"com.google.android.apps.plus",
				"com.google.android.apps.photoeditor.fragments.PlusCropActivityAlias");
		cropIntent.putExtra("crop", "true");
		cropIntent.putExtra("aspectX", wpm.getDesiredMinimumWidth());
		cropIntent.putExtra("aspectY", wpm.getDesiredMinimumHeight());
		cropIntent.putExtra("outputX", wpm.getDesiredMinimumWidth());
		cropIntent.putExtra("outputY", wpm.getDesiredMinimumHeight());
		cropIntent.putExtra("return-data", true);
		try {
			((Activity) cont).startActivityForResult(cropIntent, 1);
		} catch (ActivityNotFoundException anfe) {
			try {
				cropIntent.setClassName(
						"com.google.android.apps.plus",
						"com.google.android.apps.photoeditor.fragments.PlusCropActivity");
				((Activity) cont).startActivityForResult(cropIntent, 1);
			} catch (ActivityNotFoundException anfe2) {
				Toast.makeText(context, "Cropping requires the latest Google+.", Toast.LENGTH_LONG).show();
			}
		}
	}
}
