package bttv;

import androidx.core.util.Pair;

import java.util.List;

import kotlin.jvm.internal.BTTVDefaultConstructorMarker;
import tv.twitch.android.models.chat.MessageToken;
import tv.twitch.android.models.chomments.ChommentModel;
import tv.twitch.android.sdk.SDKServicesController;
import tv.twitch.android.shared.chat.ChatMessageParser;
import tv.twitch.android.shared.chat.chomments.ChommentModelDelegate;

public class ChommentModelDelegateWrapper extends ChommentModelDelegate {

    private Boolean BTTVshouldHighlightB = null;

    public ChommentModelDelegateWrapper(ChommentModel chommentModel,
                                        SDKServicesController sdkServicesController,
                                        ChatMessageParser chatMessageParser,
                                        int i,
                                        BTTVDefaultConstructorMarker BTTVDefaultConstructorMarker) {
        super(chommentModel, sdkServicesController, chatMessageParser, i, BTTVDefaultConstructorMarker);
    }

    @Override
    public List<MessageToken> getTokens() {
        List<MessageToken> orig = super.getTokens();
        Pair<List<MessageToken>, Boolean> result = Tokenizer.tokenize(orig);
        this.BTTVshouldHighlightB = result.second;
        return result.first;
    }

    public boolean BTTVshouldHighlight() {
        if (this.BTTVshouldHighlightB == null) {
            this.getTokens();
        }
        return this.BTTVshouldHighlightB;
    }
}
