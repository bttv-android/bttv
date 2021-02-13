package bttv;

import tv.twitch.chat.ChatMessageInfo;

import java.util.List;

import android.util.Log;
import tv.twitch.android.models.chat.MessageToken;
import tv.twitch.android.shared.chat.ChatMessageDelegate;

public class ChatMessageDeligateWrapper extends ChatMessageDelegate {
    public ChatMessageDeligateWrapper(ChatMessageInfo chatMessageInfo) {
        super(chatMessageInfo);
    }

    @Override
    public List<MessageToken> getTokens() {
        Log.w("BTTVChatMessageWrapper", "getTokens()");
        List<MessageToken> orig = super.getTokens();
        for (MessageToken token : orig) {
            Log.w("BTTVChatMessageWrapper", token.toString());
        }
        return orig;
    }
}
