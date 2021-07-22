package de.mur1.yo;

import java.util.Vector;

public class IRCMessage {
    public String prefix;
    Long channel_id;
    public Vector<UpdateData.BadgeReplace> badge_replace;
    public String name;
    public int name_color;
    public String command;
    public String message;
    public Vector<UpdateData.EmoteReplace> emote_replace;
}
