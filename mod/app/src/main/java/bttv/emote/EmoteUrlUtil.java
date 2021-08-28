package bttv.emote;

import android.content.Context;
import android.util.Log;

import tv.twitch.android.shared.chat.emotecard.EmoteCardState;
import tv.twitch.android.shared.emotes.utils.AnimatedEmotesUrlUtil;

public class EmoteUrlUtil {
    private static final String TAG = "LBTTVEMoteurlUtil";

    public static String generateEmoteUrl(EmoteCardState.Loaded loaded) {
        String id = loaded.emoteCardModel.emoteId;
        String realId = extractBTTVId(id);
        if (realId != null) {
            return realIdToUrl(realId);
        } else {
            return loaded.emoteUrl;
        }
    }

    public static AnimatedEmotesUrlUtil.EmoteUrlDetails generateEmoteUrl(AnimatedEmotesUrlUtil util, Context context, String id, float f, AnimatedEmotesUrlUtil.EmoteUrlAnimationSetting emoteUrlAnimationSetting) {
        String realId = extractBTTVId(id);
        if (realId != null) {
            return wrapInDetails(realIdToUrl(realId));
        } else if (util != null) {
            return util.generateEmoteUrl(context, id, f, emoteUrlAnimationSetting);
        } else {
            return wrapInDetails(tv.twitch.android.util.EmoteUrlUtil.generateEmoteUrl(id, f));
        }
    }

    public static String getEmoteUrl(AnimatedEmotesUrlUtil util, Context c, String id) {
        if (id.startsWith("BTTV-")) {
            return EmoteUrlUtil.generateEmoteUrl(util, c, id, 1.0f, AnimatedEmotesUrlUtil.EmoteUrlAnimationSetting.DEFAULT).getEmoteUrl();
        } else {
            return util.getEmoteUrl(c, id);
        }
    }

    public static String getEmoteUrl(String id) {
        String realId = extractBTTVId(id);
        if (realId == null) {
            return null;
        }
        return realIdToUrl(realId);
    }

    private static String extractBTTVId(String id) {
        if (!id.startsWith("BTTV-")) {
            return null;
        }
        return id.split("BTTV-")[1];
    }

    private static String realIdToUrl(String realId) {
        Emote emote = Emotes.getEmoteById(realId);
        if (emote == null) {
            Log.w("LBTTVEmoteUrlUtil", "emote is null, fall back to bttv url, realId was " + realId);
            return "https://cdn.betterttv.net/emote/" + realId + "/1x"; // gamble
        }
        return emote.url;
    }

    private static AnimatedEmotesUrlUtil.EmoteUrlDetails wrapInDetails(String s) {
        return new AnimatedEmotesUrlUtil.EmoteUrlDetails(s, AnimatedEmotesUrlUtil.EmoteUrlAnimationSetting.DEFAULT);
    }
}
