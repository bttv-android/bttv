package bttv;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import bttv.emote.ChannelEmoteData;
import bttv.emote.Emote;
import bttv.emote.Emotes;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Network {

    private static final int GLOBAL_CHANNEL_ID = -2;
    private static final String BTTV_API_HOST = "https://api.betterttv.net";
    private static final String STV_API_HOST = "https://api.7tv.app/v2";

    public static void get(String url, Callback cb) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "bttv-android")
                .build();
        client.newCall(request).enqueue(cb);
    }

    private static class ResponseHandler implements Callback {
        private final Emotes.Source source;
        private final int channelId;

        public ResponseHandler(Emotes.Source source, int channelId) {
            this.source = source;
            this.channelId = channelId;
        }

        @Override
        public final void onFailure(Call call, @NotNull IOException e) {
            Log.e("LBTTVNetworkFail", "Call failed", e);
            Log.e("LBTTVNetworkFail", call.toString());
        }

        @Override
        public final void onResponse(@NotNull Call call, Response response) throws IOException {
            try (ResponseBody responseBody = response.body()) {
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                String json = responseBody.string();


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
                e.printStackTrace();
                Log.e("LBTTVNetwork", e.getMessage(), e);
            }
        }
    }

    public static void getBTTVGlobalEmotes() {
        get(BTTV_API_HOST + "/3/cached/emotes/global", new ResponseHandler(Emotes.Source.BTTV, GLOBAL_CHANNEL_ID));
    }

    public static void getFFZGlobalEmotes() {
        get(BTTV_API_HOST + "/3/cached/frankerfacez/emotes/global", new ResponseHandler(Emotes.Source.FFZ, GLOBAL_CHANNEL_ID));
    }

    public static void get7TVGlobalEmotes() {
        get(STV_API_HOST + "/emotes/global", new ResponseHandler(Emotes.Source.STV, GLOBAL_CHANNEL_ID));
    }

    public static void getBTTVChannelEmotes(int channelId) {
        get(BTTV_API_HOST + "/3/cached/users/twitch/" + channelId, new ResponseHandler(Emotes.Source.BTTV, channelId));
    }

    public static void getFFZChannelEmotes(int channelId) {
        get(BTTV_API_HOST + "/3/cached/frankerfacez/users/twitch/" + channelId,
                new ResponseHandler(Emotes.Source.FFZ, channelId));
    }

    public static void get7TVChannelEmotes(int channelId) {
        get(STV_API_HOST + "/users/" + channelId + "/emotes",
                new ResponseHandler(Emotes.Source.STV, channelId));
    }

}
