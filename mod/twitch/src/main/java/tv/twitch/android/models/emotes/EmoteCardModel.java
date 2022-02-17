package tv.twitch.android.models.emotes;

public abstract class EmoteCardModel {
    public String emoteId;

    private EmoteCardModel(String emoteId, String emoteToken, EmoteModelAssetType emoteModelAssetType, EmoteCardType emoteCardType) {
    }

    public static final class GenericEmoteCardModel extends EmoteCardModel {
        public GenericEmoteCardModel(String emoteId, String emoteToken, EmoteModelAssetType assetType, EmoteCardType.GenericEmoteCardType emoteCardType) {
            super(emoteId, emoteToken, assetType, emoteCardType);
        }
    }
}
