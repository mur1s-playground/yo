package de.mur1.yolog;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.collection.ArrayMap;

import java.util.ArrayList;

public class ThemeManager {
    public static final String      light   = "LIGHT";
    public static final String      darcula = "DARCULA";

    public static Theme active_theme = null;
    public static ArrayMap<String, Theme> themes = new ArrayMap<String, Theme>();

    public static void init() {
        Theme light_t = new Theme();
        light_t.text_color = Color.parseColor("#000000");
        light_t.background_color = Color.parseColor("#CCCCCC");
        light_t.background_color_inv = Color.parseColor("#333333");
        light_t.button_background_color = Color.parseColor("#AAAAAA");

        themes.put(light, light_t);

        Theme darcula_t = new Theme();
        darcula_t.text_color = Color.parseColor("#a9b7c6");
        darcula_t.background_color = Color.parseColor("#2b2b2b");
        darcula_t.background_color_inv = Color.parseColor("#c4c4c4");
        darcula_t.button_background_color = Color.parseColor("#4b4b4b");

        themes.put(darcula, darcula_t);

        active_theme = darcula_t;
    }

    public static void setTheme(String name) {
        active_theme = themes.get(name);
    }

    public static String[] getAvailableThemeNames() {
        String[] themes = {light, darcula};
        return themes;
    }

    public static void applyTheme(View view) {
        view.setBackgroundColor(active_theme.background_color);

        ArrayList<View> children = getChildren(view);
        for (int v = 0; v < children.size(); v++) {
            View view1 = children.get(v);
            if (view1 instanceof ImageButton) {
                ImageButton ibutton = (ImageButton) view1;
                ibutton.setBackgroundColor(active_theme.button_background_color);
                ibutton.setColorFilter(active_theme.text_color);
            } else if (view1 instanceof TextView) {
                TextView itext = (TextView) view1;
                itext.setBackgroundColor(active_theme.background_color);
                itext.setTextColor(active_theme.text_color);
            } else if (view1 instanceof Spinner) {
                Spinner ispinner = (Spinner) view1;
                ispinner.getBackground().setColorFilter(active_theme.text_color, PorterDuff.Mode.SRC_ATOP);
            } else if (view1 instanceof SeekBar) {
                SeekBar iseekbar = (SeekBar) view1;
                iseekbar.setBackgroundColor(active_theme.background_color);
                iseekbar.getProgressDrawable().setColorFilter(active_theme.text_color, PorterDuff.Mode.MULTIPLY);
                iseekbar.getProgressDrawable().setColorFilter(active_theme.text_color, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    public static ArrayList<View> getChildren(View v) {
        if (!(v instanceof ViewGroup)) {
            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            return viewArrayList;
        }

        ArrayList<View> result = new ArrayList<View>();
        ViewGroup vg = (ViewGroup) v;
        for (int i = 0; i < vg.getChildCount(); i++) {

            View child = vg.getChildAt(i);

            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            viewArrayList.addAll(getChildren(child));

            result.addAll(viewArrayList);
        }
        return result;
    }
}
