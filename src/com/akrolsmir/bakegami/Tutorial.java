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
		builder.setTitle("Managing Settings")
				.setMessage(
						"You can change subreddits and other settings using your phone's settings button or the button on the right of the header.")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				});
		builder.show();
	}
}
