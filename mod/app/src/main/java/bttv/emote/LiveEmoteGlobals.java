package bttv.emote;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Global live-chat catalogs, deliberately independent of legacy OkHttp classes. */
final class LiveEmoteGlobals {
    private static final String TAG = "BTTV31TRACE";
    private static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;

    private LiveEmoteGlobals() {}

    static void load(final Emotes.Source source) {
        final String url;
        switch (source) {
            case BTTV:
                url = "https://api.betterttv.net/3/cached/emotes/global";
                break;
            case FFZ:
                url = "https://api.betterttv.net/3/cached/frankerfacez/emotes/global";
                break;
            case STV:
                url = "https://7tv.io/v3/emote-sets/global";
                break;
            default:
                return;
        }
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Emotes.setGlobal(Emote.fromJSONArray(fetch(url), source), source);
                } catch (Throwable failure) {
                    // A provider or dependency failure must never escape into Twitch chat.
                    Log.w(TAG, "Global emote catalog unavailable: " + source
                            + " (" + failure.getClass().getSimpleName() + ")");
                }
            }
        }, "bttv31-global-" + source.name());
        worker.setDaemon(true);
        worker.start();
    }

    static final class HttpFailure extends IOException {
        final int status;

        HttpFailure(int status) {
            super("HTTP " + status);
            this.status = status;
        }
    }

    static String fetch(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(7_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "bttv-android");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new HttpFailure(status);
            }
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8_192];
                int total = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_RESPONSE_BYTES) {
                        throw new IOException("Global emote catalog too large");
                    }
                    output.write(buffer, 0, read);
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }
}
