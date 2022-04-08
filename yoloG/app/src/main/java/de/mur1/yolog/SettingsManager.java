package de.mur1.yolog;

import android.util.ArrayMap;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class SettingsManager {
    protected static class Setting {
        public static final int TYPE_STRING = 0;
        public static final int TYPE_INT    = TYPE_STRING + 1;
        public static final int TYPE_LONG   = TYPE_INT + 1;

        Object  value;
        int     type;

        public Setting(int type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    private static ArrayList<String>                       settings_names       = new ArrayList<String>();
    private static ArrayMap<String, Setting>               settings             = new ArrayMap<String, Setting>();
    private static Semaphore                               settings_lock        = new Semaphore(1, true);

    protected static void addSetting(String name, int type, Object value) {
        try {
            settings_lock.acquire();
            settings_names.add(name);
            settings.put(name, new Setting(type, value));
            settings_lock.release();
        } catch (Exception e) {
            e.printStackTrace();
        };
    }

    protected static void loadFromDisk(String filepath) {
        byte[] settings_json = Util.file_read(filepath);
        if (settings_json != null) {
            try {
                JSONObject obj = null;
                try {
                    obj = new JSONObject(new String(settings_json, StandardCharsets.UTF_8));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (obj != null) {
                    settings_lock.acquire();
                    for (int s = 0; s < settings_names.size(); s++) {
                        String setting_name = settings_names.get(s);
                        Setting setting = settings.get(setting_name);
                        try {
                            if (setting.type == Setting.TYPE_STRING) {
                                setting.value = obj.getString(setting_name);
                            } else if (setting.type == Setting.TYPE_INT) {
                                setting.value = obj.getInt(setting_name);
                            } else if (setting.type == Setting.TYPE_LONG) {
                                setting.value = obj.getLong(setting_name);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    settings_lock.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public static void writeToDisk(String filepath) {
        StringBuilder json_b = new StringBuilder();
        try {
            settings_lock.acquire();

            json_b.append("{");
            for (int s = 0; s < settings_names.size(); s++) {
                String setting_name = settings_names.get(s);
                Setting setting = settings.get(setting_name);
                json_b.append("'"+ setting_name + "':");
                if (setting.type == Setting.TYPE_STRING) {
                    json_b.append("\"" + (String)setting.value + "\"");
                } else if (setting.type == Setting.TYPE_INT) {
                    json_b.append((Integer) setting.value);
                } else if (setting.type == Setting.TYPE_LONG) {
                    json_b.append((Long) setting.value);
                }
                if (s + 1 < settings_names.size()) {
                    json_b.append(",");
                }
            }
            json_b.append("}");

            settings_lock.release();

            Util.file_write(filepath, json_b.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {};
    }

    static int                  getIntSetting(String setting_name) {
        int result = Integer.MAX_VALUE;
        try {
            settings_lock.acquire();
            Setting setting = settings.get(setting_name);
            result = (Integer) setting.value;
            settings_lock.release();
        } catch (Exception e) {
            e.printStackTrace();
        };
        return result;
    }

    static long                  getLongSetting(String setting_name) {
        long result = Long.MAX_VALUE;
        try {
            settings_lock.acquire();
            Setting setting = settings.get(setting_name);
            result = (Long) setting.value;
            settings_lock.release();
        } catch (Exception e) {
            e.printStackTrace();
        };
        return result;
    }

    static String               getStringSetting(String setting_name) {
        String result = null;
        try {
            settings_lock.acquire();
            Setting setting = settings.get(setting_name);
            result = (String) setting.value;
            settings_lock.release();
        } catch (Exception e) {
            e.printStackTrace();
        };
        return result;
    }

    static void                setIntSetting(String setting_name, int value) {
        try {
            settings_lock.acquire();
            Setting setting = settings.get(setting_name);
            setting.value = value;
            settings_lock.release();
        } catch (Exception e) {
            e.printStackTrace();
        };
    }

    static void                setLongSetting(String setting_name, long value) {
        try {
            settings_lock.acquire();
            Setting setting = settings.get(setting_name);
            setting.value = value;
            settings_lock.release();
        } catch (Exception e) {
            e.printStackTrace();
        };
    }

    static void                 setStringSetting(String setting_name, String value) {
        try {
            settings_lock.acquire();
            Setting setting = settings.get(setting_name);
            setting.value = value;
            settings_lock.release();
        } catch (Exception e) {
            e.printStackTrace();
        };
    }
}
