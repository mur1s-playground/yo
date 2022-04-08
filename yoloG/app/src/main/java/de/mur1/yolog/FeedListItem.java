package de.mur1.yolog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

public class FeedListItem {
    Context         context;
    View            view;
    TextView        timeView;
    FlexboxLayout   messageFlexbox;

    public void addMessageFlexboxText(String text, int color, int background_color, boolean padding, boolean bold) {
        TextView tv = new TextView(context);
        tv.setTextColor(color);
        tv.setBackgroundColor(background_color);
        tv.setText(text);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        messageFlexbox.addView(tv);
        ViewGroup.LayoutParams l_params = tv.getLayoutParams();
        l_params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        l_params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (padding) tv.setPadding(0, 0, 5, 0);
        tv.setLayoutParams(l_params);
    }

    public void addMessageFlexboxImage(Bitmap bm, boolean is_badge) {
        ImageView iv = new ImageView(context);
        iv.setImageBitmap(bm);
        messageFlexbox.addView(iv);
        ViewGroup.LayoutParams l_params = iv.getLayoutParams();
        l_params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        l_params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (is_badge) iv.setPadding(2, 5, 2, 5);
        iv.setLayoutParams(l_params);
    }

    public void clearMessageFlexbox() {
        messageFlexbox.removeAllViews();
    }

    public FeedListItem(View view, IRCMessage irc_m) {
        this.view = view;
        this.context = this.view.getContext();
        this.messageFlexbox = this.view.findViewById(R.id.messageFlexbox);

        clearMessageFlexbox();

        this.view.setBackgroundColor(ThemeManager.active_theme.background_color);

        addMessageFlexboxText(irc_m.display_time, ThemeManager.active_theme.text_color, ThemeManager.active_theme.background_color, true, false);
/*
        int diff_s = Util.color_diff(irc_m.name_color, ThemeManager.active_theme.background_color);
        int diff_i = Util.color_diff(irc_m.name_color, ThemeManager.active_theme.background_color_inv);
        if (diff_s >= diff_i) {
            addMessageFlexboxText(irc_m.display_name, irc_m.name_color, ThemeManager.active_theme.background_color, true, true);
        } else {
            addMessageFlexboxText(irc_m.display_name, irc_m.name_color, ThemeManager.active_theme.background_color_inv, true, true);
        }
*/
        if (irc_m.ban_duration == 0) {
            addMessageFlexboxText("BAN", Color.parseColor("#FF0000"), ThemeManager.active_theme.background_color, true, true);
        } else {
            addMessageFlexboxText("TIMEOUT", Color.parseColor("#FF8800"), ThemeManager.active_theme.background_color, true, true);
            String time = "";
            int minutes = irc_m.ban_duration / 60;
            if (minutes > 0) {
                time += minutes + "m ";
            }
            int seconds = irc_m.ban_duration - minutes * 60;
            if (seconds > 0) {
                time += seconds + "s";
            }
            addMessageFlexboxText(time , ThemeManager.active_theme.text_color, ThemeManager.active_theme.background_color, true, true);
        }

        addMessageFlexboxText(irc_m.message, ThemeManager.active_theme.text_color, ThemeManager.active_theme.background_color, true, false);
    }


}
