package tv.twitch.chat.library.model;

public abstract class MessageToken {
    public static final class TextToken extends MessageToken {
        public TextToken(String text, tv.twitch.chat.library.model.AutoModFlags autoModFlags) {
        }

        public String getText() {
            return "STUB";
        }

        public AutoModFlags getFlags() {
            return null;
        }
    }

    public static final class EmoteToken extends MessageToken {
        public EmoteToken(String text, java.lang.String id) {}

        public String getText() {
            return null;
        }

        public String getId() {
            return null;
        }
    }

    public static final class MentionToken extends MessageToken {
    }

    public static final class UrlToken extends tv.twitch.chat.library.model.MessageToken {}
}
