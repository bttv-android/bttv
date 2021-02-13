package bttv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Data {
    public static int currentBroadcasterId = -1;
    public static ConcurrentHashMap<Integer, Set<String>> availEmoteSetMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Emote> emoteMap = new ConcurrentHashMap<>(); // maps code to emote
    public static List<Emote> globalEmotes = new ArrayList<>(); // replace with map in the future

    public static void setCurrentBroadcasterId(int id) {
        Log.i("BTTVDataSetCurrentBroadcasterId", currentBroadcasterId + " -> " + id);
        Data.currentBroadcasterId = id;
        ensureChannelEmotes(id);
    }

    private static void ensureChannelEmotes(int id) {
        if (!availEmoteSetMap.containsKey(id)) {
            fetchChannelEmotes(id);
        }
    }

    private static void fetchChannelEmotes(final int id) {
        final boolean globalFirst = Data.globalEmotes.isEmpty();

        String url = "https://api.betterttv.net/3/cached/users/twitch/" + id;
        if (globalFirst) {
            url = "https://api.betterttv.net/3/cached/emotes/global";
        }

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
                    if (globalFirst) {
                        Data.globalEmotes = Emote.fromJSONArray(res);
                        for (Emote emote : Data.globalEmotes) {
                            emoteMap.put(emote.code, emote);
                        }
                        ensureChannelEmotes(id); // now fetch it for real
                    } else {
                        ChannelEmoteData channelEmoteData = ChannelEmoteData.fromJson(res);
                        Set<String> set = new HashSet<>();
                        for (Emote emote : channelEmoteData.channelEmotes) {
                            set.add(emote.code);
                            emoteMap.put(emote.code, emote);
                        }
                        for (Emote emote : channelEmoteData.sharedEmotes) {
                            set.add(emote.code);
                            emoteMap.put(emote.code, emote);
                        }
                        for (Emote emote : Data.globalEmotes) {
                            set.add(emote.code);
                            emoteMap.put(emote.code, emote);
                        }

                        Data.availEmoteSetMap.put(id, set);
                    }
                }
            }
        });

    }

}
