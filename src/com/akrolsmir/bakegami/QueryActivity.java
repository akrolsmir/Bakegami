package com.akrolsmir.bakegami;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.UrlQuerySanitizer.ValueSanitizer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class QueryActivity extends Activity {

	private final ArrayList<String> vals = new ArrayList<String>();
	private SharedPreferences prefs;
	private int numEntries;
	ListView listview;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_query);
		prefs = getSharedPreferences("com.akrolsmir.bakegami.Query",0);
        listview = (ListView)findViewById(R.id.list);
        vals.add("+ Add a Subreddit");
        vals.add("+ Add a Tag");
        if(!prefs.contains("rq0")){
        	prefs.edit().putString("rq0","rwallpaper").apply();
        	prefs.edit().putString("rq1","qscenery").apply();
        }
        numEntries = 0;
        while(!(prefs.getString("rq"+numEntries, "").equals("")))
        {
        	vals.add(prefs.getString("rq"+numEntries, ""));
        	numEntries++;
        }
        prefs.edit().putInt("numEntries", numEntries).apply();
        listview.setAdapter(new QueryAdapter(this, vals));
        listview.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					final int position, long id) {
				Log.d("LOG",""+position);
			    final TextView textView = (TextView) view.findViewById(R.id.firstLine);
			    final TextView textView2 = (TextView) view.findViewById(R.id.secondLine);
			    int i = 0;
				if(position == 0){
					for(i = 0; i < numEntries;i++){
						if(prefs.getString("rq"+i, "").startsWith("q"))
							break;
					}
					editDialog(textView,"Subreddit",i+2);
				}
				else if(position ==1){
					editDialog(textView,"Tag",numEntries+2);
				}
				else{
					editDialog(textView,textView2.getText().toString(),position);
				}
			}
        	
        });
	}
	private void editDialog(final TextView textView, final String type, final int position){
		AlertDialog.Builder builder = new AlertDialog.Builder(QueryActivity.this);
		final EditText input = new EditText(QueryActivity.this);
		String title;
		if(textView.getText().toString().startsWith("+"))
		{
			title = "Add "+type;
			vals.add("");
			for(int i = numEntries; i > position-2;i--){
				prefs.edit().putString("rq"+i, prefs.getString("rq"+(i-1),"")).apply();
			}
			numEntries++;
		}
		else{
			title = "Edit "+type;
			input.setText(textView.getText());
		}
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		builder.setTitle(title)
				.setView(input)
				.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(INPUT_METHOD_SERVICE);
							    inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
								if(type.startsWith("S")){
									String value = input.getText().toString().substring(1+input.getText().toString().lastIndexOf("/"));
									if(value.equals("")){
										vals.remove(position);
										for(int i = position-2; i < numEntries-1;i++){
											prefs.edit().putString("rq"+i, prefs.getString("rq"+(i+1), "")).apply();
										}
										prefs.edit().remove("rq"+(numEntries-1)).apply();
										numEntries--;
									}
									else{
										vals.set(position, "r"+value);
										prefs.edit().putString("rq"+(position-2), "r"+value).apply();
									}
								}
								else{
									String value = input.getText().toString();
									if(value.equals("")){
										vals.remove(position);
										for(int i = position-2; i < numEntries-1;i++){
											prefs.edit().putString("rq"+i, prefs.getString("rq"+(i+1), "")).apply();
										}
										prefs.edit().remove("rq"+(numEntries-1)).apply();
										numEntries--;
									}
									else{
									vals.set(position, "q"+value);
									prefs.edit().putString("rq"+(position-2), "q"+value).apply();
									}
								}
								WallpaperManager.with(QueryActivity.this).resetQueue();
								listview.setAdapter(new QueryAdapter(QueryActivity.this, vals));
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(INPUT_METHOD_SERVICE);
							    inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
								dialog.cancel();
							}
						});
		AlertDialog ad = builder.create();
		ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		ad.show();
	}
	
	public static int numQueries(Context context){
    	return context.getSharedPreferences("com.akrolsmir.bakegami.Query",0).getInt("numEntries",0);
    }
	public static String getSubreddit(Context context, int index) {
    	return context.getSharedPreferences("com.akrolsmir.bakegami.Query",0).getString("rq"+index, "");
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.query, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
