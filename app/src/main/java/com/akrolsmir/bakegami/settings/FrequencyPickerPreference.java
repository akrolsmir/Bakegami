package com.akrolsmir.bakegami.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.akrolsmir.bakegami.R;
import com.akrolsmir.bakegami.WallpaperControlWidgetProvider.RefreshService;

import java.util.Arrays;
import java.util.List;

public class FrequencyPickerPreference extends DialogPreference {
	public FrequencyPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		setDialogLayoutResource(R.layout.frequencypicker_dialog);
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);

		setDialogIcon(null);
	}

	/* Constants and static methods */
	private final static List<CharSequence> TIMES =
			Arrays.asList(new CharSequence[]{"minutes", "hours", "days", "weeks"});
	private final static long[] TIMES_IN_MIN = {1, 60, 1440, 10080};

	private final static String DEFAULT_VALUE = "2 days";

	private static final String KEY = "pref_refresh_freq";

	public static long getRefreshSeconds(Context context) {
		String value = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, DEFAULT_VALUE);
		long num = Long.parseLong(value.split(" ")[0]);
		return (num == 0 ? 1 : num) * TIMES_IN_MIN[TIMES.indexOf(value.split(" ")[1])] * 60;
	}

	/* Instance variables and methods */
	private Spinner spinner;
	private EditText editText;
	private String[] values = DEFAULT_VALUE.split(" ");

	@Override
	protected View onCreateDialogView() {
		View view = super.onCreateDialogView();
		spinner = (Spinner) view.findViewById(R.id.spinner1);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
				getContext(), android.R.layout.simple_spinner_item, TIMES);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);

		editText = (EditText) view.findViewById(R.id.editText1);

		editText.setText(values[0]);
		spinner.setSelection(TIMES.indexOf(values[1]));
		return view;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		// When the user selects "OK", persist the new value
		if (positiveResult) {
			setValue(editText.getText() + " " + spinner.getSelectedItem());
		}
	}

	private void setValue(String value) {
		values = value.split(" ");
		if (values[0].equals(""))
			values[0] = "1";
		persistString(values[0] + " " + values[1]);
		setSummary(freqString() + (RefreshService.isCycling(getContext()) ? "" : " (currently paused)"));
		// Update the refresh time
		Intent intent = new Intent(getContext(), RefreshService.class);
		getContext().startService(intent.setAction(RefreshService.UPDATE));
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if (restorePersistedValue) {
			// Restore existing state
			setValue(getPersistedString(DEFAULT_VALUE));
		} else {
			// Set default state from the XML attribute
			setValue((String) defaultValue);
		}
	}

	private String freqString() {
		int num = Integer.parseInt(values[0]);
		return "Every " + (num <= 1 ? values[1].substring(0, values[1].length() - 1) : num + " " + values[1]);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

    /* Save and restore Preference state across restarts */

//    @Override
//    protected Parcelable onSaveInstanceState() {
//        final Parcelable superState = super.onSaveInstanceState();
//        // Check whether this Preference is persistent (continually saved)
//        if (isPersistent()) {
//            // No need to save instance state since it's persistent, use superclass state
//            return superState;
//        }
//
//        // Create instance of custom BaseSavedState
//        final SavedState myState = new SavedState(superState);
//        // Set the state's value with the class member that holds current setting value
//        myState.value = values[0] + " " + values[1];
//        return myState;
//    }
//
//    @Override
//    protected void onRestoreInstanceState(Parcelable state) {
//        // Check whether we saved the state in onSaveInstanceState
//        if (state == null || !state.getClass().equals(SavedState.class)) {
//            // Didn't save the state, so call superclass
//            super.onRestoreInstanceState(state);
//            return;
//        }
//
//        // Cast state to custom BaseSavedState and pass to superclass
//        SavedState myState = (SavedState) state;
//        super.onRestoreInstanceState(myState.getSuperState());
//        
//        // Set this Preference's widget to reflect the restored state
//        setValue(myState.value);
//    }
//    
//    
//    private static class SavedState extends BaseSavedState {
//        // Member that holds the setting's value
//        // Change this data type to match the type saved by your Preference
//        String value;
//
//        public SavedState(Parcelable superState) {
//            super(superState);
//        }
//
//        public SavedState(Parcel source) {
//            super(source);
//            // Get the current preference's value
//            value = source.readString();  // Change this to read the appropriate data type
//        }
//
//        @Override
//        public void writeToParcel(Parcel dest, int flags) {
//            super.writeToParcel(dest, flags);
//            // Write the preference's value
//            dest.writeString(value);  // Change this to write the appropriate data type
//        }
//
//        // Standard creator object using an instance of this class
//        public static final Parcelable.Creator<SavedState> CREATOR =
//                new Parcelable.Creator<SavedState>() {
//
//            public SavedState createFromParcel(Parcel in) {
//                return new SavedState(in);
//            }
//
//            public SavedState[] newArray(int size) {
//                return new SavedState[size];
//            }
//        };
//    }
}
