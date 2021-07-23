package de.mur1.yo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

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

    public static String setting_channel = "";
    public static String setting_username = "";
    public static String setting_token = "";
    public static boolean setting_chat_read = false;
    public static boolean setting_chat_edit = false;
    public static boolean setting_channel_moderate = false;

    public static String token_request_md5 = "";
    public static String active_username = "";

    public static boolean waiting_for_token = false;
    public static boolean irc_connected = false;

    public static void updateToken(String token) {
        setting_token = token;
        //SettingsFragment.setEditTextPreferenceToken(token);
        waiting_for_token = false;
        Log.d("Setting Token", "!");

        setSettings(setting_channel, setting_username, setting_token);
        connect();
    }

    private static IRCMessage[] ircMessages = null;

    public static void initIRCMessages() {
        if (ircMessages == null) {
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

    private String getRandomMD5() {
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
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("waiting_for_token", waiting_for_token);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        waiting_for_token = savedInstanceState.getBoolean("waiting_for_token", false);
    }

    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        executor_service_token = Executors.newSingleThreadExecutor();
        executor_service = Executors.newSingleThreadExecutor();
        main_thread_handler = HandlerCompat.createAsync(Looper.getMainLooper());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(binding.getRoot());

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        setting_channel = sharedPref.getString(SettingsActivity.SETTING_CHANNEL, "m1_1m");
        setting_username = sharedPref.getString(SettingsActivity.SETTING_USERNAME, "justinfan1337");
        if (setting_token.length() == 0) {
            setting_token = sharedPref.getString(SettingsActivity.SETTING_TOKEN, "");
        }
        setting_chat_read = sharedPref.getBoolean(SettingsActivity.SETTING_CHAT_READ, false);
        setting_chat_edit = sharedPref.getBoolean(SettingsActivity.SETTING_CHAT_EDIT, false);
        setting_channel_moderate = sharedPref.getBoolean(SettingsActivity.SETTING_CHANNEL_MODERATE, false);

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
        if (!active_username.startsWith("justinfan")) {
            if (!waiting_for_token) {
                if (setting_token.length() == 0) {
                    token_request_md5 = getRandomMD5();
                    if (prepareTokenReceive(token_request_md5)) {
                        String url = "https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=<client-id>&redirect_uri=http://localhost:8765&scope=" + scope + "&state=" + token_request_md5;
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        try {
                            waiting_for_token = true;
                            startActivity(i);
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), "Token request failed!", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Token receive could not be prepared!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }

        if (waiting_for_token) {
            Log.d("Executing UpdateToken", "!");
            executor_service_token.execute(new UpdateToken(getApplicationContext()));
        } else {
            irc_connected = true;
            Log.d("Executing IRCConnect", "onCreate!");

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
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!irc_connected) {
            irc_connected = true;
            Log.d("Executing IRCConnect", "onResume!");

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
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public static native String connect();
    public static native void setSettings(String channel, String username, String token);
    public native boolean prepareTokenReceive(String code);
}