package de.mur1.yo;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;

import java.util.Vector;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private int current = 249;
    private IRCMessage[] dataSet;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private Context context;

        private final LinearLayout badgesLayout;
        private final TextView nameView;
        private final FlexboxLayout messageFlexbox;

        public ViewHolder(View view) {
            super(view);
            context = view.getContext();
            badgesLayout = (LinearLayout) view.findViewById(R.id.badgesLayout);
            nameView = (TextView) view.findViewById(R.id.nameView);
            messageFlexbox = (FlexboxLayout) view.findViewById(R.id.messageFlexbox);
        }

        public void clearBadgesLayout() {
            badgesLayout.removeAllViews();
        }

        public void addBadgesLayoutText(String text) {
            TextView tv = new TextView(context);
            tv.setText(text);
            badgesLayout.addView(tv);
            ViewGroup.LayoutParams l_params = tv.getLayoutParams();
            l_params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            l_params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            tv.setPadding(5, 5, 0, 5);
            tv.setLayoutParams(l_params);
        }

        public void addBadgesLayoutImage(Bitmap bm) {
            ImageView iv = new ImageView(context);
            iv.setImageBitmap(bm);
            badgesLayout.addView(iv);
            ViewGroup.LayoutParams l_params = iv.getLayoutParams();
            l_params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            l_params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            iv.setPadding(5, 5, 0, 5);
            iv.setLayoutParams(l_params);
        }

        public TextView getNameView() {
            return nameView;
        }

        public void clearMessageFlexbox() {
            messageFlexbox.removeAllViews();
        }

        public void addMessageFlexboxText(String text) {
            TextView tv = new TextView(context);
            tv.setText(text);
            messageFlexbox.addView(tv);
            ViewGroup.LayoutParams l_params = tv.getLayoutParams();
            l_params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            l_params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            tv.setLayoutParams(l_params);
        }

        public void addMessageFlexboxImage(Bitmap bm) {
            ImageView iv = new ImageView(context);
            iv.setImageBitmap(bm);
            messageFlexbox.addView(iv);
            ViewGroup.LayoutParams l_params = iv.getLayoutParams();
            l_params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            l_params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            iv.setLayoutParams(l_params);
        }
    }

    public void updateData(Long channel_id, Vector<UpdateData.BadgeReplace> badge_replace, String name, int name_color, String message, Vector<UpdateData.EmoteReplace> emote_replace) {
        IRCMessage tmp = dataSet[0];
        for (int i = 1; i <= 249; i++) {
            dataSet[i - 1] = dataSet[i];
        }
        dataSet[current] = tmp;
        dataSet[current].channel_id = channel_id;
        dataSet[current].badge_replace = badge_replace;
        dataSet[current].name = name;
        dataSet[current].message = message;
        dataSet[current].name_color = name_color;
        dataSet[current].emote_replace = emote_replace;
    }

    public ChatAdapter(IRCMessage[] dataSet) {
        this.dataSet = dataSet;
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.chat_message, viewGroup, false);
        return new ViewHolder(view);
    }

    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        viewHolder.clearBadgesLayout();
        for (int b = 0; b < dataSet[position].badge_replace.size(); b++) {
            UpdateData.BadgeReplace b_repl = dataSet[position].badge_replace.get(b);
            Emote e = null;
            if (dataSet[position].channel_id != -1L) {
                e = BadgeAdapter.getBadge(dataSet[position].channel_id, b_repl.name, b_repl.version);
            }
            if (e != null) {
                if (!e.animated) {
                    viewHolder.addBadgesLayoutImage((Bitmap) e.data);
                }
            } else {
                viewHolder.addBadgesLayoutText(b_repl.name + "/" + b_repl.version);
            }
        }

        viewHolder.getNameView().setText(dataSet[position].name);
        viewHolder.getNameView().setTextColor(dataSet[position].name_color);
        viewHolder.clearMessageFlexbox();
        if (dataSet[position].emote_replace.size() == 0) {
            viewHolder.addMessageFlexboxText(dataSet[position].message);
        } else {
            int message_position = 0;
            for (int e_r = 0; e_r < dataSet[position].emote_replace.size(); e_r++) {
                UpdateData.EmoteReplace e_repl = dataSet[position].emote_replace.get(e_r);
                Emote e = EmoteAdapter.getEmote(e_repl.id);
                if (e_repl.range_start > message_position) {
                    viewHolder.addMessageFlexboxText(dataSet[position].message.substring(message_position, e_repl.range_start));
                    message_position = e_repl.range_start;
                }
                if (e != null) {
                    if (!e.animated) {
                        viewHolder.addMessageFlexboxImage((Bitmap) e.data);
                    }
                } else {
                    viewHolder.addMessageFlexboxText(dataSet[position].message.substring(message_position, e_repl.range_end + 1));
                }
                message_position = e_repl.range_end + 1;
            }
            if (message_position < dataSet[position].message.length()) {
                viewHolder.addMessageFlexboxText(dataSet[position].message.substring(message_position));
            }
        }
    }

    public int getItemCount() {
        return dataSet.length;
    }
}
