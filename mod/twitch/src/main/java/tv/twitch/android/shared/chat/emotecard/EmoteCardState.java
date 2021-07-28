package tv.twitch.android.shared.chat.emotecard;

import tv.twitch.android.shared.chat.model.EmoteCardModel;

public abstract class EmoteCardState {
    public static class Loaded {
        public String emoteUrl;
        public EmoteCardModel emoteCardModel;
    }
}
