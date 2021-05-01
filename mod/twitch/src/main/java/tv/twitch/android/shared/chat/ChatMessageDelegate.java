/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the patches.
 */
package tv.twitch.android.shared.chat;

import java.util.ArrayList;
import java.util.List;

import tv.twitch.android.models.chat.MessageBadge;
import tv.twitch.android.models.chat.MessageToken;
import tv.twitch.chat.ChatMessageInfo;

public class ChatMessageDelegate implements ChatMessageInterface {

    public ChatMessageDelegate(ChatMessageInfo chatMessageInfo) {
    }

    @Override
    public boolean isSystemMessage() {
        return false;
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
    public boolean isAction() {
        return false;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public String getUserName() {
        return "";
    }

    @Override
    public int getUserId() {
        return 0;
    }

    public int getTimestamp() {
        return 0;
    }

    public String getMessageId() {
        return "";
    }
}
