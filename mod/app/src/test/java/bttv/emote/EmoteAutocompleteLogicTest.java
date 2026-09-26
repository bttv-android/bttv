package bttv.emote;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class EmoteAutocompleteLogicTest {
    private EmoteAutocompleteLogic.Token end(String text) {
        return EmoteAutocompleteLogic.token(text, text.length(), text.length());
    }

    private EmoteAutocompleteLogic.Suggestion suggestion(String code, boolean channel) {
        return new EmoteAutocompleteLogic.Suggestion(code, "7TV", channel);
    }

    @Test public void validColonQueries() {
        for (String text : Arrays.asList(":om", "hello :om", "hello\t:om", "hello\u00a0:om")) {
            assertEquals("om", end(text).prefix);
        }
    }

    @Test public void nativeAndOrdinaryTokensDoNotTrigger() {
        for (String text : Arrays.asList("", ":", ":o", "hello", "om", "OME", "@abc", "/abc",
                "http://test", "12:30", "word:test", "hello :om ")) {
            assertNull(text, end(text));
        }
    }

    @Test public void middleOfMessage() {
        EmoteAutocompleteLogic.Token token = EmoteAutocompleteLogic.token("hello :ome test", 10, 10);
        assertEquals("ome", token.prefix);
        assertEquals(6, token.start);
        assertEquals(10, token.end);
    }

    @Test public void middleOfTokenMatchesPrefixButReplacesWholeToken() {
        EmoteAutocompleteLogic.Token token = EmoteAutocompleteLogic.token(":omega test", 3, 3);
        assertEquals("om", token.prefix);
        assertEquals(6, token.end);
        assertNull(EmoteAutocompleteLogic.token(":omega", 1, 1));
        assertNull(EmoteAutocompleteLogic.token(":omega", 2, 2));
    }

    @Test public void selectionAndInvalidCursorsDoNotTrigger() {
        assertNull(EmoteAutocompleteLogic.token(":omega", 0, 4));
        assertNull(EmoteAutocompleteLogic.token(":omega", -1, -1));
        assertNull(EmoteAutocompleteLogic.token(":omega", 7, 7));
        assertNull(EmoteAutocompleteLogic.token(null, 0, 0));
    }

    @Test public void globalOnlyCaseInsensitivePrefix() {
        List<EmoteAutocompleteLogic.Suggestion> result = EmoteAutocompleteLogic.match("oM",
                Arrays.asList(suggestion("OMEGALUL", false), suggestion("someOM", false)));
        assertEquals(1, result.size());
        assertEquals("OMEGALUL", result.get(0).code);
    }

    @Test public void channelOnlyAndNoCatalog() {
        assertTrue(EmoteAutocompleteLogic.match("om", Collections.emptyList()).isEmpty());
        assertTrue(EmoteAutocompleteLogic.match("o", Arrays.asList(suggestion("om", true))).isEmpty());
        assertTrue(EmoteAutocompleteLogic.match("om",
                Arrays.asList(suggestion("Omega", true))).get(0).channel);
    }

    @Test public void channelWinsExactDuplicateInEitherOrder() {
        EmoteAutocompleteLogic.Suggestion global = suggestion("OMEGALUL", false);
        EmoteAutocompleteLogic.Suggestion channel = suggestion("OMEGALUL", true);
        for (List<EmoteAutocompleteLogic.Suggestion> input : Arrays.asList(
                Arrays.asList(global, channel), Arrays.asList(channel, global))) {
            List<EmoteAutocompleteLogic.Suggestion> result = EmoteAutocompleteLogic.match("om", input);
            assertEquals(1, result.size());
            assertSame(channel, result.get(0));
        }
    }

    @Test public void shortestThenAlphabeticalThenChannelTie() {
        List<EmoteAutocompleteLogic.Suggestion> result = EmoteAutocompleteLogic.match("om", Arrays.asList(
                suggestion("OMEGALUL", true), suggestion("omz", false), suggestion("oma", false),
                suggestion("om", false), suggestion("OMA", true)));
        assertEquals(Arrays.asList("om", "OMA", "oma", "omz", "OMEGALUL"), codes(result));
    }

    @Test public void exactDuplicateKeepsFirstProviderAtSameScope() {
        EmoteAutocompleteLogic.Suggestion first = new EmoteAutocompleteLogic.Suggestion("om", "FFZ", true);
        assertSame(first, EmoteAutocompleteLogic.match("om", Arrays.asList(first, suggestion("om", true))).get(0));
    }

    @Test public void limitEightAndDeterministicInputOrder() {
        List<EmoteAutocompleteLogic.Suggestion> input = new ArrayList<>();
        for (int i = 12; i >= 0; i--) input.add(suggestion("om" + i, false));
        List<String> before = codes(EmoteAutocompleteLogic.match("om", input));
        assertEquals(Arrays.asList("om0", "om1", "om2", "om3", "om4", "om5", "om6", "om7"), before);
        Collections.reverse(input);
        assertEquals(before, codes(EmoteAutocompleteLogic.match("om", input)));
    }

    @Test public void malformedCodesAreIgnored() {
        assertTrue(EmoteAutocompleteLogic.match("om", Arrays.asList(null, suggestion(null, false),
                suggestion("om bad", false), suggestion("om\n", false), suggestion("om\u0000", false))).isEmpty());
    }

    private List<String> codes(List<EmoteAutocompleteLogic.Suggestion> matches) {
        List<String> result = new ArrayList<>();
        for (EmoteAutocompleteLogic.Suggestion match : matches) result.add(match.code);
        return result;
    }

    private void insertion(String original, int cursor, String expected, int expectedCursor) {
        EmoteAutocompleteLogic.Token token = EmoteAutocompleteLogic.token(original, cursor, cursor);
        EmoteAutocompleteLogic.Insertion plan = EmoteAutocompleteLogic.insertion(original, token, "OMEGALUL");
        assertNotNull(plan);
        String actual = new StringBuilder(original).replace(plan.start, plan.end, plan.replacement).toString();
        assertEquals(expected, actual);
        assertEquals(expectedCursor, plan.cursor);
    }

    @Test public void insertAtBeginningAndEnd() {
        insertion(":ome", 4, "OMEGALUL ", 9);
        insertion("hello :ome", 10, "hello OMEGALUL ", 15);
    }

    @Test public void preserveSuffixAndExistingWhitespace() {
        insertion("hello :ome world", 10, "hello OMEGALUL world", 15);
        insertion(":ome  world", 4, "OMEGALUL  world", 9);
        insertion(":ome\tworld", 4, "OMEGALUL\tworld", 9);
        insertion(":ome\nworld", 4, "OMEGALUL\nworld", 9);
    }

    @Test public void replaceEntireTokenAtInternalCursor() {
        insertion("hello :omega world", 9, "hello OMEGALUL world", 15);
    }

    @Test public void rejectStaleOrInvalidInsertion() {
        EmoteAutocompleteLogic.Token token = end(":ome");
        assertNull(EmoteAutocompleteLogic.insertion(":abc", token, "OMEGALUL"));
        assertNull(EmoteAutocompleteLogic.insertion(":ome", token, "bad code"));
        assertNull(EmoteAutocompleteLogic.insertion(":ome", null, "OMEGALUL"));
    }

    private EmoteAutocompleteLogic.Suggestion nativeItem(String code) {
        return EmoteAutocompleteLogic.Suggestion.nativeItem(code, new Object());
    }

    @Test public void nativeExactPrecedesProviderPrefixes() {
        assertEquals(Arrays.asList("OM", "oma", "Omega"), codes(EmoteAutocompleteLogic.merge("om",
                Arrays.asList(nativeItem("OM")), Arrays.asList(suggestion("Omega", false), suggestion("oma", true)))));
    }

    @Test public void providerExactPrecedesNativePrefixes() {
        assertEquals(Arrays.asList("om", "oma", "omega"), codes(EmoteAutocompleteLogic.merge("om",
                Arrays.asList(nativeItem("omega")), Arrays.asList(suggestion("oma", false), suggestion("om", true)))));
    }

    @Test public void nativeWinsCanonicalCollision() {
        EmoteAutocompleteLogic.Suggestion original = nativeItem("OMEGALUL");
        List<EmoteAutocompleteLogic.Suggestion> result = EmoteAutocompleteLogic.merge("om",
                Arrays.asList(original), Arrays.asList(suggestion("OMEGALUL", true)));
        assertEquals(1, result.size());
        assertSame(original, result.get(0));
    }

    @Test public void mergedChannelWinsGlobalCollision() {
        EmoteAutocompleteLogic.Suggestion channel = suggestion("Omega", true);
        List<EmoteAutocompleteLogic.Suggestion> result = EmoteAutocompleteLogic.merge("om", Collections.emptyList(),
                Arrays.asList(suggestion("Omega", false), channel));
        assertEquals(1, result.size());
        assertSame(channel, result.get(0));
    }

    @Test public void combinedPrefixesRankBeforeNativeSubstrings() {
        assertEquals(Arrays.asList("oma", "omz", "omeg", "OMEGALUL", "someOM", "xxom"),
                codes(EmoteAutocompleteLogic.merge("om",
                        Arrays.asList(nativeItem("OMEGALUL"), nativeItem("omz"), nativeItem("someOM"), nativeItem("xxom")),
                        Arrays.asList(suggestion("omeg", true), suggestion("oma", false)))));
    }

    @Test public void combinedLimitTenDoesNotPrecapProvidersToEight() {
        List<EmoteAutocompleteLogic.Suggestion> candidates = new ArrayList<>();
        for (int i = 0; i < 20; i++) candidates.add(suggestion("om" + i, false));
        assertEquals(10, EmoteAutocompleteLogic.merge("om", Collections.emptyList(), candidates).size());
        List<EmoteAutocompleteLogic.Suggestion> natives = new ArrayList<>();
        for (int i = 0; i < 15; i++) natives.add(nativeItem("someom" + i));
        List<EmoteAutocompleteLogic.Suggestion> result = EmoteAutocompleteLogic.merge("om", natives, candidates);
        assertEquals(10, result.size());
        assertNull(result.get(0).nativeItem);
    }

    @Test public void nativeOnlyStillPreservesSubstringOrder() {
        assertEquals(Arrays.asList("OM", "omz", "Omega", "someom", "xxom"),
                codes(EmoteAutocompleteLogic.merge("om", Arrays.asList(nativeItem("Omega"), nativeItem("OM"),
                        nativeItem("omz"), nativeItem("someom"), nativeItem("xxom")), Collections.emptyList())));
    }

    @Test public void noResultsAndEmptyNativeAreSupported() {
        assertTrue(EmoteAutocompleteLogic.merge("om", Collections.emptyList(), Collections.emptyList()).isEmpty());
        assertEquals(Arrays.asList("OMEGALUL"), codes(EmoteAutocompleteLogic.merge("om", Collections.emptyList(),
                Arrays.asList(suggestion("OMEGALUL", true), suggestion("someom", false)))));
    }

    @Test public void canonicalCaseVariantsAreNotIdenticalCollisions() {
        assertEquals(Arrays.asList("OM", "om"), codes(EmoteAutocompleteLogic.merge("om",
                Arrays.asList(nativeItem("OM")), Arrays.asList(suggestion("om", true)))));
    }
}
