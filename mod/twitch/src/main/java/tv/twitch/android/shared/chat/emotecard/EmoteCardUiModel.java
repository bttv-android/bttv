package tv.twitch.android.shared.chat.emotecard;

import tv.twitch.android.models.emotes.EmoteCardModel;

public abstract class EmoteCardUiModel {
    public abstract String getEmoteUrl();
    public abstract EmoteCardModel getEmoteCardModel();
}
