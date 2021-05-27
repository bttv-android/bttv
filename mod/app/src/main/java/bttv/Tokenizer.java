package bttv;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import bttv.emote.Emote;
import tv.twitch.android.models.chat.MessageToken.TextToken;
import tv.twitch.android.models.chat.MessageToken;
import tv.twitch.android.models.chat.MessageToken.EmoticonToken;

public class Tokenizer {

    public static List<MessageToken> tokenize(List<MessageToken> orig) {
        int channel = Data.currentBroadcasterId;
        // Don't add Emotes, when we don't have the channel's emotes (yet)
        if (!Data.channelHasEmotes(channel)) {
            return orig;
        }

        ArrayList<MessageToken> newTokens = new ArrayList<>(orig.size() + 5);

        for (MessageToken token : orig) {
            // possible issue: emotes won't work in e.g. MentionToken or BitsToken
            if (!(token instanceof TextToken)) {
                newTokens.add(token);
                continue;
            }

            TextToken text = (TextToken) token;
            StringTokenizer tokens = new StringTokenizer(text.getText(), " ");
            System.out.println(text.getText() + " " + tokens.countTokens());

            StringBuilder currentText = new StringBuilder();
            while (tokens.hasMoreTokens()) {
                String word = tokens.nextToken();
                Emote emote = Data.getEmote(word, channel);
                if (emote == null) {
                    currentText.append(word).append(" ");
                    continue;
                }
                // emote found
                String before = currentText.toString();
                if (!before.trim().isEmpty()) {
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
}
