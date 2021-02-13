package bttv;

import tv.twitch.chat.ChatMessageInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.util.Log;
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
        if (!Data.availEmoteSetMap.containsKey(Data.currentBroadcasterId)) {
            return orig;
        }
        Set<String> enabledEmotes = Data.availEmoteSetMap.get(Data.currentBroadcasterId);
        Log.d("BTTVChatMessageWrapper",
                "getTokens(), channel: " + Data.currentBroadcasterId + " enabled: " + enabledEmotes.toString());

        ArrayList<MessageToken> newTokens = new ArrayList<>();

        for (MessageToken token : orig) {
            if (!(token instanceof TextToken)) {
                newTokens.add(token);
                continue;
            }

            TextToken text = (TextToken) token;
            String[] words = text.getText().split(" ");
            Log.d("BTTVChatMessageWrapper", Arrays.toString(words));

            int mergeUntil = 0;
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                if (enabledEmotes.contains(word)) {
                    Log.d("BTTVChatMessageWrapper", word);
                    String merge = "";
                    for (int j = mergeUntil; j < i; j++) {
                        merge += (words[j] + " ");
                    }
                    newTokens.add(new TextToken(merge, text.getFlags()));
                    mergeUntil = i + 1;

                    Emote emote = Data.emoteMap.get(word);
                    if (emote == null) {
                        newTokens.add(new TextToken(word, text.getFlags()));

                    } else {
                        newTokens.add(new EmoticonToken(word, "BTTV-" + emote.id));
                    }
                }
            }

            String merge = "";
            for (int j = mergeUntil; j < words.length; j++) {
                merge += (words[j] + " ");
            }
            newTokens.add(new TextToken(merge, text.getFlags()));
            Log.d("BTTVChatMessageWrapper", Arrays.toString(newTokens.toArray()));

        }

        Log.d("BTTVChatMessageWrapper", Arrays.toString(newTokens.toArray()));

        return newTokens;
    }

}
