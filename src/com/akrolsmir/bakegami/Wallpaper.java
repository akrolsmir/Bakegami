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

import android.app.WallpaperManager;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class Wallpaper {

	//	private Bitmap picture;
	private Context context;
	private String imageURL;
	private String imageName;

	private File CACHE_DIR, PIC_DIR;

	public Wallpaper(Context context, String imageURL) {
		this.context = context;
		this.imageURL = imageURL;
		this.imageName = imageURL.substring(imageURL.lastIndexOf('/'));

		CACHE_DIR = context.getExternalCacheDir();
		PIC_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		PIC_DIR = new File(PIC_DIR, "bakegami");
		CACHE_DIR.mkdirs();
		PIC_DIR.mkdirs();
	}

	Runnable runnable;

	public void cache() {
		if (!imageInCache()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// Download stuff to cache folder
						downloadImage();
					} catch (Exception e) {
					}
				}
			}).start();
		}
	}

	public void setAsBackground() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (!imageInCache())
						downloadImage();

					// Grab the picture from cache
					WallpaperManager wpm = WallpaperManager.getInstance(context);
					FileInputStream fis = new FileInputStream(getCacheFile());
					wpm.setStream(fis);

					Log.d("Changed wallpaper", imageURL);

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private boolean imageInCache() {
//		Log.d("imageInCache", CACHE_DIR.getAbsolutePath() + imageName);
		File imageFile = new File(CACHE_DIR, imageName);
		return imageFile.exists();
	}

	private void downloadImage() throws MalformedURLException, IOException {
		InputStream input = null;
		try {
			OutputStream output = new FileOutputStream(new File(CACHE_DIR, imageName));

			input = new URL(imageURL).openStream();
//			Log.d("downloadImage", "Downloading " + imageURL);
			try {
				byte[] buffer = new byte[8192];
				int bytesRead = 0;
				while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
					output.write(buffer, 0, bytesRead);
				}
			} finally {
				output.close();
			}
		} finally {
			input.close();
//			Log.d("downloadImage", "Finished " + imageURL);
		}
	}

	//UGLY UGLY CODE DUPLICATION. F*** JAVA I/O
	private void copy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);
		
//		Log.d("copy", "Copying " + imageURL);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
//		Log.d("copy", "Finished " + imageURL);
	}

	public void favorite() {
		try {
			copy(new File(CACHE_DIR, imageName), new File(PIC_DIR, imageName));
			Log.d("PIC", PIC_DIR.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public File getCacheFile(){
		return new File(CACHE_DIR, imageName);
	}
}
