package com.akrolsmir.bakegami;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
    
	public static String KEY_PREF_SUBREDDIT = "pref_subreddit", 
			KEY_PREF_SHOW_NSFW = "pref_show_nsfw";
    
    @Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
    	// Set summary to be the user-description for the selected value
    	if (key.equals(KEY_PREF_SUBREDDIT)) {
			findPreference(key).setSummary("r/" + sp.getString(key, ""));
			WallpaperManager.with(this).resetQueue();
		}
	}
    
    public static String getSubreddit(Context context) {
    	return with(context).getString(KEY_PREF_SUBREDDIT, "");
    }
    
    public static boolean showNSFW(Context context) {
    	return with(context).getBoolean(KEY_PREF_SUBREDDIT, false);
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
        
        // Hackily updates the summary
        findPreference(KEY_PREF_SUBREDDIT).setSummary("r/" + getSubreddit(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
    

}
