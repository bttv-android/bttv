package bttv.emote;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import bttv.Network;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;

public class Emotes {

    public enum Source {
        BTTV,
        FFZ,
        STV;
        public int getPriority() {
            if (this.equals(Source.BTTV)) {
                return 3;
            } else if (this.equals(Source.FFZ)) {
                return 2;
            } else if (this.equals(Source.STV)){
                return 1;
            } else {
                Log.w("LBTTVEmotesSource", "getPriority: unknown priority!");
                return 0;
            }
        }
    }

    public static Map<String, Emote> globalEmotesBTTV = new HashMap<>();
    public static Map<String, Emote> globalEmotesFFZ = new HashMap<>();
    public static Map<String, Emote> globalEmotes7TV = new HashMap<>();

    // map channel ids to their emote codes
    public static Map<Integer, Set<String>> channelEmotesBTTV = new HashMap<>();
    public static Map<Integer, Set<String>> channelEmotesFFZ = new HashMap<>();
    public static Map<Integer, Set<String>> channelEmotes7TV = new HashMap<>();

    // map emote to their code
    private static final Map<String, Map<Integer, Emote>> codeEmoteMap = new HashMap<>();

    // map emote to their id
    private static final Map<String, Emote> idEmoteMap = new HashMap<>();

    public static boolean channelHasEmotes(Context context, int channelId) {

        // BTTV
        boolean bttvEnabled = ResUtil.getBooleanFromSettings(Settings.BTTVEmotesEnabled);
        boolean hasBTTVEmotes = bttvEnabled && channelEmotesBTTV.containsKey(channelId);
        boolean globalBTTVLoaded = bttvEnabled && !globalEmotesBTTV.isEmpty();

        // FFZ
        boolean ffzEnabled = ResUtil.getBooleanFromSettings(Settings.FFZEmotesEnabled);
        boolean hasFFZEmotes = ffzEnabled && channelEmotesFFZ.containsKey(channelId);
        boolean globalFFZLoaded = ffzEnabled && !globalEmotes7TV.isEmpty();

        // 7TV
        boolean stvEnabled = ResUtil.getBooleanFromSettings(Settings.SevenTVEmotesEnabled);
        boolean has7TVEmotes = stvEnabled && channelEmotes7TV.containsKey(channelId);
        boolean global7TVLoaded = stvEnabled && !globalEmotes7TV.isEmpty();

        return hasBTTVEmotes
                || hasFFZEmotes
                || has7TVEmotes
                || globalBTTVLoaded
                || globalFFZLoaded
                || global7TVLoaded;
    }

    public static Emote getEmote(Context ctx, String code, int channelId) {
        boolean bttvEnabled = ResUtil.getBooleanFromSettings(Settings.BTTVEmotesEnabled);
        boolean gifEnabled = !ResUtil.selectedDropdownFromSettingsIs(Settings.GifRenderMode, Res.strings.bttv_settings_gif_render_mode_disabled);
        boolean ffzEnabled = ResUtil.getBooleanFromSettings(Settings.FFZEmotesEnabled);
        boolean stvEnabled = ResUtil.getBooleanFromSettings(Settings.SevenTVEmotesEnabled);

        ArrayList<Map<String, Emote>> globals = new ArrayList<>(Arrays.asList(globalEmotesFFZ, globalEmotesBTTV, globalEmotes7TV));
        ArrayList<Map<Integer, Set<String>>> channels = new ArrayList<>(Arrays.asList(channelEmotesFFZ, channelEmotesBTTV, channelEmotes7TV));
        ArrayList<Boolean> enabled = new ArrayList<>(Arrays.asList(ffzEnabled, bttvEnabled, stvEnabled));

        for (int i = 0; i < globals.size(); i++) {
            boolean isEnabled = enabled.get(i);
            if (!isEnabled) {
                continue;
            }

            Map<String, Emote> global = globals.get(i);

            Emote emote = global.get(code);
            if (emote != null && (!emote.imageType.equals("gif") || gifEnabled)) {
                return emote;
            }

            Map<Integer, Set<String>> channel = channels.get(i);
            Set<String> set = channel.get(channelId);
            if (set != null && set.contains(code)) {
                Map<Integer, Emote > channelEmoteMap = codeEmoteMap.get(code);
                if (channelEmoteMap != null) {
                    emote = channelEmoteMap.get(channelId);
                    if (emote != null && (!emote.imageType.equals("gif") || gifEnabled)) {
                        return emote;
                    }
                }
            }
        }

        return null;
    }

    public static Emote getEmote(int channelId, String code) {
        Map<Integer, Emote> channelEmoteMap = codeEmoteMap.get(code);
        if (channelEmoteMap == null)
            return null;
        Emote emote = channelEmoteMap.get(channelId);
        if (emote != null)
            return emote;
        return channelEmoteMap.get(0); // global
    }

