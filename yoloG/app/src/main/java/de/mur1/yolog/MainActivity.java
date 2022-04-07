package de.mur1.yolog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    LinearLayout menu_inc       = null;

    LinearLayout lock_inc       = null;
    SeekBar lock_seek_bar       = null;
    ImageView lock_left_image   = null;
    ImageView lock_right_image   = null;
    boolean lock_left           = false;
    boolean lock_right          = false;
    boolean locked              = false;

    LinearLayout settings_inc   = null;

    LinearLayout content_inc    = null;

    LinearLayout                        feed_inc                    = null;
    static ArrayList<IRCMessage>        feed_list_items             = new ArrayList<IRCMessage>();
    static ListView                     feed_list_view              = null;
    static FeedListViewAdapter          feed_list_view_adapter      = null;
    static boolean                      feed_at_bottom              = true;

    LinearLayout                        chat_inc                    = null;
    static UpdateChat                   update_chat                 = null;
    long                                chat_chunk_override_value   = 0;
    static ArrayList<IRCMessage>        irc_message_items           = new ArrayList<IRCMessage>();
    static ListView                     chat_list_view              = null;
    static ChatListViewAdapter          chat_list_view_adapter      = null;
    static boolean                      chat_at_bottom              = true;
    static long                         chat_last_touch             = 0;

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

    public void openSettings() {
        menu_inc.setVisibility(View.GONE);
        content_inc.setVisibility(View.GONE);
        settings_inc.setVisibility(View.VISIBLE);

        EditText chat_chunk_ts = (EditText) settings_inc.findViewById(R.id.editTextNumberChatChunkTimestamp);
        chat_chunk_override_value = update_chat.getLastChatChunkTimestamp();
        chat_chunk_ts.setText(String.valueOf(chat_chunk_override_value));
    }

    public void saveApplySettings() {
        try {
            EditText chat_server_ip = (EditText) settings_inc.findViewById(R.id.editTextIP);
            Settings.setStringSetting(Settings.SETTING_CHAT_SERVER_IP, chat_server_ip.getText().toString());

            EditText chat_server_port = (EditText) settings_inc.findViewById(R.id.editTextNumberPort);
            Settings.setIntSetting(Settings.SETTING_CHAT_SERVER_PORT, Integer.parseInt(chat_server_port.getText().toString()));

            EditText chat_update_ival_s = (EditText) settings_inc.findViewById(R.id.editTextNumberChatUpdateInterval);
            Settings.setIntSetting(Settings.SETTING_CHAT_UPDATE_INTERVAL_SECONDS, Integer.parseInt(chat_update_ival_s.getText().toString()));

            EditText chat_chunk_ts = (EditText) settings_inc.findViewById(R.id.editTextNumberChatChunkTimestamp);
            long chat_chunk_ts_l = Long.parseLong(chat_chunk_ts.getText().toString());
            Settings.setLongSetting(Settings.SETTING_CHAT_CHUNK_TIMESTAMP, chat_chunk_ts_l);

            EditText chat_displayed_messages_count = (EditText)settings_inc.findViewById(R.id.editTextNumberDisplayedMessagesCount);
            Settings.setIntSetting(Settings.SETTING_CHAT_BACKLOG_SIZE, Integer.parseInt(chat_displayed_messages_count.getText().toString()));

            if (chat_chunk_override_value != chat_chunk_ts_l) {
                update_chat.overrideLastChatChunkTimestamp(chat_chunk_ts_l);
            }
        } catch (Exception e) {}
        closeSettings();
    }

    public void closeSettings() {
        settings_inc.setVisibility(View.GONE);
        menu_inc.setVisibility(View.VISIBLE);
        content_inc.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        /* */
        deactivateLock();
        closeSettings();

        update_chat = new UpdateChat();
        Thread update_chat_t = new Thread(update_chat);
        update_chat_t.start();

        UpdateChatList update_chat_list = new UpdateChatList(this);
        Thread update_chat_list_t = new Thread(update_chat_list);
        update_chat_list_t.start();
    }

}