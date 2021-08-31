package tv.twitch.android.shared.chat.emotecard;

import tv.twitch.android.shared.chat.emotecard.EmoteCardUiModel;

public abstract class EmoteCardState {
    public static class Loaded {
        public EmoteCardUiModel getEmoteCardUiModel(){ throw new IllegalStateException(); }
    }
}
