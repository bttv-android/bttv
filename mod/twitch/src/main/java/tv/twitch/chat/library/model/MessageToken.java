/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the actual patch.
 */
package tv.twitch.chat.library.model;
import kotlin.jvm.internal.BTTVDefaultConstructorMarker;
import tv.twitch.android.shared.chat.pub.model.messages.AutoModMessageFlags;


public abstract class MessageToken {

    public MessageToken(BTTVDefaultConstructorMarker u) {
    }

    public static final class EmoteToken extends MessageToken {
        private final String text;
        private final String id;
        public EmoteToken(String text, String id) {
            super(null);
            this.text = text;
            this.id = id;
        }

        public final String component1() {
            return this.text;
        }
        public final String component2() {
            return this.id;
        }
    }

    public static final class TextToken extends MessageToken {
        private final String text;
        public TextToken(String str, AutoModMessageFlags autoModMessageFlags) {
            super(null);
            this.text = str;
        }

        public final String getText() {
            return text;
        }

        public final AutoModMessageFlags getFlags() {
            return null;
        }

    }

    public static final class MentionToken extends MessageToken {
        public MentionToken(String text, String username, boolean isLocaluser) {
            super(null);
        }
    }

    public static final class UrlToken extends MessageToken {
        public UrlToken(String url, boolean hidden) {
            super(null);
        }
    }
}
