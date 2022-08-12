package bttv.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import bttv.Res;
import bttv.settings.UserPreferences.Entry;
import tv.twitch.android.models.settings.SettingsDestination;

public enum Settings {
    BTTVEmotesEnabled(
            new Entry.BoolEntry(
                "enable_bttv_emotes",
                new Entry.BoolValue(true),
                Res.strings.bttv_settings_enable_bttv_emotes_primary,
                Res.strings.bttv_settings_enable_bttv_emotes_secondary,
                null
            )
    ),
    GifRenderMode(
            new Entry.DropDownEntry(
                    "bttv_gif_render_mode",
                    new Entry.DropDownValue(
                            new ArrayList<>(Arrays.asList(
                                    Res.strings.bttv_settings_gif_render_mode_animate_forever,
                                    Res.strings.bttv_settings_gif_render_mode_animate,
                                    Res.strings.bttv_settings_gif_render_mode_static,
                                    Res.strings.bttv_settings_gif_render_mode_disabled
                            )),
                            Res.strings.bttv_settings_gif_render_mode_animate
                    ),
                    Res.strings.bttv_settings_gif_render_mode_title,
                    Res.strings.bttv_settings_gif_render_mode_descr,
                    null
            )
    ),
    FFZEmotesEnabled(
            new Entry.BoolEntry(
                "enable_ffz_emotes",
                new Entry.BoolValue(true),
                Res.strings.bttv_settings_enable_ffz_emotes_primary,
                null,
                null
            )
    ),
    SevenTVEmotesEnabled(
            new Entry.BoolEntry(
                "enable_7tv_emotes",
                new Entry.BoolValue(true),
                Res.strings.bttv_settings_enable_7tv_emotes_primary,
                null,
                null
            )
    ),
    AutoRedeemChannelPointsEnabled(
            new Entry.BoolEntry(
                    "enable_auto_redeem_channel_points",
                    new Entry.BoolValue(true),
                    Res.strings.bttv_settings_enable_auto_redeem_points_primary,
                    null,
                    null
            )
    ),
    ShowDeletedMessagesEnabled(
            new Entry.BoolEntry(
                    "enable_show_deleted_messages",
                    new Entry.BoolValue(false),
                    Res.strings.bttv_settings_enable_show_deleted_messages,
                    null,
                    null
            )
    ),
    SplitChatEnabled(
            new Entry.BoolEntry(
                    "enable_split_chat",
                    new Entry.BoolValue(false),
                    Res.strings.bttv_settings_enable_split_chat,
                    Res.strings.bttv_settings_enable_split_chat_descr,
                    null
            )
    ),
    ShouldShowSleepTimer(
            new Entry.BoolEntry(
                "enable_sleep_timer",
                new Entry.BoolValue(true),
                Res.strings.bttv_settings_show_sleep_timer_primary,
                null,
                null
            )
    ),
    AnonChatEnabled(
            new Entry.BoolEntry(
                    "enable_anon_chat",
                    new Entry.BoolValue(false),
                    Res.strings.bttv_settings_enable_anon_chat_primary,
                    Res.strings.bttv_settings_enable_anon_chat_secondary,
                    null
            )
    ),
    OpenHighlightedWordsButton(
            new Entry.LinkEntry(
                Res.strings.bttv_settings_open_highlights_dia_primary,
                Res.strings.bttv_settings_open_highlights_dia_secondary,
                null,
                SettingsDestination.BTTV_HIGHLIGHTS
            )
    ),
    HighlightedKeyWords(
            new Entry.StringSetEntry(
                    "bttv_highlight_set",
                    new HashSet<>(),
                    null,
                    null,
                    null
            )
    ),
    OpenBlacklistButton(
            new Entry.LinkEntry(
                    Res.strings.bttv_settings_open_blacklist_dia_primary,
                    Res.strings.bttv_settings_open_blacklist_dia_secondary,
                    null,
                    SettingsDestination.BTTV_BLACKLIST
            )
    ),
    BlacklistedKeyWords(
            new Entry.StringSetEntry(
                    "bttv_blacklist_set",
                    new HashSet<>(),
                    null,
                    null,
                    null
            )
    ),
    DevToolsEnabled(
            new Entry.BoolEntry(
                    "bttv_enable_dev_tools",
                    new Entry.BoolValue(false),
                    Res.strings.bttv_settings_enable_dev_tools,
                    Res.strings.bttv_settings_enable_dev_tools_secondary,
                    null
            )
    ),
    OpenCreditsButton(
            new Entry.LinkEntry(
                    Res.strings.bttv_settings_open_credits_button_title,
                    null,
                    null,
                    SettingsDestination.BTTV_CREDITS
            )
    );

    public final Entry entry;
    Settings(Entry entry) {
        this.entry = entry;
    }

    private static final Map<String, Settings> keyMap;
    static {
        keyMap = new HashMap<>();
        for (Settings setting : values()) {
            keyMap.put(setting.entry.key, setting);
        }
    }

    public static Settings get(String key) {
        return keyMap.get(key);
    }

    @Override
    public String toString() {
        return "Settings(" + this.entry + ")";
    }
}
