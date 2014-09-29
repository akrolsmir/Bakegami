package com.akrolsmir.bakegami;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

public class Tutorial {

	public static void onFirst(final Context context, final SharedPreferences prefs) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Tutorial")
				.setMessage(
				"As this appears to be your first time opening the app, would you like an explanation of how to navigate it?")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						partOne(context);
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								prefs.edit().putBoolean( MainActivity.CONTINUE_TUTORIAL,false).apply();
								((Activity) context).findViewById(R.id.pausePlayButton).performClick();	
							}
						})
				.setCancelable(false);
		builder.show();
	}

	private static void partOne(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Tutorial")
				.setMessage(
						"First go to the \"Settings\" menu either by pressing the Settings button on your device or the button displayed on the right side of the app's header. Here, settings such as subreddits and the time between wallpaper changes can be set. Once finished, return to the main screen and press the Play button.")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				})
				.setCancelable(false);
		builder.show();
	}
	public static void partTwo(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Tutorial")
				.setMessage(
						"Now that an image should be set as your wallpaper, other options should appear. The star button will allow you to save an image to favorites, the skip button will allow you to move to the next image, the crop button will allow you to re-crop your wallpaper, and the info button will give information about the image. Once you have an image favorited, you can set it as wallpaper by clicking it or hold your finger on it to open a menu of options.")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				})
				.setCancelable(false);
		builder.show();
	}
}
