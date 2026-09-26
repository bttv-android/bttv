package bttv.emote;

import org.junit.Test;
import static org.junit.Assert.*;

public class AutocompleteSyntheticIdTest {
    @Test public void bttvRoundTrip() { roundTrip("BTTV", "abc123", "BTTV"); }
    @Test public void ffzRoundTrip() { roundTrip("FFZ", "1234", "FFZ"); }
    @Test public void sevenTvRoundTrip() { roundTrip("7TV", "01ABCDEF", "STV"); }
    @Test public void escapingIsDeterministic() {
        roundTrip("STV", "id/+%?é", "STV");
        assertEquals("bttv31ac://STV/id%2F%2B%25%3F%C3%A9", AutocompleteSyntheticId.encode("STV", "id/+%?é"));
    }

    private void roundTrip(String provider, String id, String expectedProvider) {
        String encoded = AutocompleteSyntheticId.encode(provider, id);
        AutocompleteSyntheticId.Decoded result = AutocompleteSyntheticId.decode(encoded);
        assertNotNull(result);
        assertEquals(expectedProvider, result.provider);
        assertEquals(id, result.id);
        assertEquals(encoded, AutocompleteSyntheticId.encode(result.provider, result.id));
    }

    @Test public void stockTwitchIdsAreNotSynthetic() {
        assertNull(AutocompleteSyntheticId.decode("304412430"));
        assertNull(AutocompleteSyntheticId.decode("emotesv2_abc"));
        assertNull(AutocompleteSyntheticId.decode(null));
    }

    @Test public void malformedSyntheticIdsFailOpen() {
        for (String value : new String[]{"bttv31ac://BTTV", "bttv31ac://BTTV/", "bttv31ac://BAD/123",
                "bttv31ac://FFZ/%", "bttv31ac://STV/%FF", "bttv31ac://STV/raw/id",
                "bttv31ac://STV/id?url=https://example.com", "bttv31ac://STV/%0A"}) {
            assertNull(value, AutocompleteSyntheticId.decode(value));
        }
    }

    @Test public void invalidProviderIdsAreRejected() {
        assertNull(AutocompleteSyntheticId.encode("BAD", "123"));
        assertNull(AutocompleteSyntheticId.encode("BTTV", ""));
        assertNull(AutocompleteSyntheticId.encode("FFZ", "bad id"));
        assertNull(AutocompleteSyntheticId.encode("STV", null));
    }

    @Test public void imageUrlMustBeKnownHttpsUrl() {
        assertTrue(AutocompleteSyntheticId.validImageUrl("https://cdn.7tv.app/emote/id/2x.webp"));
        for (String value : new String[]{null, "", "http://example.com/a", "file:///tmp/a",
                "https:///a", "https://example.com/a b", "https://user@example.com/a"}) {
            assertFalse(AutocompleteSyntheticId.validImageUrl(value));
        }
    }
}
