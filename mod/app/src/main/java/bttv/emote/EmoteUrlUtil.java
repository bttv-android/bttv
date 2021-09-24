package bttv.emote;

import android.content.Context;
import android.util.Log;

import tv.twitch.android.shared.emotes.utils.AnimatedEmotesUrlUtil;

public class EmoteUrlUtil {
    private static final String TAG = "LBTTVEmoteurlUtil";

    public static String getEmoteUrl(String id) {
        String realId = extractBTTVId(id);
        if (realId == null) {
            return null;
        }
        return realIdToUrl(realId);
    }

    public static String extractBTTVId(String id) {
        if (!id.startsWith("BTTV-")) {
            return null;
        }
        return id.split("BTTV-")[1];
    }

    public static String realIdToUrl(String realId) {
        Emote emote = Emotes.getEmoteById(realId);
        if (emote == null) {
            Log.w("LBTTVEmoteUrlUtil", "emote is null, fall back to bttv url, realId was " + realId);
            return "https://cdn.betterttv.net/emote/" + realId + "/1x"; // gamble
        }
        return emote.url;
    }

}
