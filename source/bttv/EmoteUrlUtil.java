package bttv;

import android.util.Log;

public class EmoteUrlUtil {
    public static final String generateEmoteUrl(String id, float f) {
        if (id.startsWith("BTTV-")) {
            String realId = id.split("BTTV-")[1];
            Emote emote = Data.getEmoteById(realId);
            if (emote == null) {
                Log.w("LBTTVEmoteUrlUtil", "emote is null, fall back to bttv url, id was " + id);
                return "https://cdn.betterttv.net/emote/" + realId + "/1x"; // gamble
            }
            return emote.url;
        } else {
            return tv.twitch.android.util.EmoteUrlUtil.generateEmoteUrl(id, f);
        }
    }
}
