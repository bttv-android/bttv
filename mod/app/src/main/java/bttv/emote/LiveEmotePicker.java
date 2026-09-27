package bttv.emote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;

/** Full, uncapped enumeration of existing catalog snapshots. No prefix search or HTTP. */
public final class LiveEmotePicker {
    static final class Entry {
        final String provider, id, code, url;
        final boolean channel, animated;
        Entry(String provider, boolean channel, String id, String code, String url, boolean animated) {
            this.provider = provider; this.channel = channel; this.id = id;
            this.code = code; this.url = url; this.animated = animated;
        }
        int group() { return (channel ? 0 : 3) + ("STV".equals(provider) ? 0 : "BTTV".equals(provider) ? 1 : 2); }
    }

    static final class Catalog {
        final String provider;
        final boolean channel, enabled;
        final Map<String, Emote> values;
        Catalog(String provider, boolean channel, boolean enabled, Map<String, Emote> values) {
            this.provider = provider; this.channel = channel; this.enabled = enabled; this.values = values;
        }
    }

    static List<Entry> enumerate(List<Catalog> catalogs, boolean gifs, Set<String> nativeCodes) {
        Set<String> seen = new HashSet<>(nativeCodes);
        List<Entry> result = new ArrayList<>();
        for (Catalog catalog : catalogs) {
            if (!catalog.enabled || catalog.values == null) continue;
            for (Map.Entry<String, Emote> pair : catalog.values.entrySet()) {
                Emote emote = pair.getValue();
                String code = pair.getKey();
                // Disabled animation is ineligible in the live renderer, not a malformed winner.
                if (emote != null && !gifs && "gif".equals(emote.imageType)) continue;
                if (!seen.add(code)) continue;
                if (emote == null || !PickerSyntheticId.token(code) || !code.equals(emote.code)
                        || emote.source == null || !catalog.provider.equals(emote.source.name())
                        || !PickerSyntheticId.token(emote.id)
                        || !AutocompleteSyntheticId.validImageUrl(emote.url)) continue;
                result.add(new Entry(catalog.provider, catalog.channel, emote.id, code, emote.url,
                        "gif".equals(emote.imageType)));
            }
        }
        result.sort(Comparator.comparingInt(Entry::group)
                .thenComparing(e -> e.code, String.CASE_INSENSITIVE_ORDER).thenComparing(e -> e.code));
        return result;
    }

    static List<Entry> snapshot(String channel, Set<String> nativeCodes) {
        return snapshot(channel, nativeCodes, null);
    }

    static Entry winner(String channel, Set<String> nativeCodes, String code) {
        List<Entry> entries = snapshot(channel, nativeCodes, code);
        return entries.isEmpty() ? null : entries.get(0);
    }

    private static List<Entry> snapshot(String channel, Set<String> nativeCodes, String code) {
        Map<String, Emote> ffz = Emotes.globalEmotesFFZ;
        Map<String, Emote> fc = channels(channel, Emotes.Source.FFZ);
        Map<String, Emote> bttv = Emotes.globalEmotesBTTV;
        Map<String, Emote> bc = channels(channel, Emotes.Source.BTTV);
        Map<String, Emote> stv = Emotes.globalEmotes7TV;
        Map<String, Emote> sc = channels(channel, Emotes.Source.STV);
        boolean f = ResUtil.getBooleanFromSettings(Settings.FFZEmotesEnabled);
        boolean b = ResUtil.getBooleanFromSettings(Settings.BTTVEmotesEnabled);
        boolean s = ResUtil.getBooleanFromSettings(Settings.SevenTVEmotesEnabled);
        boolean gifs = !ResUtil.selectedDropdownFromSettingsIs(Settings.GifRenderMode,
                Res.strings.bttv_settings_gif_render_mode_disabled);
        List<Catalog> catalogs = new ArrayList<>();
        catalogs.add(new Catalog("FFZ", false, f, select(ffz, code))); catalogs.add(new Catalog("FFZ", true, f, select(fc, code)));
        catalogs.add(new Catalog("BTTV", false, b, select(bttv, code))); catalogs.add(new Catalog("BTTV", true, b, select(bc, code)));
        catalogs.add(new Catalog("STV", false, s, select(stv, code))); catalogs.add(new Catalog("STV", true, s, select(sc, code)));
        return enumerate(catalogs, gifs, nativeCodes);
    }

    private static Map<String, Emote> select(Map<String, Emote> catalog, String code) {
        if (code == null || catalog == null) return catalog;
        return catalog.containsKey(code) ? Collections.singletonMap(code, catalog.get(code)) : Collections.emptyMap();
    }

    static boolean sameBinding(Object expectedInput, Object actualInput, Object expectedOwner, Object actualOwner,
                               String expectedChannel, String actualChannel, long generation, long actualGeneration,
                               boolean active) {
        return active && expectedInput != null && expectedInput == actualInput && expectedOwner != null
                && expectedOwner == actualOwner && generation == actualGeneration
                && expectedChannel != null && expectedChannel.equals(actualChannel);
    }

    private static Map<String, Emote> channels(String channel, Emotes.Source source) {
        return PickerSyntheticId.positive(channel) ? LiveEmoteChannels.snapshot(channel, source) : Collections.emptyMap();
    }
    private LiveEmotePicker() {}
}
