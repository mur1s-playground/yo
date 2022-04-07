package de.mur1.yolog;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class UpdateChat implements Runnable {
    Semaphore       lock                        = new Semaphore(1, true);
    boolean         running                     = true;

    ArrayList<String>   chat_chunks                 = new ArrayList<String>();
    long                last_chat_chunk_timestamp   = 0;

    long                last_chat_chunk_timestamp_override      = 0;
    boolean             last_chat_chunk_timestamp_override_set  = false;

    public long getLastChatChunkTimestamp() {
        long result = 0;
        try {
            lock.acquire();
            result = last_chat_chunk_timestamp;
            lock.release();
        } catch (Exception e) {}
        return result;
    }

    public void overrideLastChatChunkTimestamp(long ts) {
        try {
            Log.d("UpdateChat", "ts override write");
            lock.acquire();
            last_chat_chunk_timestamp_override = ts;
            last_chat_chunk_timestamp_override_set = true;
            lock.release();
        } catch (Exception e) {}
    }

    private long getLastChatChunkTimestampOverride() {
        long ts = Long.MAX_VALUE;
        try {
            lock.acquire();
            if (last_chat_chunk_timestamp_override_set) {
                Log.d("UpdateChat", "ts override read");
                ts = last_chat_chunk_timestamp_override;
                last_chat_chunk_timestamp_override_set = false;
            }
            lock.release();
        } catch (Exception e) {}
        return ts;
    }

    private void addChatChunk(long ts, String chat_chunk) {
        try {
            lock.acquire();
            last_chat_chunk_timestamp = ts;
            chat_chunks.add(chat_chunk);
            lock.release();
        } catch (Exception e) {}
    }

    public String getChatChunk(int[] size) {
        String result = null;
        try {
            lock.acquire();
            size[0] = chat_chunks.size();
            if (size[0] > 0) {
                result = chat_chunks.get(0);
                chat_chunks.remove(0);
            }
            lock.release();
        } catch (Exception e) {}
        return result;
    }

    private String getContent(int timeout, String from_url) {
        String result = null;
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(from_url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout * 1000);
            connection.setReadTimeout(timeout * 1000);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            int file_length = connection.getContentLength();

            input = connection.getInputStream();
            output = new ByteArrayOutputStream();

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
            }
            ByteArrayOutputStream baos = (ByteArrayOutputStream) output;
            result = new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) output.close();
                if (input != null) input.close();
            } catch (IOException e) { }

            if (connection != null) connection.disconnect();
        }
        return result;
    }

    private void getList() {
        Log.d("UpdateChat", "getList");

        String server_ip = Settings.getStringSetting(Settings.SETTING_CHAT_SERVER_IP);
        int server_port = Settings.getIntSetting(Settings.SETTING_CHAT_SERVER_PORT);

        int timeout = Settings.getIntSetting(Settings.SETTING_CHAT_UPDATE_TIMEOUT_SECONDS);

        String file_index = getContent(timeout, "http://" + server_ip + ":" + server_port);

        if (file_index != null) {
            try {
                String result_lines[] = file_index.split("\n");
                for (int l = 0; l < result_lines.length; l++) {
                    String line = result_lines[l];
                    if (line.length() > 0) {
                        if (line.endsWith("</a>")) {
                            String[] ls = line.split(" ");
                            ArrayList<String> nonempty_ls = new ArrayList<String>();
                            for (int lss = 0; lss < ls.length; lss++) {
                                if (ls[lss].length() > 0) {
                                    nonempty_ls.add(ls[lss]);
                                }
                            }
                            int size = Integer.parseInt(nonempty_ls.get(nonempty_ls.size() - 4));
                            long ts = Long.parseLong(nonempty_ls.get(nonempty_ls.size() - 1).split(">")[1].split("<")[0]);

                            if (ts > last_chat_chunk_timestamp && size > 0) {
                                Log.d("UpdateChat", "fetch: " + ts);
                                String chat_chunk = getContent(timeout, "http://" + server_ip + ":" + server_port + "/" + ts);
                                if (chat_chunk != null) {
                                    addChatChunk(ts, chat_chunk);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            };
        } else {
            Log.d("UpdateChat", "file_index null");
        }
        long ts = getLastChatChunkTimestampOverride();
        if (ts != Long.MAX_VALUE) {
            last_chat_chunk_timestamp = ts;
        }
    }

    public void run() {
        while (running) {
            int update_interval = Settings.getIntSetting(Settings.SETTING_CHAT_UPDATE_INTERVAL_SECONDS);

            getList();

            try {
                Thread.sleep(update_interval * 1000);
            } catch (Exception e) {};
        }
    }
}
