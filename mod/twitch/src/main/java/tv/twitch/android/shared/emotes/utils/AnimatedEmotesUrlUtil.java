package tv.twitch.android.shared.emotes.utils;

import android.content.Context;

public class AnimatedEmotesUrlUtil {

    public final EmoteUrlDetails generateEmoteUrl(Context context, String emoticonId, float f, EmoteUrlAnimationSetting emoteUrlAnimationSetting) {
        throw new IllegalStateException("AnimatedEmotesUrlUtil.generateEmoteUrl() called");
    }

    public final String getEmoteUrl(Context context, String emoticonId) {
        throw new IllegalStateException("AnimatedEmotesUrlUtil.getEmoteUrl() called");
    }

    public enum EmoteUrlAnimationSetting {
        DEFAULT("default"),
        STATIC("static");
        EmoteUrlAnimationSetting(String s){};
    }

    public static class EmoteUrlDetails {
        public EmoteUrlDetails(String emoteUrl, EmoteUrlAnimationSetting emoteUrlAnimationSetting) {}
        public final String getEmoteUrl() {
            return "";
        }
    }
}
