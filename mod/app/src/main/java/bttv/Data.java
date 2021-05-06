package bttv;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tv.twitch.android.models.channel.ChannelModel;

public class Data {
    public static String getBttvVersion() {
        if (ctx == null) {
            Log.e("LBTTVData", "getVersion called, but ctx is null", new Exception());
            return null;
        }
        String vN = null;
        try {
            vN = "v" +ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return vN;
    }

    public static int currentBroadcasterId = -1;
    public static Context ctx;

    public static ConcurrentHashMap<Integer, Set<String>> availBTTVEmoteSetMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, Set<String>> availFFZEmoteSetMap = new ConcurrentHashMap<>();

    // map code to emote
    private static ConcurrentHashMap<String, Emote> codeEmoteMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Emote> idEmoteMap = new ConcurrentHashMap<>();
    public static HashMap<String, Emote> globalBTTVEmotes = new HashMap<>();
    public static HashMap<String, Emote> globalFFZEmotes = new HashMap<>();

    public static void setCurrentBroadcasterId(int id) {
        Log.i("LBTTVSetBroadcasterId", currentBroadcasterId + " -> " + id);
        Data.currentBroadcasterId = id;
        ensureChannelEmotes(id);
    }

    public static void setCurrentBroadcasterId(ChannelModel channel) {
        setCurrentBroadcasterId(channel.component1());
    }

    public static void setContext(Context ctx) {
        Data.ctx = ctx;
        Log.i("LBTTVDataSetContext", "context now is " + ((Data.ctx == null) ? "null" : "not null"));
    }

    public static boolean channelHasEmotes(int id) {
        boolean bttvEnabled = UserPreferences.getBTTVEmotesEnabled(ctx);
        boolean ffzEnabled = UserPreferences.getFFZEmotesEnabled(ctx);

        boolean hasBTTVEmotes = bttvEnabled && availBTTVEmoteSetMap.containsKey(id);
        boolean hasFFZEmotes = ffzEnabled && availFFZEmoteSetMap.containsKey(id);
        boolean globalBTTVLoaded = bttvEnabled && !globalBTTVEmotes.isEmpty();
        boolean globalFFZLoaded = ffzEnabled && !globalFFZEmotes.isEmpty();
        return hasBTTVEmotes || hasFFZEmotes || globalBTTVLoaded || globalFFZLoaded;
    }

    public static Emote getEmote(String code, int channelId) {
        boolean bttvEnabled = UserPreferences.getBTTVEmotesEnabled(ctx);
        boolean bttvGifEnabled = UserPreferences.getBTTVGifEmotesEnabled(ctx);
        boolean ffzEnabled = UserPreferences.getFFZEmotesEnabled(ctx);

        if (ffzEnabled) {
            Emote emote = globalFFZEmotes.get(code);
            if (emote != null) {
                return emote;
            }
            Set<String> ffzSet = availFFZEmoteSetMap.get(channelId);
            if (ffzSet != null && ffzSet.contains(code)) {
                return getEmote(code);
            }
        }

        if (bttvEnabled) {
            Emote emote = globalBTTVEmotes.get(code);
            if (emote != null) {
                if (!emote.imageType.equals("gif") || bttvGifEnabled) {
                    return emote;
                }
            }
            Set<String> bttvSet = availBTTVEmoteSetMap.get(channelId);
            if (bttvSet != null && bttvSet.contains(code)) {
                emote = codeEmoteMap.get(code);
                if (emote != null && (!emote.imageType.equals("gif") || bttvGifEnabled)) {
                    return emote;
                }
            }
        }

        return null;
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
        } else if (source == Emote.FFZ) {
            Data.globalFFZEmotes = map;
        }
    }

}
