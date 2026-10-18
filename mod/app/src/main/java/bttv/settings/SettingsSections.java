package bttv.settings;

import bttv.Res;

/** Presentation only: preference definitions, keys and defaults stay in Settings. */
public enum SettingsSections {
    EMOTES(Res.strings.bttv_settings_section_emotes),
    CHAT(Res.strings.bttv_settings_section_chat),
    OTHER(Res.strings.bttv_settings_section_other),
    ABOUT(Res.strings.bttv_settings_section_about);

    public final Res.strings title;

    SettingsSections(Res.strings title) {
        this.title = title;
    }

    public static SettingsSections sectionOf(Settings setting) {
        switch (setting) {
            case BTTVEmotesEnabled:
            case FFZEmotesEnabled:
            case SevenTVEmotesEnabled:
            case GifRenderMode:
                return EMOTES;
            case ShowDeletedMessagesEnabled:
            case SplitChatEnabled:
            case AlwaysShowChatTimestamps:
            case AnonChatEnabled:
            case OpenHighlightedWordsButton:
            case OpenBlacklistButton:
                return CHAT;
            case AutoRedeemChannelPointsEnabled:
            case HideTwitchStoriesEnabled:
            case ShouldShowSleepTimer:
                return OTHER;
            case OpenCreditsButton:
            case InvokeUpdateCheck:
                return ABOUT;
            default:
                // Backing string sets, automatic updater preference and developer tools.
                return null;
        }
    }
}
