package bttv.api;

import android.util.Log;

import tv.twitch.android.core.mvp.viewdelegate.BaseViewDelegate;

public class SettingsBottom {
    private static final String TAG = "LBTTVSettingsBottom";

    /** Called in tv.twitch.android.shared.chat.settings.entry.ChatSettingsViewDelegate's constructor */
    public static void fillSettingsContainer(BaseViewDelegate chatSettingsViewDelegate) {
        try {
            bttv.settings.SettingsBottom.fillSettingsContainer(chatSettingsViewDelegate);
        } catch (Throwable t) {
            Log.e(TAG, "fillSettingsContainer: ", t);
        }
    }

    /** Called in tv.twitch.android.shared.chat.settings.entry.ChatSettingsViewDelegate's constructor */
    public static void renderBTTV() {
        try {
            bttv.settings.SettingsBottom.renderBTTV();
        } catch (Throwable t) {
            Log.e(TAG, "renderBTTV: ", t);
        }
    }
}
