package bttv;

import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import bttv.emote.ChannelEmoteData;
import bttv.emote.Emote;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Network {

    private static final String BTTV_API_HOST = "https://api.betterttv.net";

    public static void get(String url, Callback cb) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(cb);
    }

    private static class ResponseHandler implements Callback {
        private int source;
        private int channelId;

        public ResponseHandler(int source, int channelId) {
            this.source = source;
            this.channelId = channelId;
        }

        @Override
        public final void onFailure(Call call, IOException e) {
            Log.e("LBTTVNetworkFail", "Call failed", e);
            Log.e("LBTTVNetworkFail", call.toString());
        }

        @Override
        public final void onResponse(Call call, Response response) throws IOException {
            try (ResponseBody responseBody = response.body()) {
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                String json = responseBody.string();

                if (channelId == -2) {
                    // Global
                    Data.setGlobal(Emote.fromJSONArray(json, source), source);
                } else {
                    // Channel
                    if (source == Emote.BTTV) {
                        ChannelEmoteData chEmData = ChannelEmoteData.fromJson(json);
                        Data.addChannel(channelId, chEmData);
                    } else if (source == Emote.FFZ) {
                        List<Emote> emotes = Emote.fromJSONArray(json, source);
                        Data.addChannel(channelId, emotes);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static void getBTTVGlobalEmotes() {
        get(BTTV_API_HOST + "/3/cached/emotes/global", new ResponseHandler(Emote.BTTV, -2));
    }

    public static void getFFZGlobalEmotes() {
        get(BTTV_API_HOST + "/3/cached/frankerfacez/emotes/global", new ResponseHandler(Emote.FFZ, -2));
    }

    public static void getBTTVChannelEmotes(int channelId) {
        get(BTTV_API_HOST + "/3/cached/users/twitch/" + channelId, new ResponseHandler(Emote.BTTV, channelId));
    }

    public static void getFFZChannelEmotes(int channelId) {
        get(BTTV_API_HOST + "/3/cached/frankerfacez/users/twitch/" + channelId,
                new ResponseHandler(Emote.FFZ, channelId));
    }

}
