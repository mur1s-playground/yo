package de.mur1.yolog;

public class Settings extends SettingsManager {
    public static final String     SETTING_CHAT_SERVER_IP                  = "CHAT_SERVER_IP";
    public static final String     SETTING_CHAT_SERVER_PORT                = "CHAT_SERVER_PORT";
    public static final String     SETTING_CHAT_UPDATE_INTERVAL_SECONDS    = "CHAT_UPDATE_INTERVAL_SECONDS";
    public static final String     SETTING_CHAT_UPDATE_TIMEOUT_SECONDS     = "CHAT_UPDATE_TIMEOUT_SECONDS";
    public static final String     SETTING_CHAT_CHUNK_TIMESTAMP            = "CHAT_CHUNK_TIMESTAMP";
    public static final String     SETTING_CHAT_BACKLOG_SIZE               = "CHAT_BACKLOG_SIZE";
    public static final String     SETTING_FEED_BACKLOG_SIZE               = "FEED_BACKLOG_SIZE";

    static void init(String filepath) {
        addSetting(SETTING_CHAT_SERVER_IP               , Setting.TYPE_STRING   , "192.168.178.24");
        addSetting(SETTING_CHAT_SERVER_PORT             , Setting.TYPE_INT      , 8000);
        addSetting(SETTING_CHAT_UPDATE_INTERVAL_SECONDS , Setting.TYPE_INT      , 5);
        addSetting(SETTING_CHAT_UPDATE_TIMEOUT_SECONDS  , Setting.TYPE_INT      , 5);
        addSetting(SETTING_CHAT_CHUNK_TIMESTAMP         , Setting.TYPE_LONG     , 0L);
        addSetting(SETTING_CHAT_BACKLOG_SIZE            , Setting.TYPE_INT      , 500);
        addSetting(SETTING_FEED_BACKLOG_SIZE            , Setting.TYPE_INT      , 25);

        if (filepath != null) {
            loadFromDisk(filepath);
        }
    }
}
