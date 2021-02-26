package bttv;

import tv.twitch.chat.ChatMessageInfo;

import java.util.ArrayList;
import java.util.List;

import tv.twitch.android.models.chat.MessageToken;
import tv.twitch.android.models.chat.MessageToken.TextToken;
import tv.twitch.android.models.chat.MessageToken.EmoticonToken;
import tv.twitch.android.shared.chat.ChatMessageDelegate;

public class ChatMessageDeligateWrapper extends ChatMessageDelegate {

    public ChatMessageDeligateWrapper(ChatMessageInfo chatMessageInfo) {
        super(chatMessageInfo);
    }

    @Override
    public List<MessageToken> getTokens() {
        List<MessageToken> orig = super.getTokens();

        // Don't add Emotes, when we dont have the chnnel's emotes (yet)
        if (!Data.channelHasEmotes(Data.currentBroadcasterId)) {
            return orig;
        }

        ArrayList<MessageToken> newTokens = new ArrayList<>();

        for (MessageToken token : orig) {
            // possible issue: emotes won't work in e.g. MentionToken or BitsToken
            if (!(token instanceof TextToken)) {
                newTokens.add(token);
                continue;
            }

            TextToken text = (TextToken) token;
            String[] words = text.getText().split(" ");

            int mergeStart = 0;
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                if (Data.isEmote(word, Data.currentBroadcasterId)) {

                    String merge = mergePrevious(words, mergeStart, i);
                    newTokens.add(new TextToken(merge, text.getFlags()));
                    mergeStart = i + 1;

                    Emote emote = Data.getEmote(word);
                    if (emote == null) {
                        newTokens.add(new TextToken(word, text.getFlags()));

                    } else {
                        newTokens.add(new EmoticonToken(word, "BTTV-" + emote.id));
                    }
                }
            }

            String merge = mergePrevious(words, mergeStart, words.length);
            newTokens.add(new TextToken(merge, text.getFlags()));
        }

        return newTokens;
    }

    private static String mergePrevious(String[] words, int start, int end) {
        String merge = start != 0 ? " " : ""; // space when we added an emote before it
        for (int j = start; j < end; j++) {
            merge += (words[j] + " ");
        }
        return merge;
    }

}
