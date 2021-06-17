package bttv.emote;

import android.content.Context;
import android.util.Log;

import tv.twitch.android.shared.emotes.utils.AnimatedEmotesUrlUtil;

public class EmoteUrlUtil {

    public static String generateEmoteUrl(String id, float f) {
        return generateEmoteUrl(null, null, id, f);
    }

    public static String generateEmoteUrl(AnimatedEmotesUrlUtil util, Context context, String id, float f) {
        if (id.startsWith("BTTV-")) {
            String realId = id.split("BTTV-")[1];
            Emote emote = Emotes.getEmoteById(realId);
            if (emote == null) {
                Log.w("LBTTVEmoteUrlUtil", "emote is null, fall back to bttv url, id was " + id);
                return "https://cdn.betterttv.net/emote/" + realId + "/1x"; // gamble
            }
            return emote.url;
        } else if (util != null) {
            return util.generateEmoteUrl(context, id, f);
        } else {
            return tv.twitch.android.util.EmoteUrlUtil.generateEmoteUrl(id, f);
        }
    }

    public static String getEmoteUrl(AnimatedEmotesUrlUtil util, Context c, String id) {
        if (id.startsWith("BTTV-")) {
            return EmoteUrlUtil.generateEmoteUrl(util, c, id, 1.0f);
        } else {
            return util.getEmoteUrl(c, id);
        }
    }
}
