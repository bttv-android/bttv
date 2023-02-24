/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the patches.
 */
package tv.twitch.android.shared.chat.pub;

public interface ChatMessageInterface {
    java.util.List<tv.twitch.android.models.chat.MessageBadge> getBadges();

    String getDisplayName();

    java.util.List<tv.twitch.android.models.chat.MessageToken> getTokens();

    int getUserId();

    String getUserName();

    boolean isAction();

    boolean isDeleted();

    boolean isSystemMessage();
}
