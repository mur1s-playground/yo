package de.mur1.yolog;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static String               files_internal_path = null;
    static String               settings_path       = null;

    LinearLayout menu_inc       = null;

    LinearLayout lock_inc       = null;
    SeekBar lock_seek_bar       = null;
    ImageView lock_left_image   = null;
    ImageView lock_right_image   = null;
    boolean lock_left           = false;
    boolean lock_right          = false;
    boolean locked              = false;

    ScrollView settings_inc   = null;
    LinearLayout settings_direct_inc = null;
    LinearLayout settings_proxy_inc = null;

    LinearLayout content_inc    = null;

    LinearLayout                        feed_inc                    = null;
    static ArrayList<IRCMessage>        feed_list_items             = new ArrayList<IRCMessage>();
    static ListView                     feed_list_view              = null;
    static FeedListViewAdapter          feed_list_view_adapter      = null;
    static boolean                      feed_at_bottom              = true;

    LinearLayout                        chat_inc                    = null;
    static UpdateChat                   update_chat                 = null;
    static UpdateChatList               update_chat_list            = null;
    long                                chat_chunk_override_value   = 0;
    static ArrayList<IRCMessage>        irc_message_items           = new ArrayList<IRCMessage>();
    static ListView                     chat_list_view              = null;
    static ChatListViewAdapter          chat_list_view_adapter      = null;
    static boolean                      chat_at_bottom              = true;
    static Spinner                      chat_select                 = null;
    static EditText                     chat_input                  = null;
    static ImageButton                  chat_send                   = null;

    public void activateLock() {
        menu_inc.setVisibility(View.GONE);
        lock_inc.setVisibility(View.VISIBLE);
        locked = true;
        lock_left = true;
        lock_right = true;
        lock_left_image.setColorFilter(Color.parseColor("#FF0000"));
        lock_right_image.setColorFilter(Color.parseColor("#FF0000"));
        lock_seek_bar.setProgress(50);

        feed_list_view.setEnabled(false);
        chat_list_view.setEnabled(false);
    }

    public void deactivateLock() {
        lock_inc.setVisibility(View.GONE);
        menu_inc.setVisibility(View.VISIBLE);
        locked = false;

        feed_list_view.setEnabled(true);
        chat_list_view.setEnabled(true);
    }

    public void updateChatInput(boolean connect, String channels) {
        if (!connect || Settings.getStringSetting(Settings.SETTING_IRC_NICK).toLowerCase(Locale.ROOT).startsWith("justinfan")) {
            chat_select.setVisibility(View.GONE);
            chat_input.setVisibility(View.GONE);
            chat_send.setVisibility(View.GONE);
        } else {
            String channels_arr[] = channels.split(",");
            ArrayAdapter<String> channels_adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, channels_arr);
            chat_select.setAdapter(channels_adapter);
            if (channels.split(",").length > 1) {
                chat_select.setVisibility(View.VISIBLE);
            }
            chat_input.setVisibility(View.VISIBLE);
            chat_send.setVisibility(View.VISIBLE);
        }
    }

    public void openSettings() {
        menu_inc.setVisibility(View.GONE);
        content_inc.setVisibility(View.GONE);
        settings_inc.setVisibility(View.VISIBLE);

        Spinner chat_irc_connection_type = (Spinner) settings_inc.findViewById(R.id.spinnerIRCConnectionType);
        String active_connection_type = Settings.getStringSetting(Settings.SETTING_CONNECTION_TYPE);
        for (int ct = 0; ct < chat_irc_connection_type.getAdapter().getCount(); ct++) {
            if (((String)chat_irc_connection_type.getItemAtPosition(ct)).equals(active_connection_type)) {
                chat_irc_connection_type.setSelection(ct);
                break;
            }
        }

        EditText chat_irc_nickname = (EditText) settings_direct_inc.findViewById(R.id.editTextIRCNick);
        chat_irc_nickname.setText(Settings.getStringSetting(Settings.SETTING_IRC_NICK));

        EditText chat_irc_pass = (EditText) settings_direct_inc.findViewById(R.id.editTextIRCPassword);
        chat_irc_pass.setText(Settings.getStringSetting(Settings.SETTING_IRC_PASS));

        EditText chat_irc_channels = (EditText) settings_direct_inc.findViewById(R.id.editTextIRCChannel);
        chat_irc_channels.setText(Settings.getStringSetting(Settings.SETTING_IRC_CHANNEL));

        EditText chat_irc_cert = (EditText) settings_direct_inc.findViewById(R.id.editTextIRCCertHash);
        chat_irc_cert.setText(Settings.getStringSetting(Settings.SETTING_IRC_CERTIFICATE_HASH));

        EditText chat_server_ip = (EditText) settings_proxy_inc.findViewById(R.id.editTextIP);
        chat_server_ip.setText(Settings.getStringSetting(Settings.SETTING_CHAT_SERVER_IP));

        EditText chat_server_port = (EditText) settings_proxy_inc.findViewById(R.id.editTextNumberPort);
        chat_server_port.setText(String.valueOf(Settings.getIntSetting(Settings.SETTING_CHAT_SERVER_PORT)));

        EditText chat_update_ival_s = (EditText) settings_proxy_inc.findViewById(R.id.editTextNumberChatUpdateInterval);
        chat_update_ival_s.setText(String.valueOf(Settings.getIntSetting(Settings.SETTING_CHAT_UPDATE_INTERVAL_SECONDS)));

        EditText chat_timeout_ival_s = (EditText) settings_inc.findViewById(R.id.editTextNumberChatTimeoutIntervalSeconds);
        chat_timeout_ival_s.setText(String.valueOf(Settings.getIntSetting(Settings.SETTING_CHAT_UPDATE_TIMEOUT_SECONDS)));

        EditText chat_chunk_ts = (EditText) settings_inc.findViewById(R.id.editTextNumberChatChunkTimestamp);
        chat_chunk_override_value = update_chat.getLastChatChunkTimestamp();
        chat_chunk_ts.setText(String.valueOf(chat_chunk_override_value));

        EditText chat_displayed_messages_count = (EditText)settings_inc.findViewById(R.id.editTextNumberDisplayedMessagesCount);
        chat_displayed_messages_count.setText(String.valueOf(Settings.getIntSetting(Settings.SETTING_CHAT_BACKLOG_SIZE)));
        Settings.setIntSetting(Settings.SETTING_CHAT_BACKLOG_SIZE, Integer.parseInt(chat_displayed_messages_count.getText().toString()));

        EditText feed_displayed_message_count = (EditText)settings_inc.findViewById(R.id.editTextNumberFeedDisplayedMessagesCount);
        feed_displayed_message_count.setText(String.valueOf(Settings.getIntSetting(Settings.SETTING_FEED_BACKLOG_SIZE)));

        Spinner setting_theme = settings_inc.findViewById(R.id.spinnerTheme);
        String active_name = Settings.getStringSetting(Settings.SETTING_THEME);
        for (int i = 0; i < setting_theme.getAdapter().getCount(); i++) {
            if (((String) setting_theme.getItemAtPosition(i)).equals(active_name)) {
                setting_theme.setSelection(i);
                break;
            }
        }

    }

    public void saveApplySettings() {
        try {
            Spinner chat_irc_connection_type = (Spinner) settings_inc.findViewById(R.id.spinnerIRCConnectionType);
            Settings.setStringSetting(Settings.SETTING_CONNECTION_TYPE, (String) chat_irc_connection_type.getSelectedItem());

            EditText chat_irc_nickname = (EditText) settings_direct_inc.findViewById(R.id.editTextIRCNick);
            Settings.setStringSetting(Settings.SETTING_IRC_NICK, chat_irc_nickname.getText().toString());

            EditText chat_irc_pass = (EditText) settings_direct_inc.findViewById(R.id.editTextIRCPassword);
            Settings.setStringSetting(Settings.SETTING_IRC_PASS, chat_irc_pass.getText().toString());

            EditText chat_irc_channels = (EditText) settings_direct_inc.findViewById(R.id.editTextIRCChannel);
            Settings.setStringSetting(Settings.SETTING_IRC_CHANNEL, chat_irc_channels.getText().toString());

            EditText chat_irc_cert = (EditText) settings_direct_inc.findViewById(R.id.editTextIRCCertHash);
            Settings.setStringSetting(Settings.SETTING_IRC_CERTIFICATE_HASH, chat_irc_cert.getText().toString());

            EditText chat_server_ip = (EditText) settings_inc.findViewById(R.id.editTextIP);
            Settings.setStringSetting(Settings.SETTING_CHAT_SERVER_IP, chat_server_ip.getText().toString());

            EditText chat_server_port = (EditText) settings_inc.findViewById(R.id.editTextNumberPort);
            Settings.setIntSetting(Settings.SETTING_CHAT_SERVER_PORT, Integer.parseInt(chat_server_port.getText().toString()));

            EditText chat_update_ival_s = (EditText) settings_inc.findViewById(R.id.editTextNumberChatUpdateInterval);
            Settings.setIntSetting(Settings.SETTING_CHAT_UPDATE_INTERVAL_SECONDS, Integer.parseInt(chat_update_ival_s.getText().toString()));

            EditText chat_timeout_ival_s = (EditText) settings_inc.findViewById(R.id.editTextNumberChatTimeoutIntervalSeconds);
            Settings.setIntSetting(Settings.SETTING_CHAT_UPDATE_TIMEOUT_SECONDS, Integer.parseInt(chat_timeout_ival_s.getText().toString()));

            EditText chat_chunk_ts = (EditText) settings_inc.findViewById(R.id.editTextNumberChatChunkTimestamp);
            long chat_chunk_ts_l = Long.parseLong(chat_chunk_ts.getText().toString());
            Settings.setLongSetting(Settings.SETTING_CHAT_CHUNK_TIMESTAMP, chat_chunk_ts_l);

            EditText chat_displayed_messages_count = (EditText)settings_inc.findViewById(R.id.editTextNumberDisplayedMessagesCount);
            Settings.setIntSetting(Settings.SETTING_CHAT_BACKLOG_SIZE, Integer.parseInt(chat_displayed_messages_count.getText().toString()));

            EditText feed_displayed_message_count = (EditText)settings_inc.findViewById(R.id.editTextNumberFeedDisplayedMessagesCount);
            Settings.setIntSetting(Settings.SETTING_FEED_BACKLOG_SIZE, Integer.parseInt(feed_displayed_message_count.getText().toString()));

            if (chat_chunk_override_value != chat_chunk_ts_l) {
                update_chat.overrideLastChatChunkTimestamp(chat_chunk_ts_l);
            }

            Spinner setting_theme = settings_inc.findViewById(R.id.spinnerTheme);
            Settings.setStringSetting(Settings.SETTING_THEME, (String) setting_theme.getSelectedItem());

            Settings.writeToDisk(settings_path);

            boolean connect_direct =((String)chat_irc_connection_type.getSelectedItem()).equals(UpdateChat.CONNECTION_TYPE_DIRECT);
            UpdateChatDirect.setSettings(connect_direct, chat_irc_channels.getText().toString(), chat_irc_nickname.getText().toString(), chat_irc_pass.getText().toString(), chat_irc_cert.getText().toString(), Integer.parseInt(chat_timeout_ival_s.getText().toString()));

            updateChatInput(connect_direct, chat_irc_channels.getText().toString());
        } catch (Exception e) {}
        closeSettings();
    }

    public void closeSettings() {
        settings_inc.setVisibility(View.GONE);
        menu_inc.setVisibility(View.VISIBLE);
        content_inc.setVisibility(View.VISIBLE);
    }

    public void onSelectConnectionType(String type) {
        if (type == UpdateChat.CONNECTION_TYPE_DIRECT) {
            settings_direct_inc.setVisibility(View.VISIBLE);
            settings_proxy_inc.setVisibility(View.GONE);
        } else if (type == UpdateChat.CONNECTION_TYPE_PROXY) {
            settings_proxy_inc.setVisibility(View.VISIBLE);
            settings_direct_inc.setVisibility(View.GONE);
        }
    }

    public void onThemeSwitch() {
        this.findViewById(R.id.main).setBackgroundColor(ThemeManager.active_theme.background_color);

        ThemeManager.applyTheme(menu_inc);
        ThemeManager.applyTheme(lock_inc);

        View menu_content_separator = this.findViewById(R.id.menu_content_separator);
        menu_content_separator.setBackgroundColor(ThemeManager.active_theme.text_color);

        ThemeManager.applyTheme(settings_inc);
        ThemeManager.applyTheme(feed_inc);
        ThemeManager.applyTheme(chat_inc);

        View feed_chat_separator = content_inc.findViewById(R.id.feed_chat_separator);
        feed_chat_separator.setBackgroundColor(ThemeManager.active_theme.text_color);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        files_internal_path = getApplicationContext().getFilesDir().getAbsolutePath();

        settings_path = files_internal_path + "/settings.json";
        Settings.init(settings_path);

        ThemeManager.init();
        String active_theme = Settings.getStringSetting(Settings.SETTING_THEME);
        if (active_theme != null) {
            ThemeManager.setTheme(active_theme);
        }

        UpdateChatDirect.init();
        boolean connect_direct = Settings.getStringSetting(Settings.SETTING_CONNECTION_TYPE).equals(UpdateChat.CONNECTION_TYPE_DIRECT);
        UpdateChatDirect.setSettings(connect_direct,
                Settings.getStringSetting(Settings.SETTING_IRC_CHANNEL),
                Settings.getStringSetting(Settings.SETTING_IRC_NICK),
                Settings.getStringSetting(Settings.SETTING_IRC_PASS),
                Settings.getStringSetting(Settings.SETTING_IRC_CERTIFICATE_HASH),
                Settings.getIntSetting(Settings.SETTING_CHAT_UPDATE_TIMEOUT_SECONDS));

        /* MENU */
        menu_inc = (LinearLayout) this.findViewById(R.id.menu_inc);
        ImageButton lock_button = menu_inc.findViewById(R.id.imageButtonLock);
        lock_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activateLock();
            }
        });
        ImageButton settings_button = menu_inc.findViewById(R.id.imageButtonSettings);
        settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });

        /* LOCK */
        lock_inc = (LinearLayout) this.findViewById(R.id.lock_inc);
        lock_seek_bar = (SeekBar) this.findViewById(R.id.seekBarLock);
        lock_seek_bar.setMax(100);
        lock_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i == 100) {
                    lock_right = false;
                    lock_right_image.setColorFilter(Color.parseColor("#00FF00"));
                }
                if (i == 0) {
                    lock_left = false;
                    lock_left_image.setColorFilter(Color.parseColor("#00FF00"));
                }
                if (!lock_left && !lock_right) {
                    deactivateLock();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        lock_left_image = (ImageView) this.findViewById(R.id.imageViewLockLeft);
        lock_right_image = (ImageView) this.findViewById(R.id.imageViewLockRight);

        /* SETTINGS */
        settings_inc = this.findViewById(R.id.settings_inc);

        Spinner setting_connection_type = settings_inc.findViewById(R.id.spinnerIRCConnectionType);
        String[] c_types = {UpdateChat.CONNECTION_TYPE_DIRECT, UpdateChat.CONNECTION_TYPE_PROXY};
        ArrayAdapter<String> c_adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, c_types);
        setting_connection_type.setAdapter(c_adapter);
        setting_connection_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                onSelectConnectionType((String) adapterView.getSelectedItem());
                ThemeManager.applyTheme(adapterView.getRootView());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        settings_direct_inc = settings_inc.findViewById(R.id.settings_direct_inc);
        settings_proxy_inc = settings_inc.findViewById(R.id.settings_proxy_inc);

        Spinner setting_theme = settings_inc.findViewById(R.id.spinnerTheme);
        String[] themes = ThemeManager.getAvailableThemeNames();
        ArrayAdapter<String> theme_adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, themes);
        setting_theme.setAdapter(theme_adapter);
        setting_theme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ThemeManager.setTheme((String) setting_theme.getSelectedItem());
                onThemeSwitch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ImageButton close_settings = settings_inc.findViewById(R.id.imageButtonCancel);
        close_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeSettings();
            }
        });
        ImageButton save_settings = settings_inc.findViewById(R.id.imageButtonSave);
        save_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveApplySettings();
            }
        });

        /* CONTENT */
        content_inc = this.findViewById(R.id.content_inc);

        feed_inc = content_inc.findViewById(R.id.feed_inc);
        feed_list_view = feed_inc.findViewById(R.id.listViewFeed);
        feed_list_view_adapter = new FeedListViewAdapter(getApplicationContext(), feed_list_items);
        feed_list_view.setAdapter(feed_list_view_adapter);
        feed_list_view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (!locked) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_DOWN || motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                        if (motionEvent.getAxisValue(MotionEvent.AXIS_Y) >= 0.75 * (float) feed_list_view.getHeight()) {
                            feed_at_bottom = true;
                        } else {
                            feed_at_bottom = false;
                        }
                    }
                }
                return false;
            }
        });

        chat_inc = content_inc.findViewById(R.id.chat_inc);
        chat_list_view = chat_inc.findViewById(R.id.listViewChat);
        chat_list_view_adapter = new ChatListViewAdapter(getApplicationContext(), irc_message_items);
        chat_list_view.setAdapter(chat_list_view_adapter);
        //chat_list_view.setFastScrollEnabled(true);
        chat_list_view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (!locked) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_DOWN || motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                        if (motionEvent.getAxisValue(MotionEvent.AXIS_Y) >= 0.75 * (float) chat_list_view.getHeight()) {
                            chat_at_bottom = true;
                        } else {
                            chat_at_bottom = false;
                        }
                    }
                }
                return false;
            }
        });
        chat_select = chat_inc.findViewById(R.id.spinnerChatSelect);
        chat_select.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ThemeManager.applyTheme(adapterView.getRootView());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        chat_input = chat_inc.findViewById(R.id.editTextChatInput);
        chat_send = (ImageButton) chat_inc.findViewById(R.id.imageButtonChatSend);
        chat_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String channel = (String)chat_select.getSelectedItem();
                String content = chat_input.getText().toString();
                if (content.length() > 0) {
                    if (UpdateChatDirect.send(channel, content)) {
                        long time = System.currentTimeMillis() / 1000;
                        String prefix = update_chat_list.getUserstatePrefix("#"+ channel);
                        String name = Settings.getStringSetting(Settings.SETTING_IRC_NICK);
                        if (prefix == null) {
                            prefix = "display-name=" + name;
                        }
                        String prefix_room = update_chat_list.getRoomstatePrefix("#" + channel);
                        if (prefix_room != null) {
                            prefix += ";" + prefix_room;
                        }
                        String msg  = "time:" + time + "\n";
                               msg += "prefix:" + prefix +"\n";
                               msg += "name:" + name + "\n";
                               msg += "command:PRIVMSG\n";
                               msg += "channel:#" + channel + "\n";
                               msg += "msg::" + content + "\n";

                        update_chat.addChatChunk(time, msg);
                        chat_input.setText("");
                    }
                }
            }
        });

        /* */
        deactivateLock();
        closeSettings();
        onThemeSwitch();
        updateChatInput(connect_direct, Settings.getStringSetting(Settings.SETTING_IRC_CHANNEL));

        update_chat = new UpdateChat();
        Thread update_chat_t = new Thread(update_chat);
        update_chat_t.start();

        update_chat_list = new UpdateChatList(this);
        Thread update_chat_list_t = new Thread(update_chat_list);
        update_chat_list_t.start();
    }

}