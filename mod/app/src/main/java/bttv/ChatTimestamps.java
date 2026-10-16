package bttv;

import bttv.settings.Settings;

/** Gates only Twitch's native regular-message timestamp presentation flag. */
public final class ChatTimestamps {
    private ChatTimestamps() {}

    public static boolean shouldShow(boolean stockVisible) {
        if (stockVisible) {
            return true;
        }
        try {
            return ResUtil.getBooleanFromSettings(Settings.AlwaysShowChatTimestamps);
        } catch (Throwable ignored) {
            return stockVisible;
        }
    }
}
