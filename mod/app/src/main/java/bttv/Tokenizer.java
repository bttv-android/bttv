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
import tv.twitch.chat.library.model.MessageToken.TextToken;
import tv.twitch.chat.library.model.MessageToken;
import tv.twitch.chat.library.model.MessageToken.EmoteToken;
import tv.twitch.android.shared.chat.pub.ChatMessageInterface;
import tv.twitch.android.shared.chat.ChatMessageDelegate;
import tv.twitch.chat.AutoModFlags;
import tv.twitch.chat.ChatEmoticonToken;
import tv.twitch.chat.ChatMessageInfo;
import tv.twitch.chat.ChatMessageToken;
import tv.twitch.chat.ChatMessageTokenType;
import tv.twitch.chat.ChatTextToken;

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
                if (token instanceof EmoteToken && newTokens.size() > 0) {
                    if (newTokens.get(newTokens.size() - 1) instanceof EmoteToken) {
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
                newTokens.add(new EmoteToken(word, "BTTV-" + emote.id)); // add Emote

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

    public static void retokenizeLiveChatMessage(ChatMessageInterface chatMessageInterface) {
        if (chatMessageInterface instanceof ChatMessageDelegate) {
            ChatMessageDelegate delegate = (ChatMessageDelegate) chatMessageInterface;
            retokenizeLiveChatMessage(delegate.mChatMessage);
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

        ArrayList<ChatMessageToken> newTokens = new ArrayList<>(info.tokens.length + 10);

        boolean shouldHighlight = false;
        boolean shouldBlock = false;

        for (ChatMessageToken token : info.tokens) {
            Log.d("LBTTV", "retokenizeLiveChatMessage: " + token);
            if (!isTextToken(token)) {
                if (isEmoteToken(token) && !newTokens.isEmpty()) {
                    ChatMessageToken prevToken = newTokens.get(newTokens.size() - 1);
                    if (prevToken != null && !endsWithSpace(prevToken)) {
                        ChatTextToken spaceToken = new ChatTextToken();
                        spaceToken.text = " ";
                        spaceToken.autoModFlags = new AutoModFlags();
                        newTokens.add(spaceToken);
                    }
                }
                newTokens.add(token);
                continue;
            }

            ChatTextToken textToken = (ChatTextToken) token;
            String text = textToken.text;

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
                    ChatTextToken everythingBeforeEmote = new ChatTextToken();
                    everythingBeforeEmote.text = before;
                    everythingBeforeEmote.autoModFlags = textToken.autoModFlags;
                    newTokens.add(everythingBeforeEmote);
                }
                ChatEmoticonToken emoteToken = new ChatEmoticonToken();
                emoteToken.emoticonId = "BTTV-" + emote.id;
                emoteToken.emoticonText = word;
                newTokens.add(emoteToken);

                // prepare next TextToken
                currentText.setLength(0);
                currentText.append(' ');
            }
            String before = currentText.toString();
            if (!before.trim().isEmpty()) {
                ChatTextToken everything = new ChatTextToken();
                everything.text = before;
                everything.autoModFlags = textToken.autoModFlags;
                newTokens.add(everything);
            }
        }

        info.tokens = newTokens.toArray(new ChatMessageToken[0]);

        if (shouldBlock) {
            info.messageType = "bttv-blocked-message";
        } else if (shouldHighlight) {
            info.messageType = "bttv-highlighted-message";
        }
    }

    private static boolean endsWithSpace(@NotNull ChatMessageToken token) {
        if (token.type.getValue() != ChatMessageTokenType.Text.getValue()) {
            return false;
        }
        ChatTextToken textToken = (ChatTextToken) token;
        String text = textToken.text;
        if (text.length() == 0) {
            return false;
        }
        char lastChar = text.charAt(text.length() - 1);
        return lastChar == ' ';
    }

    private static boolean isTextToken(ChatMessageToken token) {
        if (token == null) {
            return false;
        }
        return token.type.getValue() == ChatMessageTokenType.Text.getValue();
    }

    private static boolean isEmoteToken(ChatMessageToken token) {
        if (token == null) {
            return false;
        }
        return token.type.getValue() == ChatMessageTokenType.Emoticon.getValue();
    }
}
