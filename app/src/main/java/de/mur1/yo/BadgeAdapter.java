package de.mur1.yo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BadgeAdapter {
    private static JSONObject globals = null;
    private static Boolean globals_requested = false;
    private static ReentrantReadWriteLock reentrantReadWriteLockGlobals = new ReentrantReadWriteLock();

    private static HashMap<Long, JSONObject> channels = new HashMap<Long, JSONObject>();
    private static HashMap<Long, Boolean> channels_requested = new HashMap<Long, Boolean>();

    private static ReentrantReadWriteLock reentrantReadWriteLockChannels = new ReentrantReadWriteLock();

    private static HashMap<String, Emote> badge_bitmaps = new HashMap<String, Emote>();
    private static ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();


    public static void addBadge(String hash, Emote e) {
        reentrantReadWriteLock.writeLock().lock();
        badge_bitmaps.put(hash, e);
        reentrantReadWriteLock.writeLock().unlock();
    }

    public static JSONObject getGlobals() {
        reentrantReadWriteLockGlobals.writeLock().lock();
        JSONObject result = null;
        if (globals == null) {
            if (!globals_requested) {
                globals_requested = true;
                Thread globals_req = new Thread() {
                    @Override
                    public void run() {
                        try {
                            InputStream in = new java.net.URL("https://badges.twitch.tv/v1/badges/global/display").openStream();
                            Scanner s = new Scanner(in).useDelimiter("\\A");
                            String result = s.hasNext() ? s.next() : "";
                            globals = new JSONObject(result);
                            Log.d("Globals download", "!");
                        } catch (Exception e) {
                            reentrantReadWriteLockGlobals.writeLock().lock();
                            globals_requested = false;
                            reentrantReadWriteLockGlobals.writeLock().unlock();
                            Log.d("Globals download", " failed!");
                        }
                    }
                };
                globals_req.start();
            }
        } else {
            result = globals;
        }
        reentrantReadWriteLockGlobals.writeLock().unlock();
        return result;
    }

    public static JSONObject getChannels(Long channel_id) {
        reentrantReadWriteLockChannels.writeLock().lock();
        JSONObject result = channels.get(channel_id);
        if (result == null) {
            Boolean requested = channels_requested.get(channel_id);
            if (requested == null || !requested) {
                channels_requested.put(channel_id, true);
            }
            Thread stuff = new Thread() {
                @Override
                public void run() {
                    try {
                        InputStream in = new java.net.URL("https://badges.twitch.tv/v1/badges/channels/" + channel_id + "/display").openStream();
                        Scanner s = new Scanner(in).useDelimiter("\\A");
                        String result = s.hasNext() ? s.next() : "";
                        JSONObject res = new JSONObject(result);
                        channels.put(channel_id, res);
                        Log.d("Channels download", "!");
                    } catch (Exception e) {
                        reentrantReadWriteLockGlobals.writeLock().lock();
                        channels_requested.put(channel_id, false);
                        reentrantReadWriteLockGlobals.writeLock().unlock();
                        Log.d("Channels download", " failed!");
                    }
                }
            };
            stuff.start();
        }
        reentrantReadWriteLockChannels.writeLock().unlock();
        return result;
    }

    public static Emote getBadge(Long channel_id, String name, int version) {
        Emote result = null;
        if (getGlobals() != null) {
            reentrantReadWriteLock.writeLock().lock();
            result = badge_bitmaps.get(channel_id + ":" + name + "/" + version);
            reentrantReadWriteLock.writeLock().unlock();
            if (result == null) {
                Log.d("Badge not found", name);
                Thread stuff = new Thread() {
                    @Override
                    public void run() {
                        Boolean added = false;
                        try {
                            JSONObject badge_sets = globals.getJSONObject("badge_sets");
                            JSONObject badge_type = badge_sets.getJSONObject(name);
                            JSONObject badge_versions = badge_type.getJSONObject("versions");
                            JSONObject badge = badge_versions.getJSONObject(String.valueOf(version));
                            String url = badge.getString("image_url_2x");
                            Emote n_emote = new Emote();
                            InputStream in = new java.net.URL(url).openStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(in);
                            n_emote.data = (Object) bitmap;
                            n_emote.animated = false;
                            addBadge(channel_id + ":" + name + "/" + version, n_emote);
                            added = true;
                            Log.d("Badge download global", "!");
                        } catch (Exception e) {
                            String message = e.getMessage();
                            if (message != null) {
                                Log.d("Badge download global", message);
                            }
                            e.printStackTrace();
                            Log.d("Badge download global", " failed!");
                        }
                        if (!added) {
                            JSONObject channel = getChannels(channel_id);
                            if (channel != null) {
                                try {
                                    JSONObject badge_sets = channel.getJSONObject("badge_sets");
                                    JSONObject badge_type = badge_sets.getJSONObject(name);
                                    JSONObject badge_versions = badge_type.getJSONObject("versions");
                                    JSONObject badge = badge_versions.getJSONObject(String.valueOf(version));
                                    String url = badge.getString("image_url_2x");
                                    Emote n_emote = new Emote();
                                    InputStream in = new java.net.URL(url).openStream();
                                    Bitmap bitmap = BitmapFactory.decodeStream(in);
                                    n_emote.data = (Object) bitmap;
                                    n_emote.animated = false;
                                    addBadge(channel_id + ":" + name + "/" + version, n_emote);
                                    Log.d("Badge download channel ", "!");
                                } catch (Exception e) {
                                    Log.d("Badge download channel ", " failed!");
                                }
                            }
                        }
                    }
                };
                stuff.start();
            }
        }
        return result;
    }
}
