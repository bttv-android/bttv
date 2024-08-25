/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the patches.
 */
package tv.twitch.android.shared.chat;

import java.util.ArrayList;
import java.util.List;

import tv.twitch.android.models.chat.MessageBadge;
import tv.twitch.android.shared.chat.pub.model.messages.MessageToken;
import tv.twitch.android.shared.chat.pub.messages.ui.ChatMessageInterface;
import tv.twitch.chat.library.model.ChatMessageInfo;

public class ChatMessageDelegate implements ChatMessageInterface {
    public ChatMessageInfo chatMessage;

    public ChatMessageDelegate(ChatMessageInfo chatMessageInfo) {
    }

    @Override
    public List<MessageBadge> getBadges() {
        return new ArrayList<MessageBadge>();
    }

    @Override
    public List<MessageToken> getTokens() {
        return new ArrayList<MessageToken>();
    }

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public String getUserName() {
        return "";
    }

}
