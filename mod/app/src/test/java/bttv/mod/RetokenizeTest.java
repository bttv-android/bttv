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

import bttv.Data;
import bttv.Res;
import bttv.Tokenizer;
import bttv.emote.Emote;
import bttv.emote.Emotes;
import tv.twitch.chat.ChatEmoticonToken;
import tv.twitch.chat.ChatMessageInfo;
import tv.twitch.chat.ChatMessageToken;
import tv.twitch.chat.ChatMessageTokenType;
import tv.twitch.chat.ChatTextToken;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class RetokenizeTest {
    private static class Cases {

        static Object[] TrivialCase;

        static {
            ChatTextToken t1 = new ChatTextToken();
            t1.text = "Test Message, fam ";

            ChatEmoticonToken t2 = new ChatEmoticonToken();
            t2.emoticonText = "LUL";
            t2.emoticonId = "123";

            ChatTextToken t3 = new ChatTextToken();
            t3.text = "Haha";

            ChatTextToken e3 = new ChatTextToken();
            e3.text = "Haha ";


            ChatMessageInfo input = new ChatMessageInfo();
            input.tokens = new ChatMessageToken[]{t1, t2, t3};

            ChatMessageInfo expected = new ChatMessageInfo();
            expected.tokens = new ChatMessageToken[]{t1, t2, e3};

            TrivialCase = new Object[]{input, expected};
        }

        static Object[] FoundInBeginning;

        static {

            ChatTextToken t1 = new ChatTextToken();
            t1.text = "KEKW Pog and ";

            ChatEmoticonToken t2 = new ChatEmoticonToken();
            t2.emoticonText = "LUL";
            t2.emoticonId = "123";

            ChatTextToken t3 = new ChatTextToken();
            t3.text = "Haha";

            ChatEmoticonToken e1 = new ChatEmoticonToken();
            e1.emoticonText = "KEKW";
            e1.emoticonId = "BTTV-5ea831f074046462f768097a";

            ChatEmoticonToken e2 = new ChatEmoticonToken();
            e2.emoticonText = "Pog";
            e2.emoticonId = "BTTV-5ff827395ef7d10c7912c106";

            ChatTextToken e3 = new ChatTextToken();
            e3.text = " and ";

            ChatTextToken e4 = new ChatTextToken();
            e4.text = "Haha ";

            ChatMessageInfo input = new ChatMessageInfo();
            input.tokens = new ChatMessageToken[]{t1, t2, t3};

            ChatMessageInfo expected = new ChatMessageInfo();
            expected.tokens = new ChatMessageToken[]{e1, e2, e3, t2, e4};

            FoundInBeginning = new Object[]{input, expected};
        }

        static Object[] FoundAtEnd;

        static {

            ChatTextToken t1 = new ChatTextToken();
            t1.text = "Test message ";

            ChatEmoticonToken t2 = new ChatEmoticonToken();
            t2.emoticonText = "LUL";
            t2.emoticonId = "123";

            ChatTextToken t3 = new ChatTextToken();
            t3.text = "Haha, Pog";

            ChatTextToken e3 = new ChatTextToken();
            e3.text = "Haha, ";

            ChatEmoticonToken e4 = new ChatEmoticonToken();
            e4.emoticonText = "Pog";
            e4.emoticonId = "BTTV-5ff827395ef7d10c7912c106";


            ChatMessageInfo input = new ChatMessageInfo();
            input.tokens = new ChatMessageToken[]{t1, t2, t3};

            ChatMessageInfo expected = new ChatMessageInfo();
            expected.tokens = new ChatMessageToken[]{t1, t2, e3, e4};

            FoundAtEnd = new Object[]{input, expected};
        }

        static Object[] FoundInMid;

        static {
            ChatEmoticonToken t1 = new ChatEmoticonToken();
            t1.emoticonText = "LUL";
            t1.emoticonId = "123";

            ChatTextToken t2 = new ChatTextToken();
            t2.text = "Test message ";

            ChatTextToken t3 = new ChatTextToken();
            t3.text = "test Pog test";

            ChatTextToken t4 = new ChatTextToken();
            t4.text = "Haha";

            ChatTextToken e4 = new ChatTextToken();
            e4.text = "test ";

            ChatEmoticonToken e5 = new ChatEmoticonToken();
            e5.emoticonText = "Pog";
            e5.emoticonId = "BTTV-5ff827395ef7d10c7912c106";

            ChatTextToken e6 = new ChatTextToken();
            e6.text = " test ";

            ChatTextToken e7 = new ChatTextToken();
            e7.text = "Haha ";


            ChatMessageInfo input = new ChatMessageInfo();
            input.tokens = new ChatMessageToken[]{t1, t2, t1, t3, t1, t4, t1};

            ChatMessageInfo expected = new ChatMessageInfo();
            expected.tokens = new ChatMessageToken[]{t1, t2, t1, e4, e5, e6, t1, e7, t1};

            FoundInMid = new Object[]{input, expected};
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                Cases.TrivialCase,
                Cases.FoundInBeginning,
                Cases.FoundAtEnd,
                Cases.FoundInMid
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
                    new Emote("5ea831f074046462f768097a", Emotes.Source.FFZ, "KEKW", null, "png"),
                    new Emote("5ff827395ef7d10c7912c106", Emotes.Source.FFZ,"Pog", null, "png"))
        );

        // test
        Tokenizer.retokenizeLiveChatMessage(fInput);

        System.out.println("---------------");
        for (ChatMessageToken token : fExpected.tokens) {
            System.out.print(token + " ");
            if (token.type.getValue() == ChatMessageTokenType.Text.getValue()) {
                System.out.println("text: '" + ((ChatTextToken) token).text + "'");
            }
            if (token.type.getValue() == ChatMessageTokenType.Emoticon.getValue()) {
                assert token instanceof ChatEmoticonToken;
                System.out.println("name: " + ((ChatEmoticonToken) token).emoticonText);
            }
        }
        System.out.println("---------------");

        // compare
        assertEquals(fExpected.tokens.length, fInput.tokens.length);
        int index = 0;
        for (ChatMessageToken expected : fExpected.tokens) {
            ChatMessageToken received = fInput.tokens[index];
            System.out.println(received);

            // both same type
            assertEquals(expected.type.getValue(), received.type.getValue());

            if (expected.type.getValue() == ChatMessageTokenType.Text.getValue()) {
                ChatTextToken expectedTextToken = (ChatTextToken) expected;
                ChatTextToken receivedTextToken = (ChatTextToken) received;

                assertEquals(expectedTextToken.text, receivedTextToken.text);
            } else if (expected.type.getValue() == ChatMessageTokenType.Emoticon.getValue()) {
                ChatEmoticonToken expectedEmoteToken = (ChatEmoticonToken) expected;
                ChatEmoticonToken receivedEmoteToken = (ChatEmoticonToken) received;

                assertEquals(expectedEmoteToken.emoticonText, receivedEmoteToken.emoticonText);
                assertEquals(expectedEmoteToken.emoticonId, receivedEmoteToken.emoticonId);
            }

            index++;
        }
    }
}
