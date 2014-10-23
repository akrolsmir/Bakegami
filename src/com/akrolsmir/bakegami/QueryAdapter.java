package com.akrolsmir.bakegami;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class QueryAdapter extends ArrayAdapter<String> {
  private final Context context;
  private final ArrayList<String> values;

  public QueryAdapter(Context context, ArrayList<String> values) {
    super(context, R.layout.query, values);
    this.context = context;
    this.values = values;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View rowView = inflater.inflate(R.layout.query, parent, false);
    TextView textView = (TextView) rowView.findViewById(R.id.firstLine);
    ImageButton imageBtn = (ImageButton) rowView.findViewById(R.id.icon);
    TextView textView2 = (TextView) rowView.findViewById(R.id.secondLine);
    // Change the icon for Windows and iPhone
    String s = values.get(position);
    if (s.startsWith("r")){
    	textView.setText("r/"+values.get(position).substring(1));
    	textView2.setText("Subreddit");
      imageBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
    } else if(s.startsWith("q")){
    	textView.setText(values.get(position).substring(1));
    	textView2.setText("Tag");
      imageBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
    } else{
    	textView.setText(values.get(position));
    	textView2.setVisibility(View.GONE);
    	imageBtn.setImageResource(android.R.drawable.ic_menu_add);
    }

    return rowView;
  }
} 
