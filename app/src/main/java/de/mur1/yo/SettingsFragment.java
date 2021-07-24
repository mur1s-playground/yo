package de.mur1.yo;

import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    public CheckBoxPreference check_box_preference_chat_read = null;
    public CheckBoxPreference check_box_preference_chat_edit = null;
    public CheckBoxPreference check_box_preference_channel_moderate = null;
    public EditTextPreference edit_text_preference_username = null;
    public EditTextPreference edit_text_preference_token = null;

    public void setChatSettingsEnabled(boolean enable) {
        if (this.check_box_preference_chat_read != null) {
            this.check_box_preference_chat_read.setEnabled(enable);
            this.check_box_preference_chat_edit.setEnabled(enable);
            this.check_box_preference_channel_moderate.setEnabled(enable);
        }
    }

    public SettingsFragment() { }

    public void onResume() {
        super.onResume();
        check_box_preference_chat_read = this.findPreference(SettingsActivity.SETTING_CHAT_READ);
        check_box_preference_chat_edit = this.findPreference(SettingsActivity.SETTING_CHAT_EDIT);
        check_box_preference_channel_moderate = this.findPreference(SettingsActivity.SETTING_CHANNEL_MODERATE);
        edit_text_preference_username = this.findPreference(SettingsActivity.SETTING_USERNAME);
        if (edit_text_preference_username != null) {
            edit_text_preference_username.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    edit_text_preference_username.setText((String)newValue);
                    setChatSettingsEnabled(!((String) newValue).startsWith("justinfan"));
                    return false;
                }
            });
        }
        edit_text_preference_token = this.findPreference(SettingsActivity.SETTING_TOKEN);
        if (edit_text_preference_token != null) {
            edit_text_preference_token.setText(MainActivity.setting_token);
            if (edit_text_preference_token.getText().length() > 0) {
                edit_text_preference_token.setEnabled(true);
            } else {
                edit_text_preference_token.setEnabled(false);
            }
        }
        setChatSettingsEnabled(!MainActivity.setting_username.startsWith("justinfan"));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }
}