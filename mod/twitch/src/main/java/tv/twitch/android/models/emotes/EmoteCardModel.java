package tv.twitch.android.models.emotes;

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
