/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the patches.
 */
package tv.twitch.android.shared.chat.chomments;

import java.util.List;

import tv.twitch.android.models.chat.MessageBadge;
import tv.twitch.android.models.chat.MessageToken;
import tv.twitch.android.models.chomments.ChommentModel;
import tv.twitch.android.shared.chat.pub.model.ChatMessageTokenizerWrapper;
import tv.twitch.android.provider.experiments.ExperimentHelper;
import tv.twitch.android.sdk.SDKServicesController;
import tv.twitch.android.shared.chat.pub.ChatMessageInterface;
import tv.twitch.android.shared.chat.ChatMessageParser;

public class ChommentModelDelegate implements ChatMessageInterface {

    public ChommentModelDelegate(ChommentModel chommentModel,
                                 SDKServicesController sdkServicesController,
                                 ChatMessageParser chatMessageParser,
                                 ExperimentHelper experimentHelper,
                                 ChatMessageTokenizerWrapper chatMessageTokenizerWrapper) {
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
    public int getUserId() {
        return 0;
    }

    @Override
    public String getUserName() {
        return null;
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
    public boolean isSystemMessage() {
        return false;
    }

}
