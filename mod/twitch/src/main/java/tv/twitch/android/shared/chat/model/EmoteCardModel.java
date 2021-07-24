package tv.twitch.android.shared.chat.model;

public abstract class EmoteCardModel {
    public String emoteId;

    private EmoteCardModel(String emoteId, String emoteToken) {
    }

    public static class GlobalEmoteCardModel extends EmoteCardModel {
        public GlobalEmoteCardModel(String emoteId, String emoteToken) {
            super(emoteId, emoteToken);
        }
    }
}
