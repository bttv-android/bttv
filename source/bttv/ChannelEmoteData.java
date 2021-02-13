package bttv;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChannelEmoteData {
    final String id;
    final List<Emote> channelEmotes;
    final List<Emote> sharedEmotes;

    public ChannelEmoteData(String id, List<Emote> channelEmotes, List<Emote> sharedEmotes) {
        this.id = id;
        this.channelEmotes = channelEmotes;
        this.sharedEmotes = sharedEmotes;
    }

    public static ChannelEmoteData fromJson(String json) {
        JSONObject reader = new JSONObject(json);
        String id = reader.getString("id");
        JSONArray channelEmotes = reader.getJSONArray("channelEmotes");
        JSONArray sharedEmotes = reader.getJSONArray("sharedEmotes");

        List<Emote> realChannelEmotes = Emote.fromJSONArray(channelEmotes);
        List<Emote> realSharedEmotes = Emote.fromJSONArray(sharedEmotes);

        return new ChannelEmoteData(id, realChannelEmotes, realSharedEmotes);
    }

    @Override
    public String toString() {
        return "ChannelEmoteData(id: " + id + ", channelEm:" + channelEmotes.toString() + ", sharedEm:"
                + sharedEmotes.toString() + ")";
    }
}
