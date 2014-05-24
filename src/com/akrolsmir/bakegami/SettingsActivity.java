package com.akrolsmir.bakegami;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
    
	public static String KEY_PREF_UPDATE_FREQ = "pref_update_freq",
			KEY_PREF_UPDATE_BLOCK = "pref_update_block",
			KEY_PREF_SUBREDDIT = "pref_subreddit";
    
    @Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		if (key.equals(KEY_PREF_UPDATE_FREQ) || key.equals(KEY_PREF_UPDATE_BLOCK)) {
			Preference connectionPref = findPreference(key);
			// Set summary to be the user-description for the selected value
			connectionPref.setSummary(sp.getString(key, ""));
			
			String summary = "Every "
					+ sp.getString(KEY_PREF_UPDATE_FREQ, "") + " "
					+ ((ListPreference)findPreference(KEY_PREF_UPDATE_BLOCK)).getEntry();
			findPreference(KEY_PREF_UPDATE_FREQ).setSummary(summary);
			findPreference(KEY_PREF_UPDATE_BLOCK).setSummary(summary);
		} else if (key.equals(KEY_PREF_SUBREDDIT)) { 
			findPreference(key).setSummary("r/" + sp.getString(key, ""));
		}
	}
    
    public static SharedPreferences with(Context context) {
    	return PreferenceManager.getDefaultSharedPreferences(context);
    }
    
	public static long getRefreshSeconds(Context context) {
		return 60 * Long.parseLong(with(context).getString(KEY_PREF_UPDATE_FREQ, "1"))
				* Long.parseLong(with(context).getString(KEY_PREF_UPDATE_BLOCK, "1"));
	}
    
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        
        // TODO kind of hacky
        onSharedPreferenceChanged(with(this), KEY_PREF_UPDATE_BLOCK);
        onSharedPreferenceChanged(with(this), KEY_PREF_SUBREDDIT);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
    
//    public class FrequencyPickerPreference extends DialogPreference {
//        public FrequencyPickerPreference(Context context, AttributeSet attrs) {
//            super(context, attrs);
//            
//            setDialogLayoutResource(R.layout.frequencypicker_dialog);
//            setPositiveButtonText(android.R.string.ok);
//            setNegativeButtonText(android.R.string.cancel);
//            
//            setDialogIcon(null);
//        }
//        
//        @Override
//        protected void onDialogClosed(boolean positiveResult) {
//            // When the user selects "OK", persist the new value
//            if (positiveResult) {
//                persistInt(mNewValue);
//            }
//        }
//        
//        @Override
//        protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
//            if (restorePersistedValue) {
//                // Restore existing state
//                mCurrentValue = this.getPersistedInt(DEFAULT_VALUE);
//            } else {
//                // Set default state from the XML attribute
//                mCurrentValue = (Integer) defaultValue;
//                persistInt(mCurrentValue);
//            }
//        }
//        
//        @Override
//        protected Object onGetDefaultValue(TypedArray a, int index) {
//            return a.getInteger(index, DEFAULT_VALUE);
//        }
//    }
}
