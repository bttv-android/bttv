package bttv;

import java.util.List;

import kotlin.jvm.internal.DefaultConstructorMarker;
import tv.twitch.android.models.chat.MessageToken;
import tv.twitch.android.models.chomments.ChommentModel;
import tv.twitch.android.sdk.SDKServicesController;
import tv.twitch.android.shared.chat.ChatMessageParser;
import tv.twitch.android.shared.chat.chomments.ChommentModelDelegate;

public class ChommentModelDelegateWrapper extends ChommentModelDelegate {

    public ChommentModelDelegateWrapper(ChommentModel chommentModel,
                                        SDKServicesController sdkServicesController,
                                        ChatMessageParser chatMessageParser,
                                        int i,
                                        DefaultConstructorMarker defaultConstructorMarker) {
        super(chommentModel, sdkServicesController, chatMessageParser, i, defaultConstructorMarker);
    }

    @Override
    public List<MessageToken> getTokens() {
        List<MessageToken> orig = super.getTokens();
        return Tokenizer.tokenize(orig);
    }
}
