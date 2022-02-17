package tv.twitch.android.core.strings;

public abstract class StringResource {
    public static StringResource.Companion Companion;

    public static final class Companion {
        public final StringResource fromString(String str) {
            throw new IllegalStateException("stub called");
        }
    }
}
