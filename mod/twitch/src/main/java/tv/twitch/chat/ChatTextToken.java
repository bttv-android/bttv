package tv.twitch.chat;

public class ChatTextToken extends ChatMessageToken {
    public AutoModFlags autoModFlags;
    public String text = null;

    public ChatTextToken() {
        this.type = ChatMessageTokenType.Text;
        this.autoModFlags = new AutoModFlags();
    }
}
