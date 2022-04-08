package de.mur1.yolog;

import static java.lang.Math.abs;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Util {
    public static byte[] file_read(String filename) {
        File file = new File(filename);
        if (!file.exists()) return null;

        int size = (int) file.length();
        byte[] bytes = new byte[size];

        try {
            FileInputStream buf = new FileInputStream(file);
            int bytes_read = 0;
            while (bytes_read < size) {
                bytes_read += buf.read(bytes, bytes_read, bytes.length-bytes_read);
            }
            buf.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            bytes = null;
        }
        return bytes;
    }

    public static void file_write(String filename, byte[] content) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(filename);
            os.write(content);
        } catch (Exception e) {} finally {
            try {
                if (os != null) os.close();
            } catch (Exception e) {};
        }
    }

    public static int color_diff(int color1, int color2) {
        String color1_hex = String.format("%06X", (0xFFFFFF & color1));
        String color1_r16 = color1_hex.substring(0, 2);
        String color1_g16 = color1_hex.substring(2, 4);
        String color1_b16 = color1_hex.substring(4, 6);
        int color1_r = Integer.valueOf(color1_r16.charAt(0))*16 + Integer.valueOf(color1_r16.charAt(1));
        int color1_g = Integer.valueOf(color1_g16.charAt(0))*16 + Integer.valueOf(color1_g16.charAt(1));
        int color1_b = Integer.valueOf(color1_b16.charAt(0))*16 + Integer.valueOf(color1_b16.charAt(1));
        int color1_ = color1_r + color1_g + color1_b;

        String color2_hex = String.format("%06X", (0xFFFFFF & color2));
        String color2_r16 = color2_hex.substring(0, 2);
        String color2_g16 = color2_hex.substring(2, 4);
        String color2_b16 = color2_hex.substring(4, 6);
        int color2_r = Integer.valueOf(color2_r16.charAt(0))*16 + Integer.valueOf(color2_r16.charAt(1));
        int color2_g = Integer.valueOf(color2_g16.charAt(0))*16 + Integer.valueOf(color2_g16.charAt(1));
        int color2_b = Integer.valueOf(color2_b16.charAt(0))*16 + Integer.valueOf(color2_b16.charAt(1));
        int color2_ = color2_r + color2_g + color2_b;

        return abs(color1_ - color2_);
    }
}
