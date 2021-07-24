package de.mur1.yo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.os.HandlerCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayout;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.mur1.yo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private ActivityMainBinding binding;

    private ExecutorService executor_service_token = null;
    private ExecutorService executor_service = null;
    private Handler main_thread_handler = null;

    public static RecyclerView chatRecyclerView;
    public static ChatAdapter chatAdapter;
    public static RecyclerView.LayoutManager chatLayoutManager;
    public static Boolean chatAtBottom = true;

    public static SharedPreferences shared_pref = null;

    public static String setting_channel = "";
    public static String setting_username = "";
    public static String setting_token = "";
    public static boolean setting_chat_read = false;
    public static boolean setting_chat_edit = false;
    public static boolean setting_channel_moderate = false;

    public static String token_request_md5 = "";
    public static String active_username = "";

    public static String token_request_url = "";
    public static boolean waiting_for_token = false;
    public static boolean start_token_activity = false;
    public static boolean irc_connected = false;

    public static void updateSharedPreferences() {
        Log.d("MainActivity", "updateSharedPreferences!");

        boolean reconnect = false;
        boolean token_force_verify = false;
        boolean token_request = false;

        String channel_new = shared_pref.getString(SettingsActivity.SETTING_CHANNEL, "m1_1m");
        if (!channel_new.equals(setting_channel)) {
            setting_channel = channel_new;
            reconnect = true;
        }

        String username_new = shared_pref.getString(SettingsActivity.SETTING_USERNAME, "justinfan1337");
        if (!username_new.equals(setting_username)) {
            setting_username = username_new;
            reconnect = true;
            if (!username_new.startsWith("justinfan")) {
                token_force_verify = true;
                token_request = true;

                if (setting_token.length() == 0) {
                    setting_token = shared_pref.getString(SettingsActivity.SETTING_TOKEN, "");
                    reconnect = true;
                }

                boolean chat_read_new = shared_pref.getBoolean(SettingsActivity.SETTING_CHAT_READ, false);
                if (chat_read_new != setting_chat_read) {
                    setting_chat_read = chat_read_new;
                    reconnect = true;
                    token_request = true;
                }

                boolean chat_edit_new = shared_pref.getBoolean(SettingsActivity.SETTING_CHAT_EDIT, false);
                if (chat_edit_new != setting_chat_edit) {
                    setting_chat_edit = chat_edit_new;
                    reconnect = true;
                    token_request = true;
                }

                boolean channel_moderate_new = shared_pref.getBoolean(SettingsActivity.SETTING_CHANNEL_MODERATE, false);
                if (channel_moderate_new != setting_channel_moderate) {
                    setting_channel_moderate = channel_moderate_new;
                    reconnect = true;
                    token_request = true;
                }

                String scope = "";
                if (setting_chat_read) {
                    scope += "chat:read";
                }
                if (setting_chat_edit) {
                    if (scope.length() > 0) {
                        scope += " ";
                    }
                    scope += "chat:edit";
                }
                if (setting_channel_moderate) {
                    if (scope.length() > 0) {
                        scope += " ";
                    }
                    scope += "channel:moderate";
                }

                if (!setting_chat_read && !setting_chat_edit && !setting_channel_moderate) {
                    active_username = "justinfan1337";
                } else {
                    active_username = setting_username;
                }

                if (token_request) {
                    token_request_md5 = getRandomMD5();
                    if (prepareTokenReceive(token_request_md5)) {
                        token_request_url = "https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=<client-id>&redirect_uri=http://localhost:8765&scope=" + scope + "&state=" + token_request_md5;
                        if (token_force_verify) {
                            token_request_url += "&force_verify=true";
                        }
                        waiting_for_token = true;
                        start_token_activity = true;
                    } else {
                        //error handling
                    }
                }
            }
        }
        if (!waiting_for_token && reconnect) {
            reconnectIRC();
        }
    }

    public static void reconnectIRC() {
        Log.d("MainActivity", "reconnectIRC!");
        setSettings(setting_channel, setting_username, setting_token);
        connect();
    }

    public static void updateToken(String token) {
        Log.d("MainActivity", "updateToken!");
        setting_token = token;
        waiting_for_token = false;

        reconnectIRC();
    }

    public void startTokenActivity() {
        if (start_token_activity) {
            start_token_activity = false;
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(token_request_url));
            try {
                startActivity(i);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Token request failed!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private static IRCMessage[] ircMessages = null;

    public static void initIRCMessages() {
        if (ircMessages == null) {
            Log.d("MainActivity", "initIRCMessages!");
            ircMessages = new IRCMessage[250];
            for (int i = 0; i < ircMessages.length; i++) {
                ircMessages[i] = new IRCMessage();
                ircMessages[i].channel_id = -1L;
                ircMessages[i].badge_replace = new Vector<UpdateData.BadgeReplace>();
                ircMessages[i].name = "";
                ircMessages[i].name_color = Color.parseColor("#000000");
                ircMessages[i].message = "";
                ircMessages[i].emote_replace = new Vector<UpdateData.EmoteReplace>();
            }
        }
    }

    private static String getRandomMD5() {
        StringBuilder result = new StringBuilder();
        String[] md5_alphabet = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };
        Random r = new Random();
        for (int i = 0; i < 32; i++) {
            result.append(md5_alphabet[r.nextInt(16)]);
        }
        return result.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initNative();

        if (executor_service_token == null) {
            executor_service_token = Executors.newSingleThreadExecutor();
        } else if (executor_service_token.isTerminated()) {
            executor_service_token = Executors.newSingleThreadExecutor();
        }
        if (executor_service == null) {
            executor_service = Executors.newSingleThreadExecutor();
        } else if (executor_service.isTerminated()) {
            executor_service = Executors.newSingleThreadExecutor();
        }
        if (main_thread_handler == null) {
            main_thread_handler = HandlerCompat.createAsync(Looper.getMainLooper());
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(binding.getRoot());

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        shared_pref = PreferenceManager.getDefaultSharedPreferences(this);

        updateSharedPreferences();
        if (waiting_for_token) {
            Log.d("MainActivity", "executor_service_token execute!");
            startTokenActivity();
            executor_service_token.execute(new UpdateToken(getApplicationContext()));
        } else {
            reconnectIRC();
        }

        initIRCMessages();

        chatRecyclerView = binding.chatView;
        chatLayoutManager = new LinearLayoutManager(getBaseContext());
        chatRecyclerView.setLayoutManager(chatLayoutManager);
        chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    if (!recyclerView.canScrollVertically(1)) {
                        chatAtBottom = true;
                    } else {
                        chatAtBottom = false;
                    }
                }
        });
        chatAdapter = new ChatAdapter(ircMessages);
        chatRecyclerView.setAdapter(chatAdapter);

        executor_service.execute(new UpdateData(getApplicationContext()));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public static native void initNative();
    public static native String connect();
    public static native void setSettings(String channel, String username, String token);
    public native static boolean prepareTokenReceive(String code);
}