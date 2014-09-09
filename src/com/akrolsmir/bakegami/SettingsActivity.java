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
    
	public static String[] KEY_PREF_SUBREDDITS = {"sr0","sr1","sr2","sr3","sr4","sr5","sr6",
		"sr7","sr8","sr9"}; 
	public static String KEY_PREF_SHOW_NSFW = "pref_show_nsfw";
    
    @Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
    	// Set summary to be the user-description for the selected value
    		for( String k : KEY_PREF_SUBREDDITS)
    			if (key.equals(k)) { 
    				findPreference(key).setSummary("r/" + sp.getString(key, ""));
				WallpaperManager.with(this).resetQueue();
				break;
    			}
	}
    
    public static String getSubreddit(Context context, int index) {
    	return with(context).getString(KEY_PREF_SUBREDDITS[index], "");
    }
    
    public static boolean showNSFW(Context context) {
    	return with(context).getBoolean(KEY_PREF_SHOW_NSFW, false);
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
