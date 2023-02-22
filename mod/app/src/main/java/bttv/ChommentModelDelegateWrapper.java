package bttv;

import android.util.Log;

import androidx.core.util.Pair;

import java.util.List;

import bttv.highlight.Highlight;
import tv.twitch.android.models.chat.MessageToken;
import tv.twitch.android.models.chomments.ChommentModel;
import tv.twitch.android.shared.chat.pub.model.ChatMessageTokenizerWrapper;
import tv.twitch.android.provider.experiments.ExperimentHelper;
import tv.twitch.android.sdk.SDKServicesController;
import tv.twitch.android.shared.chat.ChatMessageParser;
import tv.twitch.android.shared.chat.chomments.ChommentModelDelegate;

public class ChommentModelDelegateWrapper extends ChommentModelDelegate {

    private Boolean BTTVshouldHighlightB = null;

    public ChommentModelDelegateWrapper(ChommentModel chommentModel,
                                        SDKServicesController sdkServicesController,
                                        ChatMessageParser chatMessageParser,
                                        ExperimentHelper experimentHelper,
                                        ChatMessageTokenizerWrapper chatMessageTokenizerWrapper) {
        super(
            chommentModel,
            sdkServicesController,
            chatMessageParser,
            experimentHelper,
            chatMessageTokenizerWrapper
        );
    }

    @Override
    public List<MessageToken> getTokens() {
        List<MessageToken> orig = super.getTokens();
        Pair<List<MessageToken>, Boolean> result = Tokenizer.tokenize(orig);
        this.BTTVshouldHighlightB = result.second;
        return result.first;
    }

    public boolean BTTVshouldHighlight() {
        if (this.BTTVshouldHighlightChannel()) {
            Log.i("LBTTVHighl", "BTTVshouldHighlightChannel() returned true");
            return true;
        }
        if (this.BTTVshouldHighlightB == null) {
            this.getTokens();
        }
        return this.BTTVshouldHighlightB;
    }

    private boolean BTTVshouldHighlightChannel() {
        if (this.getDisplayName() != null && Highlight.shouldHighlightChannel(this.getDisplayName())) {
            return true;
        } else {
            return this.getUserName() != null && Highlight.shouldHighlightChannel(this.getUserName());
        }
    }
}
