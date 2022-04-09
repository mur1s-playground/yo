package de.mur1.yolog;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class ChannelFilterAdapter extends ArrayAdapter<String> {
    private Context context;
    private String[] items;

    private static Semaphore lock = new Semaphore(1, false);
    private static ArrayMap<String, Boolean> channel_filter_enabled = new ArrayMap<String, Boolean>();

    public ChannelFilterAdapter(Context context, String items[]) {
        super(context, R.layout.channel_filter_spinner_item);
        this.context = context;
        this.items = items;
        for (int c = 0; c < items.length; c++) {
            setChannelEnabled(items[c], true);
        }
    }

    public static boolean isChannelEnabled(String name) {
        boolean result = true;
        try {
            lock.acquire();
            if (channel_filter_enabled.containsKey(name)) {
                result = channel_filter_enabled.get(name);
            }
            lock.release();
        } catch (Exception e) {}
        return result;
    }

    public static void setChannelEnabled(String name, boolean enabled) {
        try {
            lock.acquire();
            if (channel_filter_enabled.containsKey(name)) {
                int idx = channel_filter_enabled.indexOfKey(name);
                channel_filter_enabled.setValueAt(idx, enabled);
            } else {
                channel_filter_enabled.put(name, enabled);
            }
            lock.release();
        } catch (Exception e) {}
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        CheckBox chkbox = null;

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.channel_filter_spinner_item, null);

            chkbox = convertView.findViewById(R.id.checkBoxChannelFilter);
            convertView.setTag(chkbox);
        } else {
            chkbox = (CheckBox) convertView.getTag();
        }
        chkbox.setText(items[position]);
        chkbox.setChecked(isChannelEnabled(items[position]));
        chkbox.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float position = motionEvent.getAxisValue(MotionEvent.AXIS_X);
                int width = view.getWidth();
                CheckBox cb = (CheckBox) view;
                if (position >= 0.5*(float)width) {
                    for (int c = 0; c < parent.getChildCount(); c++) {
                        ((CheckBox) parent.getChildAt(c).getTag()).setChecked(false);
                    }
                    for (int cf_e = 0; cf_e < channel_filter_enabled.size(); cf_e++) {
                        String key = channel_filter_enabled.keyAt(cf_e);
                        if (key.equals(cb.getText().toString())) {
                            ChannelFilterAdapter.setChannelEnabled(key, true);
                        } else {
                            ChannelFilterAdapter.setChannelEnabled(key, false);
                        }
                    }
                } else {
                    ChannelFilterAdapter.setChannelEnabled(cb.getText().toString(), !cb.isChecked());
                }
                return false;
            }
        });

        return convertView;
    }

    @Override
    public int getCount() {
        return this.items.length;
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}
