package bttv;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import bttv.emote.Emote;
import bttv.emote.Emotes;
import bttv.highlight.Highlight;
import tv.twitch.android.models.chat.AutoModMessageFlags;
import tv.twitch.android.models.chat.MessageToken.TextToken;
import tv.twitch.android.models.chat.MessageToken;
import tv.twitch.android.models.chat.MessageToken.EmoticonToken;
import tv.twitch.chat.AutoModFlags;
import tv.twitch.chat.ChatEmoticonToken;
import tv.twitch.chat.ChatMessageInfo;
import tv.twitch.chat.ChatMessageToken;
import tv.twitch.chat.ChatMessageTokenType;
import tv.twitch.chat.ChatTextToken;

public class Tokenizer {

    public static List<MessageToken> tokenize(List<MessageToken> orig) {
        Context ctx = Data.ctx;
        int channel = Data.currentBroadcasterId;

        // Don't add Emotes, when we don't have the channel's emotes (yet)
        if (!Emotes.channelHasEmotes(ctx, channel)) {
            return orig;
        }

        ArrayList<MessageToken> newTokens = new ArrayList<>(orig.size() + 5);

        for (MessageToken token : orig) {
            // possible issue: emotes won't work in e.g. MentionToken or BitsToken
            if (!(token instanceof TextToken)) {
                if (token instanceof EmoticonToken && newTokens.size() > 0) {
                    if (newTokens.get(newTokens.size() - 1) instanceof EmoticonToken) {
                        newTokens.add(new TextToken(" ", new AutoModMessageFlags()));
                    }
                }
                newTokens.add(token);
                continue;
            }

            TextToken text = (TextToken) token;
            String[] tokens = text.getText().split(" ");

            StringBuilder currentText = new StringBuilder();
            for (String word : tokens) {
                Emote emote = Emotes.getEmote(ctx, word, channel);
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

        return newTokens;
    }

    public static void retokenizeLiveChatMessage(ChatMessageInfo info) {
        Context ctx = Data.ctx;
        int channel = Data.currentBroadcasterId;

        ArrayList<ChatMessageToken> newTokens = new ArrayList<>(info.tokens.length + 10);

        boolean shouldHighlight = false;

        for (ChatMessageToken token : info.tokens) {
            if (token.type.getValue() != ChatMessageTokenType.Text.getValue()) {
                if (token.type.getValue() == ChatMessageTokenType.Emoticon.getValue() && newTokens.size() > 0) {
                    ChatMessageToken prevToken = newTokens.get(newTokens.size() - 1);
                    if (prevToken != null && prevToken.type.getValue() == ChatMessageTokenType.Emoticon.getValue()) {
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

            String[] tokens = text.split(" ");

            StringBuilder currentText = new StringBuilder();
            for(String word : tokens) {
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

        if (shouldHighlight) {
            info.messageType = "bttv-highlighted-message";
        }
    }
}
