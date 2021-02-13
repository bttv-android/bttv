package bttv;

import java.util.ArrayList;
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

        ArrayList<Emote> realChannelEmotes = new ArrayList<>(channelEmotes.length());
        for (int i = 0; i < channelEmotes.length(); i++) {
            JSONObject obj = channelEmotes.getJSONObject(i);
            realChannelEmotes.add(Emote.fromJson(obj));
        }

        ArrayList<Emote> realSharedEmotes = new ArrayList<>(sharedEmotes.length());
        for (int i = 0; i < sharedEmotes.length(); i++) {
            JSONObject obj = sharedEmotes.getJSONObject(i);
            realSharedEmotes.add(Emote.fromJson(obj));
        }

        return new ChannelEmoteData(id, realChannelEmotes, realSharedEmotes);
    }

    @Override
    public String toString() {
        return "ChannelEmoteData(id: " + id + ", channelEm:" + channelEmotes.toString() + ", sharedEm:"
                + sharedEmotes.toString() + ")";
    }
}
