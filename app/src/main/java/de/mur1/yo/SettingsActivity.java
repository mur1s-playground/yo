package de.mur1.yo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import android.content.SharedPreferences;
import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    public static final String SETTING_CHANNEL = "edit_text_preference_channel";
    public static final String SETTING_USERNAME = "edit_text_preference_username";
    public static final String SETTING_TOKEN = "edit_text_preference_token";
    public static final String SETTING_CHAT_READ = "check_box_preference_chat_read";
    public static final String SETTING_CHAT_EDIT = "check_box_preference_chat_edit";
    public static final String SETTING_CHANNEL_MODERATE = "check_box_preference_channel_moderate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
}