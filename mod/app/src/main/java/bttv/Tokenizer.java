package bttv;

import android.content.Context;
import android.util.Log;

import androidx.core.util.Pair;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import bttv.emote.Emote;
import bttv.emote.Emotes;
import bttv.highlight.Blacklist;
import bttv.highlight.Highlight;
import tv.twitch.android.shared.chat.pub.model.messages.AutoModMessageFlags;
import tv.twitch.android.shared.chat.pub.model.messages.MessageToken;
import tv.twitch.android.shared.chat.pub.model.messages.MessageToken.TextToken;
import tv.twitch.android.shared.chat.pub.model.messages.MessageToken.EmoticonToken;
import tv.twitch.android.shared.chat.pub.messages.ui.ChatMessageInterface;
import tv.twitch.android.shared.chat.ChatMessageDelegate;
import tv.twitch.chat.library.model.ChatMessageInfo;

public class Tokenizer {

    public static Pair<List<MessageToken>, Boolean> tokenize(List<MessageToken> orig) {
        Context ctx = Data.ctx;
        int channel = Data.currentBroadcasterId;

        // Don't add Emotes, when we don't have the channel's emotes (yet)
        // and we don't need to Highlight any messages
        if (!Emotes.channelHasEmotes(ctx, channel) && Highlight.getInstance().isEmpty()) {
            return new Pair<>(orig, false);
        }

        ArrayList<MessageToken> newTokens = new ArrayList<>(orig.size() + 5);
        boolean shouldHighlight = false;

        for (MessageToken token : orig) {
            // possible issue: emotes won't work in e.g. MentionToken or BitsToken
            if (!(token instanceof TextToken)) {
                if (token instanceof EmoticonToken && !newTokens.isEmpty()) {
                    if (newTokens.get(newTokens.size() - 1) instanceof EmoticonToken) {
                        newTokens.add(new TextToken(" ", new AutoModMessageFlags()));
                    }
                }
                newTokens.add(token);
                continue;
            }

            TextToken text = (TextToken) token;

            if (text.getText().equals(" ")) {
                // " ".split(" ") will produce an empty array
                // this is why we need to handle this edge-case
                newTokens.add(token);
                continue;
            }
            String[] tokens = text.getText().split(" ");

            StringBuilder currentText = new StringBuilder();
            for (String word : tokens) {
                Emote emote = Emotes.getEmote(ctx, word, channel);
                if (Highlight.shouldHighlight(word)) {
                    shouldHighlight = true;
                }
                if (emote == null) {
                    currentText.append(word).append(" ");
                    continue;
                }
                // emote found
                String before = currentText.toString();
                if (!before.isEmpty()) {
                    newTokens.add(new TextToken(currentText.toString(), text.getFlags())); // add everything before Emote as TextToken
                }
                newTokens.add(new EmoticonToken(word, "BTTV-" + emote.id)); // add Emote

                // prepare next TextToken
                currentText.setLength(0);
                currentText.append(' ');
            }
            String before = currentText.toString();
            if (!before.trim().isEmpty()) {
                newTokens.add(new TextToken(before, text.getFlags()));
            }
        }

        return new Pair<>(newTokens, shouldHighlight);
    }

    /** @noinspection unused */
    public static void retokenizeLiveChatMessage(ChatMessageInterface chatMessageInterface) {
        if (chatMessageInterface instanceof ChatMessageDelegate) {
            ChatMessageDelegate delegate = (ChatMessageDelegate) chatMessageInterface;
            retokenizeLiveChatMessage(delegate.chatMessage);
        } else {
            Log.w(
                "LBTTV",
                "retokenizeLiveChatMessage: interface it not a ChatMessageDelegate it's a "
                + chatMessageInterface.getClass().getName());
        }
    }

