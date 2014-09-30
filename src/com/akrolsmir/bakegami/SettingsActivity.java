package com.akrolsmir.bakegami;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        View homeBtn = findViewById(android.R.id.home);
        if( homeBtn != null ){
        	OnClickListener dismissClickListener = new OnClickListener(){
        		@Override
        		public void onClick(View v){
        			SettingsActivity.this.finish();
        		}
        	};
        	ViewParent homeBtnContainer = homeBtn.getParent();

            // The home button is an ImageView inside a FrameLayout
            if (homeBtnContainer instanceof FrameLayout) {
                ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();

                if (containerParent instanceof LinearLayout) {
                    // This view also contains the title text, set the whole view as clickable
                    ((LinearLayout) containerParent).setOnClickListener(dismissClickListener);
                } else {
                    // Just set it on the home button
                    ((FrameLayout) homeBtnContainer).setOnClickListener(dismissClickListener);
                }
            } else {
                // The 'If all else fails' default case
                homeBtn.setOnClickListener(dismissClickListener);
            }
        }
    }
    
	public static String[] KEY_PREF_SUBREDDITS = {"sr0","sr1","sr2","sr3","sr4","sr5","sr6",
		"sr7","sr8","sr9"}; 
	public static String KEY_PREF_SHOW_NSFW = "pref_show_nsfw";
	public static String KEY_PREF_ALLOW_DATA = "pref_allow_data";
    
	private void addBack(PreferenceScreen preferenceScreen){
		final Dialog dialog = preferenceScreen.getDialog();
		if (dialog != null) {
			dialog.getActionBar().setDisplayHomeAsUpEnabled(true);
			View homeBtn = dialog.findViewById(android.R.id.home);
			if (homeBtn != null) {
				OnClickListener dismissClickListener = new OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				};
				ViewParent homeBtnContainer = homeBtn.getParent();

				// The home button is an ImageView inside a FrameLayout
				if (homeBtnContainer instanceof FrameLayout) {
					ViewGroup containerParent = (ViewGroup) homeBtnContainer
							.getParent();

					if (containerParent instanceof LinearLayout) {
						// This view also contains the title text, set the whole
						// view as clickable
						((LinearLayout) containerParent)
								.setOnClickListener(dismissClickListener);
					} else {
						// Just set it on the home button
						((FrameLayout) homeBtnContainer)
								.setOnClickListener(dismissClickListener);
					}
				} else {
					// The 'If all else fails' default case
					homeBtn.setOnClickListener(dismissClickListener);
				}
			}
		}
	}
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference){
		if(preference instanceof PreferenceScreen)
		{
			addBack((PreferenceScreen)preference);
		}
		return true;
	}
    @Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
    	// Set summary to be the user-description for the selected value
    		for( String k : KEY_PREF_SUBREDDITS)
    			if (key.equals(k)) { 
    				findPreference(key).setSummary("r/" + sp.getString(key, ""));
    				WallpaperManager.with(this).resetQueue();
    				return;
    			}
    		if(key.equals(KEY_PREF_ALLOW_DATA) && sp.getBoolean(KEY_PREF_ALLOW_DATA, false) == true)
    			WallpaperManager.with(this).fetchNextUrls();
	}
    
    public static String getSubreddit(Context context, int index) {
    	return with(context).getString(KEY_PREF_SUBREDDITS[index], "");
    }
    
    public static boolean showNSFW(Context context) {
    	return with(context).getBoolean(KEY_PREF_SHOW_NSFW, false);
    }
    
    public static boolean allowData(Context context){
    	return with(context).getBoolean(KEY_PREF_ALLOW_DATA, true);
    }
    
    public static long getRefreshSeconds(Context context) {
    	return FrequencyPickerPreference.getRefreshSeconds(context);
    }
    
    private static SharedPreferences with(Context context) {
    	return PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        if(findPreference("nestedKey") != null && findPreference("nestedKey") instanceof PreferenceScreen){
        	addBack((PreferenceScreen)findPreference("nestedKey"));
        }
        
        // Hackily updates the summary
	for( int i = 0; i < 10; i++ )
        	findPreference(KEY_PREF_SUBREDDITS[i]).setSummary("r/" + getSubreddit(this,i));
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
    

}
