package bttv;

import android.content.Context;
import android.util.Log;

public class EmoteUrlUtil {
    public static final String generateEmoteUrl(String id, float f) {
        Log.w("LBTTVEmoteUrlUtil", "generateEmoteUrl()", new Exception());
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

    public static final String getEmoteUrl(Context c, String id) {
        if (id.startsWith("BTTV-")) {
            return EmoteUrlUtil.generateEmoteUrl(id, 1.0f);
        } else {
            return tv.twitch.android.util.EmoteUrlUtil.getEmoteUrl(c, id);
        }
    }
}
