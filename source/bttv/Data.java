package bttv;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Data {

    public static int currentBroadcasterId = -1;
    public static Map<Integer, ChannelEmoteData> channelEmoteMap = new HashMap<>();

    public static void setCurrentBroadcasterId(int id) {
        Log.i("BTTVDataSetCurrentBroadcasterId", currentBroadcasterId + " -> " + id);
        Data.currentBroadcasterId = id;
        ensureChannelEmotes(id);
    }

    private static void ensureChannelEmotes(int id) {
        if (!channelEmoteMap.containsKey(id)) {
            fetchChannelEmotes(id);
        }
    }

    private static void fetchChannelEmotes(final int id) {
        String url = "https://api.betterttv.net/3/cached/users/twitch/" + id;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("BTTVDataFetchChannelEmotes", "Call failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);
                    String res = responseBody.string();
                    Log.d("BTTVDataFetchChannelEmotes", res);
                    ChannelEmoteData channelEmoteData = ChannelEmoteData.fromJson(res);
                    Log.d("BTTVDataFetchChannelEmotes", channelEmoteData.toString());
                    Data.channelEmoteMap.put(id, channelEmoteData);
                }
            }
        });

    }

}
