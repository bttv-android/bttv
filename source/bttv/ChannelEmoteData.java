package bttv;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class ChannelEmoteData {
    final List<Emote> channelEmotes;
    final List<Emote> sharedEmotes;

    public ChannelEmoteData(List<Emote> channelEmotes, List<Emote> sharedEmotes) {
        this.channelEmotes = channelEmotes;
        this.sharedEmotes = sharedEmotes;
    }

    public static ChannelEmoteData fromJson(String json) {
        Log.i("LBTTVChannelEmoteData", json);
        JSONObject reader = new JSONObject(json);
        JSONArray channelEmotes = reader.getJSONArray("channelEmotes");
        JSONArray sharedEmotes = reader.getJSONArray("sharedEmotes");

        List<Emote> realChannelEmotes = Emote.fromJSONArray(channelEmotes, Emote.BTTV);
        List<Emote> realSharedEmotes = Emote.fromJSONArray(sharedEmotes, Emote.BTTV);

        return new ChannelEmoteData(realChannelEmotes, realSharedEmotes);
    }

    @Override
    public String toString() {
        return "ChannelEmoteData(channelEm:" + channelEmotes.toString() + ", sharedEm:" + sharedEmotes.toString() + ")";
    }
}
