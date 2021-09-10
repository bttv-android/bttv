package tv.twitch.android.shared.chat.emotecard;

import tv.twitch.android.shared.chat.model.EmoteCardModel;

public abstract class EmoteCardUiModel {
    public abstract String getEmoteUrl();
    public abstract EmoteCardModel getEmoteCardModel();
}
