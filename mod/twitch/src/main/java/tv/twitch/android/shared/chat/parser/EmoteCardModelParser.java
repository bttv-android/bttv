package tv.twitch.android.shared.chat.parser;

import tv.twitch.android.shared.chat.model.EmoteCardModel;

public class EmoteCardModelParser {
    public static abstract class EmoteCardModelResponse {

        public static final class Success extends EmoteCardModelParser.EmoteCardModelResponse {
            public Success(EmoteCardModel emoteCardModel) {
                super(null);
            }

            public final EmoteCardModel getEmoteCardModel() {
                return null;
            }
        }

        private EmoteCardModelResponse() {
        }

        public EmoteCardModelResponse(kotlin.jvm.internal.DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

    }

}
