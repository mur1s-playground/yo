package de.mur1.yolog;

public class UpdateChatDirect {
    static {
        System.loadLibrary("native-lib");
    }

    public static native void init();
    public static native void setSettings(boolean connect, String channels, String username, String token, String cert_hash, int timeout_seconds);
    public static native char[] updateMessage();
    public static native boolean send(String channel, String content);
}
