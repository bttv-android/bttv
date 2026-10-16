package bttv.emote;

import android.util.Log;

import tv.twitch.android.shared.emotes.utils.AnimatedEmotesUrlUtil;

public class EmoteUrlUtil {
    private static final String TAG = "LBTTVEmoteurlUtil";

    public static String getEmoteUrl(String id, AnimatedEmotesUrlUtil.EmoteUrlAnimationSetting setting) {
        String url = getEmoteUrl(id);
        if (url == null) {
            return null;
        }
        if (setting == AnimatedEmotesUrlUtil.EmoteUrlAnimationSetting.STATIC) {
            return url + "?static=true";
        }
        return url;
    }

    public static String getEmoteUrl(String id) {
        String realId = extractBTTVId(id);
        if (realId == null) {
            return null;
        }
        return realIdToUrl(realId);
    }

    private static final String TWITCH_EMOTE_URL_MARKER = "/emoticons/v2/";

    /** @return the emote id in a Twitch emote url (".../emoticons/v2/<id>/..."), null if it is none */
    public static String idFromTwitchUrl(String uri) {
        if (uri == null) {
            return null;
        }
        int start = uri.indexOf(TWITCH_EMOTE_URL_MARKER);
        if (start == -1) {
            return null;
        }
        start += TWITCH_EMOTE_URL_MARKER.length();
        int end = uri.indexOf('/', start);
        return end == -1 ? uri.substring(start) : uri.substring(start, end);
    }

    // id of an emote in a message that got a wider slot, see WideEmotes
    public static final String WIDE_PREFIX = "BTTV-wide-";

    public static String extractBTTVId(String id) {
        if (id.startsWith(WIDE_PREFIX)) {
            return id.substring(WIDE_PREFIX.length());
        }
        if (!id.startsWith("BTTV-")) {
            return null;
        }
        return id.split("BTTV-")[1];
    }

    public static String realIdToUrl(String realId) {
        Emote emote = Emotes.getEmoteById(realId);
        if (emote == null) {
            Log.w("LBTTVEmoteUrlUtil", "emote is null, fall back to bttv url, realId was " + realId);
            return "https://cdn.betterttv.net/emote/" + realId + "/2x"; // gamble
        }
        return emote.url;
    }

}
