package bttv;

public class EmoteUrlUtil {
    public static final String generateEmoteUrl(String id, float f) {
        if (id.startsWith("BTTV-")) {
            String realId = id.split("BTTV-")[1];
            return "https://cdn.betterttv.net/emote/" + realId + "/2x";
        } else {
            return tv.twitch.android.util.EmoteUrlUtil.generateEmoteUrl(id, f);
        }
    }
}
