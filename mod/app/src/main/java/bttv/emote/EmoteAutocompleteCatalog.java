package bttv.emote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;

/** Read existing published catalogs only; never request data or maintain another cache. */
final class EmoteAutocompleteCatalog {
    private EmoteAutocompleteCatalog() {}

    static List<EmoteAutocompleteLogic.Suggestion> find(String channelId, String prefix) {
        List<EmoteAutocompleteLogic.Suggestion> candidates = new ArrayList<>();
        boolean gifs = !ResUtil.selectedDropdownFromSettingsIs(
                Settings.GifRenderMode, Res.strings.bttv_settings_gif_render_mode_disabled);
        // Preserve the renderer's provider precedence for otherwise identical entries.
        if (ResUtil.getBooleanFromSettings(Settings.FFZEmotesEnabled)) {
            add(candidates, Emotes.globalEmotesFFZ, "FFZ", false, gifs, prefix);
            add(candidates, LiveEmoteChannels.snapshot(channelId, Emotes.Source.FFZ),
                    "FFZ", true, gifs, prefix);
        }
        if (ResUtil.getBooleanFromSettings(Settings.BTTVEmotesEnabled)) {
            add(candidates, Emotes.globalEmotesBTTV, "BTTV", false, gifs, prefix);
            add(candidates, LiveEmoteChannels.snapshot(channelId, Emotes.Source.BTTV),
                    "BTTV", true, gifs, prefix);
        }
        if (ResUtil.getBooleanFromSettings(Settings.SevenTVEmotesEnabled)) {
            add(candidates, Emotes.globalEmotes7TV, "7TV", false, gifs, prefix);
            add(candidates, LiveEmoteChannels.snapshot(channelId, Emotes.Source.STV),
                    "7TV", true, gifs, prefix);
        }
        // Apply the ten-row cap only AFTER native collisions and combined ranking.
        return EmoteAutocompleteLogic.match(prefix, candidates, Integer.MAX_VALUE);
    }

    private static void add(List<EmoteAutocompleteLogic.Suggestion> candidates,
                            Map<String, Emote> catalog, String provider, boolean channel,
                            boolean gifs, String prefix) {
        if (catalog == null) return;
        for (Emote emote : catalog.values()) {
            if (emote != null && emote.code != null
                    && emote.code.regionMatches(true, 0, prefix, 0, prefix.length())
                    && AutocompleteSyntheticId.encode(provider, emote.id) != null
                    && AutocompleteSyntheticId.validImageUrl(emote.url)
                    && (gifs || !"gif".equals(emote.imageType))) {
                candidates.add(new EmoteAutocompleteLogic.Suggestion(emote.code, provider, channel,
                        emote.id, emote.url, emote.imageType, null));
            }
        }
    }
}
