/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the patches.
 */
package tv.twitch.android.shared.chat.pub.messages.ui;

import java.util.List;

import tv.twitch.android.shared.chat.pub.model.messages.MessageToken;

public interface ChatMessageInterface {
    List<tv.twitch.android.models.chat.MessageBadge> getBadges();

    String getDisplayName();

    List<MessageToken> getTokens();

    String getUserName();

}