    public static Emote getEmoteById(String id) {
        return idEmoteMap.get(id);
    }

    public static void ensureChannelEmotes(Context context, int id) {
        boolean bttvEnabled = ResUtil.getBooleanFromSettings(Settings.BTTVEmotesEnabled);
        boolean ffzEnabled = ResUtil.getBooleanFromSettings(Settings.FFZEmotesEnabled);
        boolean stvEnabled = ResUtil.getBooleanFromSettings(Settings.SevenTVEmotesEnabled);

        if (bttvEnabled && globalEmotesBTTV.isEmpty()) {
            Network.getBTTVGlobalEmotes();
        }
        if (ffzEnabled && globalEmotesFFZ.isEmpty()) {
            Network.getFFZGlobalEmotes();
        }
        if (stvEnabled && globalEmotes7TV.isEmpty()) {
            Network.get7TVGlobalEmotes();
        }
        if (bttvEnabled && !channelEmotesBTTV.containsKey(id)) {
            Network.getBTTVChannelEmotes(id);
        }
        if (ffzEnabled && !channelEmotesFFZ.containsKey(id)) {
            Network.getFFZChannelEmotes(id);
        }
        if (stvEnabled && !channelEmotes7TV.containsKey(id)) {
            Network.get7TVChannelEmotes(id);
        }
    }

    public static void setGlobal(List<Emote> emotes, Emotes.Source source) {
        HashMap<String, Emote> map = new HashMap<>();
        for (Emote emote : emotes) {
            map.put(emote.code, emote);
            addToCodeEmoteMap(0, emote);
            idEmoteMap.put(emote.id, emote);
        }
        switch (source) {
            case BTTV:
                globalEmotesBTTV = map;
                break;
            case FFZ:
                globalEmotesFFZ = map;
                break;
            case STV:
                globalEmotes7TV = map;
                break;
            default:
                Log.e("LBTTVEmotes", "setGlobal: source unknown!", new Exception());
        }
    }

    // FFZ
    public static void addChannelFFZ(int id, List<Emote> emotes) {
        Set<String> set = new HashSet<>();
        for (Emote emote : emotes) {
            set.add(emote.code);
            addToCodeEmoteMap(id, emote);
            idEmoteMap.put(emote.id, emote);
        }

        channelEmotesFFZ.put(id, set);
    }

    // BTTV
    public static void addChannelBTTV(int id, ChannelEmoteData chEmData) {
        Set<String> set = new HashSet<>();
        for (Emote emote : chEmData.channelEmotes) {
            set.add(emote.code);
            addToCodeEmoteMap(id, emote);
            idEmoteMap.put(emote.id, emote);
        }
        for (Emote emote : chEmData.sharedEmotes) {
            set.add(emote.code);
            addToCodeEmoteMap(id, emote);
            idEmoteMap.put(emote.id, emote);
        }

        channelEmotesBTTV.put(id, set);
    }

    // FFZ
    public static void addChannel7TV(int id, List<Emote> emotes) {
        Log.i("LBTTVDEBUG", "addChannel7TV: " + Arrays.toString(emotes.toArray()));

        Set<String> set = new HashSet<>();
        for (Emote emote : emotes) {
            set.add(emote.code);
            addToCodeEmoteMap(id, emote);
            idEmoteMap.put(emote.id, emote);
        }

        channelEmotes7TV.put(id, set);
        Log.i("LBTTVDEBUG", channelEmotes7TV.toString());
    }


    private static void addToCodeEmoteMap(int channelId, Emote emote) {
        String code = emote.code;
        Map<Integer, Emote> channelEmoteMap = codeEmoteMap.get(code); // get map channel -> Emote for code
        if (channelEmoteMap == null) { // if code is not in map (for no channel) create it
            Map<Integer, Emote> map = new TreeMap<>();
            map.put(channelId, emote);
            codeEmoteMap.put(code, map);
        } else { // if exists for any channel, check if it does for this
            Emote existing = channelEmoteMap.get(channelId);
            if (existing == null || existing.source.getPriority() <= emote.source.getPriority()) { // replace if more important
                channelEmoteMap.put(channelId, emote);
            }
        }
    }

    public static ArrayList<String> getAllEmotes(int channelId) {
        ArrayList<String> keys = new ArrayList<>();

        keys.addAll(globalEmotesFFZ.keySet());
        keys.addAll(globalEmotesBTTV.keySet());
        keys.addAll(globalEmotes7TV.keySet());

        Set<String> set;
        if ((set = channelEmotesFFZ.get(channelId)) != null)
            keys.addAll(set);
        if ((set = channelEmotesBTTV.get(channelId)) != null)
            keys.addAll(set);
        if ((set = channelEmotes7TV.get(channelId)) != null)
            keys.addAll(set);
        return keys;
    }

}
