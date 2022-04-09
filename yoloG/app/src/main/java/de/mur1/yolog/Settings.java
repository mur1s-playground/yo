package de.mur1.yolog;

public class Settings extends SettingsManager {
    public static final String     SETTING_CONNECTION_TYPE                 = "CONNECTION_TYPE";
    public static final String     SETTING_IRC_NICK                        = "IRC_NICK";
    public static final String     SETTING_IRC_PASS                        = "IRC_PASS";
    public static final String     SETTING_IRC_CHANNEL                     = "IRC_CHANNEL";
    public static final String     SETTING_IRC_CERTIFICATE_HASH            = "IRC_CERTIFICATE_HASH";
    public static final String     SETTING_CHAT_SERVER_IP                  = "CHAT_SERVER_IP";
    public static final String     SETTING_CHAT_SERVER_PORT                = "CHAT_SERVER_PORT";
    public static final String     SETTING_CHAT_UPDATE_INTERVAL_SECONDS    = "CHAT_UPDATE_INTERVAL_SECONDS";
    public static final String     SETTING_CHAT_UPDATE_TIMEOUT_SECONDS     = "CHAT_UPDATE_TIMEOUT_SECONDS";
    public static final String     SETTING_CHAT_CHUNK_TIMESTAMP            = "CHAT_CHUNK_TIMESTAMP";
    public static final String     SETTING_CHAT_BACKLOG_SIZE               = "CHAT_BACKLOG_SIZE";
    public static final String     SETTING_FEED_BACKLOG_SIZE               = "FEED_BACKLOG_SIZE";
    public static final String     SETTING_THEME                           = "THEME";

    static void init(String filepath) {
        addSetting(SETTING_CONNECTION_TYPE              , Setting.TYPE_STRING   , UpdateChat.CONNECTION_TYPE_DIRECT);
        addSetting(SETTING_IRC_NICK                     , Setting.TYPE_STRING   , "justinfan1337");
        addSetting(SETTING_IRC_PASS                     , Setting.TYPE_STRING   , "");
        addSetting(SETTING_IRC_CHANNEL                  , Setting.TYPE_STRING   , "m1_1m");
        addSetting(SETTING_IRC_CERTIFICATE_HASH         , Setting.TYPE_STRING   , "8X8ThYzPyo7QDk1mEloDT/DXEVGQ88tte3iD1F67TLg=");
        addSetting(SETTING_CHAT_SERVER_IP               , Setting.TYPE_STRING   , "192.168.178.24");
        addSetting(SETTING_CHAT_SERVER_PORT             , Setting.TYPE_INT      , 8000);
        addSetting(SETTING_CHAT_UPDATE_INTERVAL_SECONDS , Setting.TYPE_INT      , 5);
        addSetting(SETTING_CHAT_UPDATE_TIMEOUT_SECONDS  , Setting.TYPE_INT      , 5);
        addSetting(SETTING_CHAT_CHUNK_TIMESTAMP         , Setting.TYPE_LONG     , 0L);
        addSetting(SETTING_CHAT_BACKLOG_SIZE            , Setting.TYPE_INT      , 500);
        addSetting(SETTING_FEED_BACKLOG_SIZE            , Setting.TYPE_INT      , 25);
        addSetting(SETTING_THEME                        , Setting.TYPE_STRING   , ThemeManager.light);

        if (filepath != null) {
            loadFromDisk(filepath);
        }
    }
}
