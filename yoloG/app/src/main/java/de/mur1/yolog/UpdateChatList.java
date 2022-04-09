package de.mur1.yolog;

import android.app.Activity;
import android.content.Context;
import android.util.ArrayMap;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class UpdateChatList implements Runnable {
    ArrayMap<String, IRCMessage> room_state      = new ArrayMap<String, IRCMessage>();
    ArrayMap<String, IRCMessage> user_state      = new ArrayMap<String, IRCMessage>();

    Activity activity;
    Semaphore lock                              = new Semaphore(1, true);
    boolean         running                     = true;

    public String getUserstatePrefix(String channel) {
        String result = null;
        try {
            lock.acquire();
            if (user_state.containsKey(channel)) {
                result = user_state.get(channel).prefix;
            }
            lock.release();
        } catch (Exception e) {};
        return result;
    }

    public String getRoomstatePrefix(String channel) {
        String result = null;
        try {
            lock.acquire();
            if (room_state.containsKey(channel)) {
                result = room_state.get(channel).prefix;
            }
            lock.release();
        } catch (Exception e) {};
        return result;
    }

    public UpdateChatList(Activity activity) {
        this.activity = activity;
    }
    public void run() {
        while (running) {
            int update_interval = Settings.getIntSetting(Settings.SETTING_CHAT_UPDATE_INTERVAL_SECONDS);

            int[] available_chunks = new int[1];
            String chunk = MainActivity.update_chat.getChatChunk(available_chunks);
            if (chunk != null) {
                int[] type_counts = new int[4];
                ArrayList<IRCMessage> new_chunk = IRCMessage.newChunk(chunk, type_counts);

                try {
                    lock.acquire();

                    if (type_counts[IRCMessage.TYPE_USERSTATE] > 0 || type_counts[IRCMessage.TYPE_ROOMSTATE] > 0) {
                        int ct = 0;
                        for (int c = 0; c < new_chunk.size(); c++) {
                            if (new_chunk.get(c).type == IRCMessage.TYPE_USERSTATE) {
                                ct++;
                                if (user_state.containsKey(new_chunk.get(c).channel)) {
                                    int idx = user_state.indexOfKey(new_chunk.get(c).channel);
                                    user_state.setValueAt(idx, new_chunk.get(c));
                                } else {
                                    user_state.put(new_chunk.get(c).channel, new_chunk.get(c));
                                }
                            }
                            if (new_chunk.get(c).type == IRCMessage.TYPE_ROOMSTATE) {
                                ct++;
                                if (room_state.containsKey(new_chunk.get(c).channel)) {
                                    int idx = room_state.indexOfKey(new_chunk.get(c).channel);
                                    room_state.setValueAt(idx, new_chunk.get(c));
                                } else {
                                    room_state.put(new_chunk.get(c).channel, new_chunk.get(c));
                                }
                            }
                            if (ct == type_counts[IRCMessage.TYPE_USERSTATE] + type_counts[IRCMessage.TYPE_ROOMSTATE]) break;
                        }
                    }
                    lock.release();
                } catch (Exception e) {}

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int first_visible_feed = MainActivity.feed_list_view.getFirstVisiblePosition();
                        int fa_size = MainActivity.feed_list_items.size();
                        int fe_size = type_counts[IRCMessage.TYPE_CLEARMSG];
                        int allowed_size_feed = Settings.getIntSetting(Settings.SETTING_FEED_BACKLOG_SIZE);
                        if (fa_size + fe_size > allowed_size_feed) {
                            for (int c = 0; c < fa_size + fe_size - allowed_size_feed; c++) {
                                first_visible_feed--;
                                MainActivity.feed_list_items.remove(0);
                            }
                        }
                        if (first_visible_feed < 0) first_visible_feed = 0;

                        int first_visible = MainActivity.chat_list_view.getFirstVisiblePosition();
                        int ma_size = MainActivity.irc_message_items.size();
                        int ne_size = type_counts[IRCMessage.TYPE_PRIVMSG];
                        int allowed_size = Settings.getIntSetting(Settings.SETTING_CHAT_BACKLOG_SIZE);
                        if (ma_size + ne_size > allowed_size) {
                            for (int c = 0; c < ma_size + ne_size - allowed_size; c++) {
                                first_visible--;
                                MainActivity.irc_message_items.remove(0);
                            }
                        }
                        if (first_visible < 0) first_visible = 0;

                        for (int c = 0; c < new_chunk.size(); c++) {
                            IRCMessage current = new_chunk.get(c);
                            if (current.type == IRCMessage.TYPE_PRIVMSG) {
                                MainActivity.irc_message_items.add(current);
                            } else if (current.type == IRCMessage.TYPE_CLEARMSG) {
                                MainActivity.feed_list_items.add(current);
                            }
                        }
                        MainActivity.chat_list_view_adapter.notifyDataSetChanged();
                        if (MainActivity.chat_at_bottom) {
                            MainActivity.chat_list_view.smoothScrollToPosition(MainActivity.irc_message_items.size());
                        } else {
                            MainActivity.chat_list_view.setSelection(first_visible);
                        }
                        MainActivity.feed_list_view_adapter.notifyDataSetChanged();
                        if (MainActivity.feed_at_bottom) {
                            MainActivity.feed_list_view.smoothScrollToPosition(MainActivity.feed_list_items.size());
                        } else {
                            MainActivity.feed_list_view.setSelection(first_visible_feed);
                        }
                    }
                });
            }

            if (available_chunks[0] == 1) {
                try {
                    Thread.sleep(update_interval * 500);
                } catch (Exception e) {
                }
            }
        }
    }
}
