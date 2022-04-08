package de.mur1.yolog;

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
}
