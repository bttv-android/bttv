package bttv;

import android.util.Log;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bttv.emote.ChannelEmoteData;
import bttv.emote.Emote;
import bttv.emote.Emotes;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class Network {

    private static final int GLOBAL_CHANNEL_ID = -2;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Set<String> finishedRequests = Collections.synchronizedSet(new HashSet<String>());
    public static final String BTTV_API_HOST = "https://api.betterttv.net";
    public static final String FFZ_API_HOST = "https://api.frankerfacez.com/v1";
    public static final String STV_API_HOST = "https://7tv.io/v3";
    public static final String CHATTERINO_API_HOST = "https://api.chatterino.com";

    public static void get(String url, Callback cb) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "bttv-android")
                .build();
        client.newCall(request).enqueue(cb);
    }

    // does not depend on okhttp, as it is not available in obfuscated Twitch builds
    private static void getEmotes(String url, Emotes.Source source, int channelId) {
        finishedRequests.remove(requestKey(source, channelId));
        EXECUTOR.execute(new EmoteRequest(url, source, channelId));
    }

    private static String requestKey(Emotes.Source source, int channelId) {
        return source + ":" + channelId;
    }

    /** @return true if the last request for the emotes of this channel is done, no matter if it failed */
    public static boolean isChannelRequestFinished(Emotes.Source source, int channelId) {
        return finishedRequests.contains(requestKey(source, channelId));
    }

    private static class EmoteRequest implements Runnable {
        private final String url;
        private final Emotes.Source source;
        private final int channelId;

        public EmoteRequest(String url, Emotes.Source source, int channelId) {
            Log.d("LBTTV", "EmoteRequest: "  + source.toString() + " ch: " + channelId);
            this.url = url;
            this.source = source;
            this.channelId = channelId;
        }

        @Override
        public final void run() {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "bttv-android");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300)
                    throw new IOException("Unexpected code " + code + " for " + url);

                String json = readAll(connection.getInputStream());

                if (channelId == GLOBAL_CHANNEL_ID) {
                    // Global
                    Emotes.setGlobal(Emote.fromJSONArray(json, source), source);
                } else {
                    // Channel
                    switch (source) {
                        case BTTV:
                            ChannelEmoteData chEmData = ChannelEmoteData.fromJson(json);
                            Emotes.addChannelBTTV(channelId, chEmData);
                            break;
                        case FFZ:
                            List<Emote> emotes = Emote.fromJSONArray(json, source);
                            Emotes.addChannelFFZ(channelId, emotes);
                            break;
                        case STV:
                            emotes = Emote.fromJSONArray(json, source);
                            Emotes.addChannel7TV(channelId, emotes);
                            break;
                        default:
                            Log.e("LBTTVNetwork", "Source unknown!", new Exception());
                    }
                }
            } catch (JSONException e) {
                Log.e("LBTTVNetwork", e.getMessage(), e);
            } catch (Throwable e) {
                Log.e("LBTTVNetworkFail", "Call failed: " + url, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                finishedRequests.add(requestKey(source, channelId));
            }
        }

        private static String readAll(InputStream in) throws IOException {
            try (InputStream stream = in) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return out.toString("UTF-8");
            }
        }
    }

    public static void getBTTVGlobalEmotes() {
        getEmotes(BTTV_API_HOST + "/3/cached/emotes/global", Emotes.Source.BTTV, GLOBAL_CHANNEL_ID);
    }

    public static void getFFZGlobalEmotes() {
        getEmotes(BTTV_API_HOST + "/3/cached/frankerfacez/emotes/global", Emotes.Source.FFZ, GLOBAL_CHANNEL_ID);
    }

    public static void get7TVGlobalEmotes() {
        getEmotes(STV_API_HOST + "/emote-sets/global", Emotes.Source.STV, GLOBAL_CHANNEL_ID);
    }

    public static void getBTTVChannelEmotes(int channelId) {
        getEmotes(BTTV_API_HOST + "/3/cached/users/twitch/" + channelId, Emotes.Source.BTTV, channelId);
    }

    public static void getFFZChannelEmotes(int channelId) {
        getEmotes(BTTV_API_HOST + "/3/cached/frankerfacez/users/twitch/" + channelId, Emotes.Source.FFZ, channelId);
    }

    public static void get7TVChannelEmotes(int channelId) {
        getEmotes(STV_API_HOST + "/users/twitch/" + channelId, Emotes.Source.STV, channelId);
    }

}
