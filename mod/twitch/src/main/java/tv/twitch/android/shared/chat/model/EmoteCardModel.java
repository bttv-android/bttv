package tv.twitch.android.shared.chat.model;

import tv.twitch.android.shared.emotes.models.EmoteModelAssetType;

public abstract class EmoteCardModel {
    public String emoteId;

    private EmoteCardModel(String emoteId, String emoteToken, EmoteModelAssetType emoteModelAssetType) {
    }

    public static class GlobalEmoteCardModel extends EmoteCardModel {
        public GlobalEmoteCardModel(String emoteId, String emoteToken, EmoteModelAssetType emoteModelAssetType) {
            super(emoteId, emoteToken, emoteModelAssetType);
        }
    }
}
