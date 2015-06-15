package com.akrolsmir.bakegami.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.akrolsmir.bakegami.R;
import com.akrolsmir.bakegami.WallpaperManager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SortPreference extends DialogPreference implements OnItemSelectedListener {

	private Spinner sortSpinner;
	private Spinner timeSpinner;
	private TextView text;
	private final static List<CharSequence> SORT =
			Arrays.asList(new CharSequence[]{"Hot", "New", "Rising", "Controversial", "Top"});
	private final static List<CharSequence> TIME =
			Arrays.asList(new CharSequence[]{"this hour", "today", "this week", "this month", "this year", "all time"});
	private final static String DEFAULT_VALUE = "Hot";
	private final static String KEY = "pref_sort";
	private String values = DEFAULT_VALUE;

	public SortPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.sort_dialog);
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);

		setDialogIcon(null);
	}

	@Override
	protected View onCreateDialogView() {
		View view = super.onCreateDialogView();
		sortSpinner = (Spinner) view.findViewById(R.id.sortSpinner);
		timeSpinner = (Spinner) view.findViewById(R.id.timeSpinner);
		text = (TextView) view.findViewById(R.id.separatorText);
		ArrayAdapter<CharSequence> adapter1 = new ArrayAdapter<CharSequence>(
				getContext(), android.R.layout.simple_spinner_item, SORT);
		ArrayAdapter<CharSequence> adapter2 = new ArrayAdapter<CharSequence>(
				getContext(), android.R.layout.simple_spinner_item, TIME);
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sortSpinner.setAdapter(adapter1);
		sortSpinner.setOnItemSelectedListener(this);
		timeSpinner.setAdapter(adapter2);
		if (values.contains("from")) {
			sortSpinner.setSelection(SORT.indexOf(values.split(" ")[0]));
			timeSpinner.setSelection(TIME.indexOf(values.substring(values.indexOf("from") + 5)));
			timeSpinner.setVisibility(View.VISIBLE);
			text.setVisibility(View.VISIBLE);
		} else {
			sortSpinner.setSelection(SORT.indexOf(values));
			timeSpinner.setVisibility(View.INVISIBLE);
			text.setVisibility(View.INVISIBLE);
		}
		return view;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		// When the user selects "OK", persist the new value
		if (positiveResult) {
			if (text.getVisibility() == View.VISIBLE)
				setValue(sortSpinner.getSelectedItem() + " " + text.getText() + " " + timeSpinner.getSelectedItem());
			else
				setValue(sortSpinner.getSelectedItem() + "");
			WallpaperManager.with(getContext()).resetQueue();
		}
	}

	private void setValue(String value) {
		values = value;
		persistString(value);
		setSummary(value);
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

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
							   long id) {
		String sort = sortSpinner.getSelectedItem() + "";
		if (sort.equals("Top") || sort.equals("Controversial")) {
			if (text.getVisibility() == View.INVISIBLE) {
				timeSpinner.setSelection(TIME.indexOf("today"));
				timeSpinner.setVisibility(View.VISIBLE);
				text.setVisibility(View.VISIBLE);
			}
		} else {
			timeSpinner.setVisibility(View.INVISIBLE);
			text.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub

	}

	public static String[] getValues(Context context) {
		String value = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, DEFAULT_VALUE);
		String[] values;
		if (value.contains("from")) {
			values = new String[2];
			values[0] = (value.split(" ")[0]).toLowerCase(Locale.getDefault());
			String time = value.substring(value.indexOf("from") + 5);
			if (time.equals("today"))
				values[1] = "day";
			else if (time.equals("all time"))
				values[1] = "all";
			else
				values[1] = time.split(" ")[1];
		} else {
			values = new String[1];
			values[0] = value.toLowerCase(Locale.getDefault());
		}

		return values;
	}
}
