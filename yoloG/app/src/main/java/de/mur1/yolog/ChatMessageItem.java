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

public class ChatMessageItem {
    Context         context;
    View            view;
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
        if (padding) tv.setPadding(5, 0, 5, 0);
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

    public ChatMessageItem(View view, IRCMessage irc_m) {
        this.view = view;
        this.context = this.view.getContext();
        this.messageFlexbox = this.view.findViewById(R.id.messageFlexbox);

        this.view.setBackgroundColor(ThemeManager.active_theme.background_color);

        clearMessageFlexbox();

        addMessageFlexboxText(irc_m.display_time, ThemeManager.active_theme.text_color, ThemeManager.active_theme.background_color, true, false);
        addMessageFlexboxText(irc_m.channel, ThemeManager.active_theme.text_color, ThemeManager.active_theme.background_color, true, true);

        for (int b = 0; b < irc_m.badge_replace.size(); b++) {
            BadgeReplace b_repl = irc_m.badge_replace.get(b);
            Emote e = null;
            if (irc_m.channel_id != -1L) {
                e = BadgeAdapter.getBadge(irc_m.channel_id, b_repl.name, b_repl.version);
            }
            if (e != null) {
                if (!e.animated) {
                    addMessageFlexboxImage((Bitmap) e.data, true);
                }
            } else {
                addMessageFlexboxText(b_repl.name + "/" + b_repl.version, ThemeManager.active_theme.text_color, ThemeManager.active_theme.background_color, true, false);
            }
        }

        int diff_s = Util.color_diff(irc_m.name_color, ThemeManager.active_theme.background_color);
        int diff_i = Util.color_diff(irc_m.name_color, ThemeManager.active_theme.background_color_inv);
        if (diff_s >= diff_i) {
            addMessageFlexboxText(irc_m.display_name, irc_m.name_color, ThemeManager.active_theme.background_color, true, true);
        } else {
            addMessageFlexboxText(irc_m.display_name, irc_m.name_color, ThemeManager.active_theme.background_color_inv, true, true);
        }

        if (irc_m.message != null) {
            if (irc_m.emote_replace.size() == 0) {
                addMessageFlexboxText(irc_m.message, ThemeManager.active_theme.text_color, ThemeManager.active_theme.background_color, true, false);
            } else {
                int message_position = 0;
                for (int e_r = 0; e_r < irc_m.emote_replace.size(); e_r++) {
                    EmoteReplace e_repl = irc_m.emote_replace.get(e_r);
                    Emote e = EmoteAdapter.getEmote(e_repl.id);
                    if (e_repl.range_start > message_position) {
                        addMessageFlexboxText(irc_m.message.substring(message_position, e_repl.range_start), ThemeManager.active_theme.text_color, ThemeManager.active_theme.background_color, false, false);
                        message_position = e_repl.range_start;
                    }
                    if (e != null) {
                        if (!e.animated) {
                            addMessageFlexboxImage((Bitmap) e.data, false);
                        }
                    } else {
                        addMessageFlexboxText(irc_m.message.substring(message_position, e_repl.range_end + 1), ThemeManager.active_theme.text_color, ThemeManager.active_theme.background_color, false, false);
                    }
                    message_position = e_repl.range_end + 2;
                }
                if (message_position < irc_m.message.length()) {
                    addMessageFlexboxText(irc_m.message.substring(message_position), ThemeManager.active_theme.text_color, ThemeManager.active_theme.background_color,false, false);
                }
            }
        }
    }


}