    public static void retokenizeLiveChatMessage(ChatMessageInfo info) {
        Context ctx = Data.ctx;
        int channel = Data.currentBroadcasterId;

        ArrayList<tv.twitch.chat.library.model.MessageToken> newTokens = new ArrayList<>(info.tokens.size() + 10);

        boolean shouldHighlight = false;
        boolean shouldBlock = false;

        for (tv.twitch.chat.library.model.MessageToken token : info.tokens) {
            Log.d("LBTTV", "retokenizeLiveChatMessage: " + token);
            if (!isTextToken(token)) {
                if (isEmoteToken(token) && !newTokens.isEmpty()) {
                    tv.twitch.chat.library.model.MessageToken prevToken = newTokens.get(newTokens.size() - 1);
                    if (prevToken != null && !endsWithSpace(prevToken)) {
                        tv.twitch.chat.library.model.MessageToken.TextToken spaceToken = new tv.twitch.chat.library.model.MessageToken.TextToken(
                                " ",
                                new tv.twitch.chat.library.model.AutoModFlags()
                        );
                        newTokens.add(spaceToken);
                    }
                }
                newTokens.add(token);
                continue;
            }

            tv.twitch.chat.library.model.MessageToken.TextToken textToken = (tv.twitch.chat.library.model.MessageToken.TextToken) token;
            String text = textToken.getText();

            if (text.equals(" ")) {
                // " ".split(" ") will produce an empty array
                // this is why we need to handle this edge-case
                newTokens.add(textToken);
                continue;
            }
            String[] tokens = text.split(" ");

            StringBuilder currentText = new StringBuilder();
            for(String word : tokens) {
                if (Blacklist.shouldBlock(word)) {
                    shouldBlock = true;
                }
                if (Highlight.shouldHighlight(word)) {
                    shouldHighlight = true;
                }
                Emote emote = Emotes.getEmote(ctx, word, channel);
                if (emote == null) {
                    currentText.append(word).append(" ");
                    continue;
                }
                // emote found
                String before = currentText.toString();
                if (!before.isEmpty()) {
                    tv.twitch.chat.library.model.MessageToken.TextToken everythingBeforeEmote = new tv.twitch.chat.library.model.MessageToken.TextToken(before, textToken.getFlags());
                    newTokens.add(everythingBeforeEmote);
                }
                tv.twitch.chat.library.model.MessageToken.EmoteToken emoteToken = new tv.twitch.chat.library.model.MessageToken.EmoteToken(word, "BTTV-" + emote.id);
                newTokens.add(emoteToken);

                // prepare next TextToken
                currentText.setLength(0);
                currentText.append(' ');
            }
            String before = currentText.toString();
            if (!before.trim().isEmpty()) {
                tv.twitch.chat.library.model.MessageToken.TextToken everything = new tv.twitch.chat.library.model.MessageToken.TextToken(
                        before, textToken.getFlags()
                );
                newTokens.add(everything);
            }
        }

        info.tokens = newTokens;

        if (shouldBlock) {
            info.messageType = "bttv-blocked-message";
        } else if (shouldHighlight) {
            info.messageType = "bttv-highlighted-message";
        }
    }

    private static boolean endsWithSpace(@NotNull tv.twitch.chat.library.model.MessageToken token) {
        if (!(token instanceof tv.twitch.chat.library.model.MessageToken.TextToken)) {
            return false;
        }
        tv.twitch.chat.library.model.MessageToken.TextToken textToken = (tv.twitch.chat.library.model.MessageToken.TextToken) token;
        String text = textToken.getText();
        if (text.isEmpty()) {
            return false;
        }
        char lastChar = text.charAt(text.length() - 1);
        return lastChar == ' ';
    }

    private static boolean isTextToken(tv.twitch.chat.library.model.MessageToken token) {
        if (token == null) {
            return false;
        }
        return token instanceof tv.twitch.chat.library.model.MessageToken.TextToken;
    }

    private static boolean isEmoteToken(tv.twitch.chat.library.model.MessageToken token) {
        if (token == null) {
            return false;
        }
        return token instanceof tv.twitch.chat.library.model.MessageToken.EmoteToken;
    }
}
