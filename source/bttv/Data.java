package bttv;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;

public class Data {
    public static int currentBroadcasterId = -1;

    private static ConcurrentHashMap<Integer, Set<String>> availBTTVEmoteSetMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, Set<String>> availFFZEmoteSetMap = new ConcurrentHashMap<>();

    // map code to emote
    private static ConcurrentHashMap<String, Emote> codeEmoteMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Emote> idEmoteMap = new ConcurrentHashMap<>();
    private static HashMap<String, Emote> globalBTTVEmotes = new HashMap<>();
    private static HashMap<String, Emote> globalFFZEmotes = new HashMap<>();

    public static void setCurrentBroadcasterId(int id) {
        Log.i("BTTVDataSetCurrentBroadcasterId", currentBroadcasterId + " -> " + id);
        Data.currentBroadcasterId = id;
        ensureChannelEmotes(id);
    }

    public static boolean channelHasEmotes(int id) {
        boolean hasBTTVEmotes = availBTTVEmoteSetMap.containsKey(id);
        boolean hasFFZEmotes = availFFZEmoteSetMap.containsKey(id);
        boolean globalBTTVLoaded = !globalBTTVEmotes.isEmpty();
        boolean globalFFZLoaded = !globalFFZEmotes.isEmpty();
        return hasBTTVEmotes || hasFFZEmotes || globalBTTVLoaded || globalFFZLoaded;
    }

    public static boolean isEmote(String code, int channelId) {
        if (globalFFZEmotes.containsKey(code)) {
            return true;
        }
        if (globalBTTVEmotes.containsKey(code)) {
            return true;
        }
        Set<String> bttvSet = availBTTVEmoteSetMap.get(channelId);
        if (bttvSet != null && bttvSet.contains(code)) {
            return true;
        }
        Set<String> ffzSet = availFFZEmoteSetMap.get(channelId);
        if (ffzSet != null && ffzSet.contains(code)) {
            return true;
        }
        return false;
    }

    public static Emote getEmote(String code) {
        return codeEmoteMap.get(code);
    }

    public static Emote getEmoteById(String id) {
        return idEmoteMap.get(id);
    }

    private static void ensureChannelEmotes(int id) {
        if (globalBTTVEmotes.isEmpty()) {
            Network.getBTTVGlobalEmotes();
        }
        if (globalFFZEmotes.isEmpty()) {
            Network.getFFZGlobalEmotes();
        }
        if (!availBTTVEmoteSetMap.containsKey(id)) {
            Network.getBTTVChannelEmotes(id);
        }
        if (!availFFZEmoteSetMap.containsKey(id)) {
            Network.getFFZChannelEmotes(id);
        }
    }

    // FFZ
    public static void addChannel(int id, List<Emote> emotes) {
        Set<String> set = new HashSet<>();
        for (Emote emote : emotes) {
            set.add(emote.code);
            codeEmoteMap.put(emote.code, emote);
            idEmoteMap.put(emote.id, emote);
        }

        Data.availFFZEmoteSetMap.put(id, set);
        Log.d("BTTVData", "Channel FFZ: " + set.toString());
    }

    // BTTV
    public static void addChannel(int id, ChannelEmoteData chEmData) {
        Set<String> set = new HashSet<>();
        for (Emote emote : chEmData.channelEmotes) {
            set.add(emote.code);
            codeEmoteMap.put(emote.code, emote);
            idEmoteMap.put(emote.id, emote);
        }
        for (Emote emote : chEmData.sharedEmotes) {
            set.add(emote.code);
            codeEmoteMap.put(emote.code, emote);
            idEmoteMap.put(emote.id, emote);
        }

        Data.availBTTVEmoteSetMap.put(id, set);
        Log.d("BTTVData", "Channel BTTV: " + set.toString());

    }

    public static void setGlobal(List<Emote> emotes, int source) {
        HashMap<String, Emote> map = new HashMap<>();
        for (Emote emote : emotes) {
            map.put(emote.code, emote);
            codeEmoteMap.put(emote.code, emote);
            idEmoteMap.put(emote.id, emote);
        }
        if (source == Emote.BTTV) {
            Data.globalBTTVEmotes = map;
            Log.d("BTTVData", "Global BTTV: " + map.keySet().toString());
        } else if (source == Emote.FFZ) {
            Data.globalFFZEmotes = map;
            Log.d("BTTVData", "Global FFZ: " + map.keySet().toString());
        }
    }

}
