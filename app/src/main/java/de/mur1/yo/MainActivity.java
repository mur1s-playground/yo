package de.mur1.yo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.mur1.yo.databinding.ActivityMainBinding;

interface MainActivityCallback {
    void onComplete();
}

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private ActivityMainBinding binding;

    private ExecutorService executor_service = null;
    private Handler main_thread_handler = null;

    public static RecyclerView chatRecyclerView;
    public static ChatAdapter chatAdapter;
    public static RecyclerView.LayoutManager chatLayoutManager;
    public static Boolean chatAtBottom = true;

    private IRCMessage[] ircMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        executor_service = Executors.newSingleThreadExecutor();
        main_thread_handler = HandlerCompat.createAsync(Looper.getMainLooper());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

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

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}