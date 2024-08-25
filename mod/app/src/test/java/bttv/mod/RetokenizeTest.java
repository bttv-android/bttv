package bttv.mod;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import bttv.Data;
import bttv.Res;
import bttv.Tokenizer;
import bttv.emote.Emote;
import bttv.emote.Emotes;
import tv.twitch.chat.ChatEmoticonToken;
import tv.twitch.chat.ChatMentionToken;
import tv.twitch.chat.library.model.ChatMessageInfo;
import tv.twitch.chat.ChatMessageToken;
import tv.twitch.chat.ChatMessageTokenType;
import tv.twitch.chat.ChatTextToken;
import tv.twitch.chat.ChatUrlToken;
import tv.twitch.chat.library.model.MessageToken;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class RetokenizeTest {
    private static class Cases {

        static Object[] TrivialCase;

        static {
            MessageToken.TextToken t1 = new MessageToken.TextToken("Test Message, fam ", null);
            MessageToken.EmoteToken t2 = new MessageToken.EmoteToken("LUL", "123");
            MessageToken.TextToken t3 = new MessageToken.TextToken("Haha", null);
            MessageToken.TextToken e3 = new MessageToken.TextToken("Haha ", null);

            ChatMessageInfo input = new ChatMessageInfo();
            input.tokens = List.of(t1, t2, t3);
            ChatMessageInfo expected = new ChatMessageInfo();
            expected.tokens = List.of(t1, t2, e3);

            TrivialCase = new Object[]{input, expected};
        }

        static Object[] FoundInBeginning;

        static {

            MessageToken.TextToken t1 = new MessageToken.TextToken("KEKW Pog and ", null);
            MessageToken.EmoteToken t2 = new MessageToken.EmoteToken("LUL", "123");
            MessageToken.TextToken t3 = new MessageToken.TextToken("Haha", null);
            MessageToken.EmoteToken e1 = new MessageToken.EmoteToken("KEKW", "BTTV-5ea831f074046462f768097a");
            MessageToken.TextToken e2 = new MessageToken.TextToken(" ", null);
            MessageToken.EmoteToken e3 = new MessageToken.EmoteToken("Pog", "BTTV-5ff827395ef7d10c7912c106");
            MessageToken.TextToken e4 = new MessageToken.TextToken(" and ", null);
            MessageToken.TextToken e5 = new MessageToken.TextToken("Haha ", null);

            ChatMessageInfo input = new ChatMessageInfo();
            input.tokens = List.of(t1, t2, t3);
            ChatMessageInfo expected = new ChatMessageInfo();
            expected.tokens = List.of(e1, e2, e3, e4, t2, e5);

            FoundInBeginning = new Object[]{input, expected};
        }

        static Object[] FoundAtEnd;

        static {

            MessageToken.TextToken t1 = new MessageToken.TextToken("Test message ", null);
            MessageToken.EmoteToken t2 = new MessageToken.EmoteToken("LUL", "123");
            MessageToken.TextToken t3 = new MessageToken.TextToken("Haha, Pog", null);
            MessageToken.TextToken e3 = new MessageToken.TextToken("Haha, ", null);
            MessageToken.EmoteToken e4 = new MessageToken.EmoteToken("Pog", "BTTV-5ff827395ef7d10c7912c106");

            ChatMessageInfo input = new ChatMessageInfo();
            input.tokens = List.of(t1, t2, t3);
            ChatMessageInfo expected = new ChatMessageInfo();
            expected.tokens = List.of(t1, t2, e3, e4);

            FoundAtEnd = new Object[]{input, expected};
        }

        static Object[] FoundInMid;

        static {
            MessageToken.EmoteToken t1 = new MessageToken.EmoteToken("LUL", "123");
            MessageToken.TextToken t2 = new MessageToken.TextToken("Test message ", null);
            MessageToken.TextToken t3 = new MessageToken.TextToken("test Pog test", null);
            MessageToken.TextToken t4 = new MessageToken.TextToken("Haha", null);
            MessageToken.TextToken e4 = new MessageToken.TextToken("test ", null);
            MessageToken.EmoteToken e5 = new MessageToken.EmoteToken("Pog", "BTTV-5ff827395ef7d10c7912c106");
            MessageToken.TextToken e6 = new MessageToken.TextToken(" test ", null);
            MessageToken.TextToken e7 = new MessageToken.TextToken("Haha ", null);

            ChatMessageInfo input = new ChatMessageInfo();
            input.tokens = List.of(t1, t2, t1, t3, t1, t4, t1);
            ChatMessageInfo expected = new ChatMessageInfo();
            expected.tokens = List.of(t1, t2, t1, e4, e5, e6, t1, e7, t1);

            FoundInMid = new Object[]{input, expected};
        }

        static Object[] UrlAfterMention;

        static {
            MessageToken.MentionToken mentionToken = new MessageToken.MentionToken();
            MessageToken.TextToken textToken = new MessageToken.TextToken(" ", null);
            MessageToken.UrlToken urlToken = new MessageToken.UrlToken();

            ChatMessageInfo input = new ChatMessageInfo();
            input.tokens = List.of(mentionToken, textToken, urlToken);
            ChatMessageInfo expected = new ChatMessageInfo();
            expected.tokens = List.of(mentionToken, textToken, urlToken);

            UrlAfterMention = new Object[]{input, expected};
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                Cases.TrivialCase,
                Cases.FoundInBeginning,
                Cases.FoundAtEnd,
                Cases.FoundInMid,
                Cases.UrlAfterMention
        );
    }

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    Context mockContext;

    @Mock
    SharedPreferences mockPrefs;

    private final ChatMessageInfo fInput;

    private final ChatMessageInfo fExpected;

    public RetokenizeTest(ChatMessageInfo input, ChatMessageInfo expected) {
        this.fInput = input;
        this.fExpected = expected;
    }

    @Test
    public void test() {
        // Set up mocks
        when(mockContext.getSharedPreferences("BTTV", 0))
                .thenReturn(mockPrefs);
        when(mockPrefs.getBoolean("enable_ffz_emotes", true)).thenReturn(true);
        when(mockPrefs.getString("bttv_gif_render_mode", Res.strings.bttv_settings_gif_render_mode_animate.name()))
                .thenReturn(Res.strings.bttv_settings_gif_render_mode_animate.name());
        Data.setContext(mockContext);
        Data.setCurrentBroadcasterId(10);
        Emotes.addChannelFFZ(10,
                Arrays.asList(
                    new Emote("5ea831f074046462f768097a", Emotes.Source.FFZ, "KEKW", null, "png", "FoseFx"),
                    new Emote("5ff827395ef7d10c7912c106", Emotes.Source.FFZ,"Pog", null, "png", "FoseFx"))
        );

        // test
        Tokenizer.retokenizeLiveChatMessage(fInput);

        System.out.println("---------------");
        for (MessageToken token : fExpected.tokens) {
            System.out.print(token + " ");
            if (token instanceof  MessageToken.TextToken) {
                System.out.println("text: '" + ((MessageToken.TextToken) token).getText() + "'");
            }
            if (token instanceof  MessageToken.EmoteToken) {
                System.out.println("name: " + ((MessageToken.EmoteToken) token).getText());
            }
        }
        System.out.println("---------------");

        // compare
        assertEquals(fExpected.tokens.size(), fInput.tokens.size());
        int index = 0;
        for (MessageToken expected : fExpected.tokens) {
            MessageToken received = fInput.tokens.get(index);
            System.out.println(received);

            if (expected instanceof MessageToken.TextToken) {
                MessageToken.TextToken expectedTextToken = (MessageToken.TextToken) expected;
                MessageToken.TextToken receivedTextToken = (MessageToken.TextToken) received;

                assertEquals(expectedTextToken.getText(), receivedTextToken.getText());
            } else if (expected instanceof MessageToken.EmoteToken) {
                MessageToken.EmoteToken expectedEmoteToken = (MessageToken.EmoteToken) expected;
                MessageToken.EmoteToken receivedEmoteToken = (MessageToken.EmoteToken) received;

                assertEquals(expectedEmoteToken.getText(), receivedEmoteToken.getText());
                assertEquals(expectedEmoteToken.getId(), receivedEmoteToken.getId());
            }

            index++;
        }
    }
}
