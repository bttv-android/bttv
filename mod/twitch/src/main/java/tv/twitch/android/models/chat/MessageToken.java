/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the actual patch.
 */
package tv.twitch.android.models.chat;

interface Useless {
}

public class MessageToken {

    public MessageToken(Useless u) {
    }

    public static final class EmoticonToken extends MessageToken {
        private final String text;
        private final String id;
        public EmoticonToken(String text, String id) {
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
        private final String txt;
        public TextToken(String str, AutoModMessageFlags autoModMessageFlags) {
            super(null);
            this.txt = str;
        }

        public final String getText() {
            return txt;
        }

        public final AutoModMessageFlags getFlags() {
            return null;
        }

    }

}
