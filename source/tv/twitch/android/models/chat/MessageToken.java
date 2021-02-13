/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the actual patch.
 */
package tv.twitch.android.models.chat;

interface Useless {}

public class MessageToken {

    public MessageToken(Useless u) {
    }

    public static final class EmoticonToken extends MessageToken {
        public EmoticonToken(String text, String id) {
            super(null);
        }
    }

    public static final class TextToken extends MessageToken {
        public TextToken(String str, AutoModMessageFlags autoModMessageFlags) {
            super(null);
        }
        public final String getText() {
            return "";
        }
    }

}
