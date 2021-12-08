package tv.twitch.android.models.emotes;

import kotlin.jvm.internal.BTTVDefaultConstructorMarker;

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
