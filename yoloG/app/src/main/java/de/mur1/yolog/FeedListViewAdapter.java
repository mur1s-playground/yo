package de.mur1.yolog;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class FeedListViewAdapter extends ArrayAdapter<IRCMessage> {
    Context context;
    ArrayList<IRCMessage> items;
    LayoutInflater layout_inflater;

    public FeedListViewAdapter(Context context, ArrayList<IRCMessage> items) {
        super(context, R.layout.chat_message, items);
        this.context = context;
        this.items = items;
        layout_inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layout_inflater.inflate(R.layout.chat_message, null);
        }
        FeedListItem fli = new FeedListItem(convertView, items.get(position));
        convertView.setTag(fli);
        return convertView;
    }

    public int getCount() {
        return items.size();
    }
}
