package bttv;

import tv.twitch.chat.ChatMessageInfo;
import java.util.List;
import tv.twitch.android.models.chat.MessageToken;
import tv.twitch.android.shared.chat.ChatMessageDelegate;

public class ChatMessageDelegateWrapper extends ChatMessageDelegate {

    public ChatMessageDelegateWrapper(ChatMessageInfo chatMessageInfo) {
        super(chatMessageInfo);
    }

    @Override
    public List<MessageToken> getTokens() {
        List<MessageToken> orig = super.getTokens();
        return Tokenizer.tokenize(orig);
    }

}
