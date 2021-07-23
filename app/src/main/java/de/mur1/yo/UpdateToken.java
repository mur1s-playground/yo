package de.mur1.yo;

import android.content.Context;
import android.os.Handler;

public class UpdateToken implements Runnable {
    Context context;

    public UpdateToken(Context context) {
        this.context = context;
    }

    public void run() {
        while (true) {
            String token = getTokenIfReady();
            if (token.length() > 0) {
                Handler mainHandler = new Handler(context.getMainLooper());
                Runnable updateUI = new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.updateToken(token);
                    }
                };
                mainHandler.post(updateUI);
                break;
            }
            try {
                Thread.sleep(100);
            } catch(Exception e) {

            }
        }
    }

    public native String getTokenIfReady();
}
