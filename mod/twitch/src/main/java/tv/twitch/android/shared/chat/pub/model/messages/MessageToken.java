/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the actual patch.
 */
package tv.twitch.android.shared.chat.pub.model.messages;
import kotlin.jvm.internal.BTTVDefaultConstructorMarker;
import tv.twitch.android.shared.chat.pub.model.messages.AutoModMessageFlags;


public abstract class MessageToken {

    public MessageToken(BTTVDefaultConstructorMarker u) {
    }

    public static final class EmoticonToken extends MessageToken {
        private final String text;
        private final String id;
        public EmoticonToken(String text, String id) {
            super(null);
            this.text = text;
            this.id = id;
        }

    }

    public static final class TextToken extends MessageToken {
        private final String text;
        private final AutoModMessageFlags flags;
        public TextToken(String str, AutoModMessageFlags flags) {
            super(null);
            this.text = str;
            this.flags = flags;
        }

        public final String getText() {
            return text;
        }

        public final AutoModMessageFlags getFlags() {
            return flags;
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
