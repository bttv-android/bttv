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
import tv.twitch.android.models.chat.AutoModMessageFlags;
import tv.twitch.android.shared.chat.pub.model.messages.MessageToken;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class TokenizerTest {
    final static AutoModMessageFlags flags = new AutoModMessageFlags();

    private static class Cases {
        static Object[] TrivialCase = {
                // Input
                Arrays.asList(new MessageToken.TextToken("Test Message, fam ", flags),
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("Haha", flags)
                ),
                // Expected Output
                Arrays.asList(new MessageToken.TextToken("Test Message, fam ", flags),
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("Haha ", flags)
                ),
        };

        static Object[] FoundInBeginning = {
                // Input
                Arrays.asList(new MessageToken.TextToken("KEKW Pog and ", flags),
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("Haha", flags)
                ),
                // Expected Output
                Arrays.asList(
                        new MessageToken.EmoticonToken("KEKW", "BTTV-5ea831f074046462f768097a"),
                        new MessageToken.TextToken(" ", flags),
                        new MessageToken.EmoticonToken("Pog", "BTTV-5ff827395ef7d10c7912c106"),
                        new MessageToken.TextToken(" and ", flags),
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("Haha ", flags)
                ),
        };

        static Object[] FoundAtEnd = {
                // Input
                Arrays.asList(new MessageToken.TextToken("Test message ", flags),
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("Haha, Pog", flags)
                ),
                // Expected Output
                Arrays.asList(
                        new MessageToken.TextToken("Test message ", flags),
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("Haha, ", flags),
                        new MessageToken.EmoticonToken("Pog", "BTTV-5ff827395ef7d10c7912c106")
                )
        };

        static Object[] FoundInMid = {
                // Input
                Arrays.asList(
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("Test message ", flags),
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("test Pog test", flags),
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("Haha", flags),
                        new MessageToken.EmoticonToken("LUL", "123")
                    ),
                // Expected Output
                Arrays.asList(
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("Test message ", flags),
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("test ", flags),
                        new MessageToken.EmoticonToken("Pog", "BTTV-5ff827395ef7d10c7912c106"),
                        new MessageToken.TextToken(" test ", flags),
                        new MessageToken.EmoticonToken("LUL", "123"),
                        new MessageToken.TextToken("Haha ", flags),
                        new MessageToken.EmoticonToken("LUL", "123")
                )
        };

        static Object[] UrlAfterMention = {
                // Input
                Arrays.asList(
                        new MessageToken.MentionToken("@bttv_android", "bttv_android", true),
                        new MessageToken.TextToken(" ", flags),
                        new MessageToken.UrlToken("https://bmn.dev/", false)
                ),
                // Expected Output
                Arrays.asList(
                        new MessageToken.MentionToken("@bttv_android", "bttv_android", true),
                        new MessageToken.TextToken(" ", flags),
                        new MessageToken.UrlToken("https://bmn.dev/", false)
                ),
        };
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

    private final List<MessageToken> fInput;

    private final List<MessageToken> fExpected;

    public TokenizerTest(List<MessageToken> input, List<MessageToken> expected) {
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
        List<MessageToken> res = Tokenizer.tokenize(fInput).first;

        System.out.println("---------------");
        for (MessageToken token : fExpected) {
            System.out.print(token + " ");
            if (token instanceof MessageToken.TextToken) {
                System.out.println("text: '" + ((MessageToken.TextToken) token).getText() + "'");
            }
            if (token instanceof MessageToken.EmoticonToken) {
                System.out.println("name: " + ((MessageToken.EmoticonToken) token).component1());
            }
        }
        System.out.println("---------------");

        // compare
        assertEquals(fExpected.size(), res.size());
        int index = 0;
        for (MessageToken expected : fExpected) {
            MessageToken received = res.get(index);
            System.out.println(received);
            // both same type
            assertEquals(expected instanceof  MessageToken.TextToken, received instanceof MessageToken.TextToken);
            assertEquals(expected instanceof  MessageToken.EmoticonToken, received instanceof MessageToken.EmoticonToken);

            if (expected instanceof  MessageToken.TextToken && received instanceof MessageToken.TextToken) {
                MessageToken.TextToken expectedTextToken = (MessageToken.TextToken) expected;
                MessageToken.TextToken receivedTextToken = (MessageToken.TextToken) received;

                assertEquals(expectedTextToken.getText(), receivedTextToken.getText());
            } else if (expected instanceof  MessageToken.EmoticonToken && received instanceof MessageToken.EmoticonToken) {
                MessageToken.EmoticonToken expectedEmoteToken = (MessageToken.EmoticonToken) expected;
                MessageToken.EmoticonToken receivedEmoteToken = (MessageToken.EmoticonToken) received;

                assertEquals(expectedEmoteToken.component1(), receivedEmoteToken.component1());
                assertEquals(expectedEmoteToken.component2(), receivedEmoteToken.component2());
            }

            index++;
        }
    }
}
