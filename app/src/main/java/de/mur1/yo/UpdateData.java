package de.mur1.yo;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Vector;


public class UpdateData implements Runnable {
    public class BadgeReplace {
        String name;
        int version;
    }

    public class EmoteReplace {
        Long id;
        int range_start, range_end;
    }

    Context context;

    public UpdateData(Context context) {
        this.context = context;
    }

    public void run() {
        while (true) {
            String msg = updateMessage();
            if (msg.length() > 0) {
                String[] splt = msg.split("\\n");
                if (splt.length > 2) {
                    String[] prefix_arr = splt[0].split(";");
                    HashMap<String, String> prefix_key_value = new HashMap<>();

                    for (int p = 0; p < prefix_arr.length; p++) {
                        String[] key_value = prefix_arr[p].split("=", -1);
                        if (key_value.length == 2) {
                            prefix_key_value.put(key_value[0], key_value[1]);
                        }
                    }

                    String display_name = prefix_key_value.get("display-name");
                    int name_color = Color.parseColor("#000000");
                    ;
                    try {
                        String n_c = prefix_key_value.get("color");
                        if (n_c != null) {
                            name_color = Color.parseColor(n_c);
                        }
                    } catch (Exception e) {
                    }

                    String room_id = prefix_key_value.get("room-id");
                    Long channel_id = -1L;
                    try {
                        if (room_id.length() > 0) {
                            channel_id = Long.valueOf(room_id);
                        }
                    } catch (Exception e) {
                    }

                    Vector<BadgeReplace> badge_replace = new Vector<BadgeReplace>();
                    try {
                        String badges = prefix_key_value.get("badges");
                        if (badges.length() > 0) {
                            String[] badge_arr = badges.split(",");
                            for (int b_a = 0; b_a < badge_arr.length; b_a++) {
                                String[] badge = badge_arr[b_a].split("/");
                                if (badge.length > 1) {
                                    BadgeReplace b_r = new BadgeReplace();
                                    b_r.name = badge[0];
                                    b_r.version = Integer.parseInt(badge[1]);
                                    badge_replace.add(b_r);
                                }
                            }
                        }
                    } catch (Exception e) {
                    }

                    Vector<EmoteReplace> emote_replace = new Vector<EmoteReplace>();
                    try {
                        String emotes = prefix_key_value.get("emotes");
                        if (emotes.length() > 0) {
                            String[] emotes_arr = emotes.split("/");
                            for (int e = 0; e < emotes_arr.length; e++) {
                                String[] emote_splt = emotes_arr[e].split(":");
                                Long emote_id = Long.valueOf(emote_splt[0]);
                                String[] emote_ranges = emote_splt[1].split(",");
                                for (int er = 0; er < emote_ranges.length; er++) {
                                    String[] emote_range = emote_ranges[er].split("-");
                                    EmoteReplace e_r = new EmoteReplace();
                                    e_r.id = emote_id;
                                    e_r.range_start = Integer.parseInt(emote_range[0]);
                                    e_r.range_end = Integer.parseInt(emote_range[1]);
                                    Log.d("range_start-range_end", emote_range[0] + ":" + e_r.range_start + " - " + e_r.range_end + ":" + emote_range[1]);
                                    if (emote_replace.size() == 0) {
                                        emote_replace.add(e_r);
                                    } else {
                                        Boolean inserted = false;
                                        for (int e_r_s = 0; e_r_s < emote_replace.size(); e_r_s++) {
                                            if (emote_replace.get(e_r_s).range_start > e_r.range_start) {
                                                emote_replace.insertElementAt(e_r, e_r_s);
                                                inserted = true;
                                                break;
                                            }
                                        }
                                        if (!inserted) {
                                            emote_replace.add(e_r);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }

                    String message = splt[3];

                    if (display_name != null) {
                        Handler mainHandler = new Handler(context.getMainLooper());
                        int finalName_color = name_color;
                        Long finalChannel_id = channel_id;
                        Runnable updateUI = new Runnable() {
                            @Override
                            public void run() {
                                LinearLayoutManager llm = (LinearLayoutManager) MainActivity.chatLayoutManager;
                                int first_visible = 249;
                                if (!MainActivity.chatAtBottom) {
                                    first_visible = llm.findFirstVisibleItemPosition() - 1;
                                    if (first_visible < 0) first_visible = 0;
                                }
                                MainActivity.chatAdapter.updateData(finalChannel_id, badge_replace, display_name, finalName_color, message, emote_replace);
                                MainActivity.chatAdapter.notifyDataSetChanged();
                                llm.scrollToPosition(first_visible);
                            }
                        };
                        mainHandler.post(updateUI);
                    }
                }
                try {
                    Thread.sleep(10);
                } catch(Exception e) {

                }
            } else {
                try {
                    Thread.sleep(100);
                } catch(Exception e) {

                }
            }
        }
    }

    public native String updateMessage();
}
