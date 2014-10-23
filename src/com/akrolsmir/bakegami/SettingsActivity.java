package com.akrolsmir.bakegami;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	private final ArrayList<String> vals = new ArrayList<String>();
	private SharedPreferences prefs;
	private int numEntries;
	ListView listview;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Preference button = (Preference)findPreference("pref_subreddits");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) { 
                            Intent intent = new Intent(SettingsActivity.this,QueryActivity.class);
                            startActivity(intent);
                            return true;
                        }
                    });
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
    
	public static String KEY_PREF_SHOW_NSFW = "pref_show_nsfw";
	public static String KEY_PREF_ALLOW_DATA = "pref_allow_data";
	public static String KEY_PREF_SUBREDDITS_MAIN = "pref_subreddits";
    
	
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
		if(key.equals(KEY_PREF_ALLOW_DATA) && sp.getBoolean(KEY_PREF_ALLOW_DATA, false) == true)
			WallpaperManager.with(this).fetchNextUrls();
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
		if (findPreference(KEY_PREF_SUBREDDITS_MAIN) != null
				&& findPreference(KEY_PREF_SUBREDDITS_MAIN) instanceof PreferenceScreen) {
			addBack((PreferenceScreen) findPreference(KEY_PREF_SUBREDDITS_MAIN));
		}

	}

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
    

}
