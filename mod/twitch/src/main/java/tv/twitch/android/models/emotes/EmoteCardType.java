package tv.twitch.android.models.emotes;

import kotlin.jvm.internal.BTTVDefaultConstructorMarker;

public abstract class EmoteCardType {
    private EmoteCardType() {
    }

    public /* synthetic */ EmoteCardType(BTTVDefaultConstructorMarker defaultConstructorMarker) {
        this();
    }

    public static abstract class GenericEmoteCardType extends EmoteCardType {
        private GenericEmoteCardType() {
            super(null);
        }

        public /* synthetic */ GenericEmoteCardType(BTTVDefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
        public static class Global extends GenericEmoteCardType {
            public Global() {
                super(null);
            }
        }
    }
}
