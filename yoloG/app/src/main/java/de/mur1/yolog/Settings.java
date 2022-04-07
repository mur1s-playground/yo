package de.mur1.yolog;

import java.util.concurrent.Semaphore;

public class Settings {
    public static final int     SETTING_CHAT_SERVER_IP                  = 0;
    public static final int     SETTING_CHAT_SERVER_PORT                = SETTING_CHAT_SERVER_IP + 1;
    public static final int     SETTING_CHAT_UPDATE_INTERVAL_SECONDS    = SETTING_CHAT_SERVER_PORT + 1;
    public static final int     SETTING_CHAT_UPDATE_TIMEOUT_SECONDS     = SETTING_CHAT_UPDATE_INTERVAL_SECONDS + 1;
    public static final int     SETTING_CHAT_CHUNK_TIMESTAMP            = SETTING_CHAT_UPDATE_TIMEOUT_SECONDS + 1;
    public static final int     SETTING_CHAT_BACKLOG_SIZE               = SETTING_CHAT_CHUNK_TIMESTAMP + 1;
    public static final int     SETTING_FEED_BACKLOG_SIZE               = SETTING_CHAT_BACKLOG_SIZE + 1;

    private static Semaphore settings_lock  = new Semaphore(1, true);

    private static String       settings_chat_server_ip               = "192.168.178.24";
    private static int          settings_chat_server_port             = 8000;
    private static int          settings_chat_update_interval_seconds = 5;
    private static int          settings_chat_update_timeout_seconds  = 5;
    private static long         settings_chat_chunk_timestamp         = 0L;
    private static int          settings_chat_backlog_size            = 500;

    private static int          settings_feed_backlog_size            = 25;


    static int                  getIntSetting(int setting) {
        int result = Integer.MAX_VALUE;
        try {
            settings_lock.acquire();
            if (setting == SETTING_CHAT_SERVER_PORT) result = settings_chat_server_port;
            if (setting == SETTING_CHAT_UPDATE_INTERVAL_SECONDS) result = settings_chat_update_interval_seconds;
            if (setting == SETTING_CHAT_UPDATE_TIMEOUT_SECONDS) result = settings_chat_update_timeout_seconds;
            if (setting == SETTING_CHAT_BACKLOG_SIZE) result = settings_chat_backlog_size;
            if (setting == SETTING_FEED_BACKLOG_SIZE) result = settings_feed_backlog_size;
            settings_lock.release();
        } catch (Exception e) {};
        return result;
    }

    static long                  getLongSetting(int setting) {
        long result = Long.MAX_VALUE;
        try {
            settings_lock.acquire();
            if (setting == SETTING_CHAT_CHUNK_TIMESTAMP) result = settings_chat_chunk_timestamp;
            settings_lock.release();
        } catch (Exception e) {};
        return result;
    }



    static String               getStringSetting(int setting) {
        String result = null;
        try {
            settings_lock.acquire();
            if (setting == SETTING_CHAT_SERVER_IP) result = settings_chat_server_ip;
            settings_lock.release();
        } catch (Exception e) {};
        return result;
    }

    static void                setIntSetting(int setting, int value) {
        try {
            settings_lock.acquire();
            if (setting == SETTING_CHAT_SERVER_PORT) settings_chat_server_port = value;
            if (setting == SETTING_CHAT_UPDATE_INTERVAL_SECONDS) settings_chat_update_interval_seconds = value;
            if (setting == SETTING_CHAT_UPDATE_TIMEOUT_SECONDS) settings_chat_update_timeout_seconds = value;
            if (setting == SETTING_CHAT_BACKLOG_SIZE) settings_chat_backlog_size = value;
            if (setting == SETTING_FEED_BACKLOG_SIZE) settings_feed_backlog_size = value;
            settings_lock.release();
        } catch (Exception e) {};
    }

    static void                setLongSetting(int setting, long value) {
        try {
            settings_lock.acquire();
            if (setting == SETTING_CHAT_CHUNK_TIMESTAMP) settings_chat_chunk_timestamp = value;
            settings_lock.release();
        } catch (Exception e) {};
    }

    static void                 setStringSetting(int setting, String value) {
        try {
            settings_lock.acquire();
            if (setting == SETTING_CHAT_SERVER_IP) settings_chat_server_ip = value;
            settings_lock.release();
        } catch (Exception e) {};
    }
}
