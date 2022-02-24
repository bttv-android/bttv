package tv.twitch.chat;

public class ChatUrlToken extends ChatMessageToken {
    public ChatUrlToken() {
        this.type = ChatMessageTokenType.Url;
    }
}
