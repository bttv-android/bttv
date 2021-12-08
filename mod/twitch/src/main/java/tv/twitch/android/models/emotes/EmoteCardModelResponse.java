package tv.twitch.android.provider.chat.model;

import kotlin.jvm.internal.BTTVDefaultConstructorMarker;
import tv.twitch.android.models.emotes.EmoteCardModel;

public abstract class EmoteCardModelResponse {

  public static final class Success extends EmoteCardModelResponse {
    public Success(EmoteCardModel emoteCardModel) {
      super(null);
    }

    public final EmoteCardModel getEmoteCardModel() {
      return null;
    }
  }

  private EmoteCardModelResponse() {
  }

  public EmoteCardModelResponse(BTTVDefaultConstructorMarker BTTVDefaultConstructorMarker) {
    this();
  }

}
