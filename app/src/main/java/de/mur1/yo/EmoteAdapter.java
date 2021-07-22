package de.mur1.yo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EmoteAdapter {
    private static HashMap<Long, Emote> emote_bitmaps = new HashMap<Long, Emote>();
    private static HashMap<Long, Boolean> emote_bitmaps_requested = new HashMap<Long, Boolean>();
    private static ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

    public static void addEmote(Long id, Emote e) {
        reentrantReadWriteLock.writeLock().lock();
        emote_bitmaps.put(id, e);
        reentrantReadWriteLock.writeLock().unlock();
    }

    public static Emote getEmote(Long id) {
        reentrantReadWriteLock.writeLock().lock();
        Emote result = emote_bitmaps.get(id);
        Boolean req = false;
        if (result == null) {
            Boolean req_ = emote_bitmaps_requested.get(id);
            if (req_ == null || !req_) {
                emote_bitmaps_requested.put(id, true);
                req = true;
            }
        }
        reentrantReadWriteLock.writeLock().unlock();
        if (req) {
            Log.d("Emote not found", id + "");
            Thread stuff = new Thread() {
                @Override
                public void run() {
                    try {
                        Emote n_emote = new Emote();
                        InputStream in = new java.net.URL("https://static-cdn.jtvnw.net/emoticons/v1/" + id + "/2.0").openStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(in);
                        n_emote.data = (Object) bitmap;
                        n_emote.animated = false;
                        addEmote(id, n_emote);
                        Log.d("Emote download", "!");
                    } catch (Exception e) {
                        String message = e.getMessage();
                        if (message != null) {
                            Log.d("Emote download", message);
                        }
                        reentrantReadWriteLock.writeLock().lock();
                        emote_bitmaps_requested.put(id, false);
                        reentrantReadWriteLock.writeLock().unlock();
                        Log.d("Emote download", " failed!");
                    }
                }
            };
            stuff.start();
        }
        return result;
    }
}
