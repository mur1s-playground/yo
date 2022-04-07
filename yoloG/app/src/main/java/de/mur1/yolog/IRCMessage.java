package de.mur1.yolog;

import android.graphics.Color;
import android.util.Log;

import java.lang.reflect.Array;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

public class IRCMessage {
    public static final int TYPE_PRIVMSG  = 0;
    public static final int TYPE_CLEARMSG = TYPE_PRIVMSG + 1;

    private long time;
    private String prefix;
    private String channel;
    private String name;
    private String command;

    public int                      type;
    public String                   display_time;
    public Long                     channel_id;
    public Vector<BadgeReplace>     badge_replace;
    public String                   display_name;
    public int                      name_color;
    public Vector<EmoteReplace>     emote_replace;
    public String                   message;
    public int                      ban_duration;

    public static ArrayList<IRCMessage> newChunk(String chunk, int[] t_count) {
        ArrayList<IRCMessage> result = new ArrayList<IRCMessage>();

        String lines[] = chunk.split("\n");
        IRCMessage irc_m = null;
        for (int l = 0; l < lines.length; l++) {
            if (lines[l].startsWith("time:")) {
                if (irc_m != null) {
                    result.add(irc_m);
                }
                irc_m = new IRCMessage();
                irc_m.time = Long.valueOf(lines[l].substring((new String("time:").length())));
                Date dt = new Date(irc_m.time * 1000);
                String h_str = "" + dt.getHours();
                if (h_str.length() == 1) h_str = "0" + h_str;
                String m_str = "" + dt.getMinutes();
                if (m_str.length() == 1) m_str = "0" + m_str;

                irc_m.display_time = h_str + ":" + m_str;
            } else if (lines[l].startsWith("prefix:")) {
                irc_m.prefix = lines[l].substring((new String("prefix:").length()));

                String[] prefix_arr = irc_m.prefix.split(";");
                HashMap<String, String> prefix_key_value = new HashMap<>();
                for (int p = 0; p < prefix_arr.length; p++) {
                    String[] key_value = prefix_arr[p].split("=", -1);
                    if (key_value.length == 2) {
                        prefix_key_value.put(key_value[0], key_value[1]);
                    }
                }

                try {
                    String b_c = prefix_key_value.get("@ban-duration");
                    if (b_c != null) {
                        irc_m.ban_duration = Integer.parseInt(b_c);
                    }
                } catch (Exception e) { }

                irc_m.display_name = prefix_key_value.get("display-name");
                irc_m.name_color = Color.parseColor("#000000");
                try {
                    String n_c = prefix_key_value.get("color");
                    if (n_c != null) {
                        irc_m.name_color = Color.parseColor(n_c);
                    }
                } catch (Exception e) {
                }

                String room_id = prefix_key_value.get("room-id");
                irc_m.channel_id = -1L;
                try {
                    if (room_id.length() > 0) {
                        irc_m.channel_id = Long.valueOf(room_id);
                    }
                } catch (Exception e) {
                }

                irc_m.badge_replace = new Vector<BadgeReplace>();
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
                                irc_m.badge_replace.add(b_r);
                            }
                        }
                    }
                } catch (Exception e) {
                }

                irc_m.emote_replace = new Vector<EmoteReplace>();
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
                                if (irc_m.emote_replace.size() == 0) {
                                    irc_m.emote_replace.add(e_r);
                                } else {
                                    Boolean inserted = false;
                                    for (int e_r_s = 0; e_r_s < irc_m.emote_replace.size(); e_r_s++) {
                                        if (irc_m.emote_replace.get(e_r_s).range_start > e_r.range_start) {
                                            irc_m.emote_replace.insertElementAt(e_r, e_r_s);
                                            inserted = true;
                                            break;
                                        }
                                    }
                                    if (!inserted) {
                                        irc_m.emote_replace.add(e_r);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                }

            } else if (lines[l].startsWith("name:")) {
                irc_m.name = lines[l].substring((new String("name:").length()));
            } else if (lines[l].startsWith("command:")) {
                irc_m.command = lines[l].substring((new String("command:").length()));
                if (irc_m.command.equals("PRIVMSG")) {
                    t_count[TYPE_PRIVMSG]++;
                    irc_m.type = TYPE_PRIVMSG;
                } else if (irc_m.command.equals("CLEARCHAT")) {
                    t_count[TYPE_CLEARMSG]++;
                    irc_m.type = TYPE_CLEARMSG;
                }
            } else if (lines[l].startsWith("channel:")) {
                irc_m.channel = lines[l].substring((new String("channel:").length()));
            } else if (lines[l].startsWith("msg:")) {
                irc_m.message = lines[l].substring((new String("msg:").length()));
            }
        }
        if (irc_m != null) {
            result.add(irc_m);
        }
        return result;
    }
}
