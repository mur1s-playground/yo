package de.mur1.yo;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    public static CheckBoxPreference check_box_preference_chat_read;
    public static CheckBoxPreference check_box_preference_chat_edit;
    public static CheckBoxPreference check_box_preference_channel_moderate;
    public static EditTextPreference edit_text_preference_token;

    public static void setChatSettingsEnabled(boolean enable) {
        if (SettingsFragment.check_box_preference_chat_read != null) {
            SettingsFragment.check_box_preference_chat_read.setEnabled(enable);
            SettingsFragment.check_box_preference_chat_edit.setEnabled(enable);
            SettingsFragment.check_box_preference_channel_moderate.setEnabled(enable);
        }
    }

    public static void setEditTextPreferenceToken(String token) {
        edit_text_preference_token.setText(token);
    }

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        check_box_preference_chat_read = this.findPreference(SettingsActivity.SETTING_CHAT_READ);
        check_box_preference_chat_edit = this.findPreference(SettingsActivity.SETTING_CHAT_EDIT);
        check_box_preference_channel_moderate = this.findPreference(SettingsActivity.SETTING_CHANNEL_MODERATE);
        edit_text_preference_token = this.findPreference(SettingsActivity.SETTING_TOKEN);

        setChatSettingsEnabled(!MainActivity.setting_username.startsWith("justinfan"));
    }

    public void onSharedPreferenceChange(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SettingsActivity.SETTING_USERNAME)) {
            String username = sharedPreferences.getString(SettingsActivity.SETTING_USERNAME, "justinfan1337");
            setChatSettingsEnabled(!username.startsWith("justinfan"));
        }
    }

}