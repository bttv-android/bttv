package bttv;

import android.util.Log;

import androidx.core.util.Pair;

import java.util.List;

import javax.inject.Provider;

import bttv.highlight.Highlight;
import tv.twitch.android.shared.chat.pub.model.messages.MessageToken;
import tv.twitch.android.models.chomments.ChommentModel;
import tv.twitch.android.shared.chat.pub.model.ChatMessageTokenizerWrapper;
import tv.twitch.android.shared.chat.ChatMessageParser;
import tv.twitch.android.shared.chat.chomments.ChommentModelDelegate;

public class ChommentModelDelegateWrapper extends ChommentModelDelegate {

    private Boolean BTTVshouldHighlightB = null;

    public ChommentModelDelegateWrapper(ChommentModel chommentModel,
                                        Provider<ChatMessageParser> chatMessageParserProvider,
                                        ChatMessageTokenizerWrapper chatMessageTokenizerWrapper) {
        super(
            chommentModel,
            chatMessageParserProvider,
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
