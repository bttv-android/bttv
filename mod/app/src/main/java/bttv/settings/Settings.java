package bttv.settings;

import java.util.HashMap;
import java.util.Map;

import bttv.settings.UserPreferences.Entry;

public enum Settings {
    BTTVEmotesEnabled(
            new Entry.BoolEntry(
                "enable_bttv_emotes",
                new Entry.BoolValue(true),
                "bttv_settings_enable_bttv_emotes_primary",
                "bttv_settings_enable_bttv_emotes_secondary",
                null
            )
    ),
    BTTVGifEmotesEnabled(
            new Entry.BoolEntry(
                "enable_bttv_gif_emotes",
                new Entry.BoolValue(true),
                "bttv_settings_enable_gif_emotes_primary",
                null,
                null
            )
    ),
    FFZEmotesEnabled(
            new Entry.BoolEntry(
                "enable_ffz_emotes",
                new Entry.BoolValue(true),
                "bttv_settings_enable_ffz_emotes_primary",
                null,
                null
            )
    ),
    SevenTVEmotesEnabled(
            new Entry.BoolEntry(
                "enable_7tv_emotes",
                new Entry.BoolValue(true),
                "bttv_settings_enable_7tv_emotes_primary",
                null,
                null
            )
    ),
    AutoRedeemChannelPointsEnabled(
            new Entry.BoolEntry(
                "enable_auto_redeem_channel_points",
                new Entry.BoolValue(true),
                "bttv_settings_enable_auto_redeem_points_primary",
                null,
                null
            )
    ),
    ShouldShowSleepTimer(
            new Entry.BoolEntry(
                "enable_sleep_timer",
                new Entry.BoolValue(true),
                "bttv_settings_show_sleep_timer_primary",
                null,
                null
            )
    );

    public final Entry.BoolEntry entry;
    Settings(Entry.BoolEntry entry) {
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
