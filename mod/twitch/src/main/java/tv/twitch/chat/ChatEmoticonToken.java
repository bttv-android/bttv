package tv.twitch.chat;

public class ChatEmoticonToken extends ChatMessageToken {
    public String emoticonId;
    public String emoticonText;

    public ChatEmoticonToken() {
        this.type = ChatMessageTokenType.Emoticon;
    }
}
