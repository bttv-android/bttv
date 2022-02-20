package tv.twitch.chat;

public class ChatMentionToken extends ChatMessageToken {
    public ChatMentionToken() {
        this.type = ChatMessageTokenType.Mention;
    }
}
