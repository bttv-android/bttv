/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the patches.
 */
package tv.twitch.android.shared.chat.chomments;

import java.util.List;

import javax.inject.Provider;

import tv.twitch.android.models.chat.MessageBadge;
import tv.twitch.android.shared.chat.pub.model.messages.MessageToken;
import tv.twitch.android.models.chomments.ChommentModel;
import tv.twitch.android.shared.chat.pub.model.ChatMessageTokenizerWrapper;
import tv.twitch.android.shared.chat.pub.messages.ui.ChatMessageInterface;

public class ChommentModelDelegate implements ChatMessageInterface {

    public ChommentModelDelegate(ChommentModel chommentModel,
                                 Provider<tv.twitch.android.shared.chat.ChatMessageParserSdk> chatMessageParserSdkProvider,
                                 Provider<tv.twitch.android.shared.chat.ChatMessageParser> chatMessageParserProvider,
                                 ChatMessageTokenizerWrapper chatMessageTokenizerWrapper,
                                 tv.twitch.android.shared.chat.KmpChatExperiment kpmChatExperiment) {
    }

    @Override
    public List<MessageBadge> getBadges() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public List<MessageToken> getTokens() {
        return null;
    }

    @Override
    public String getUserName() {
        return null;
    }

}
