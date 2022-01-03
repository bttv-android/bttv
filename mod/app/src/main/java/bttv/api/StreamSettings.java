package bttv.api;

import android.util.Log;

import tv.twitch.android.core.mvp.viewdelegate.BaseViewDelegate;

public class StreamSettings {
    private static final String TAG = "LBTTVStreamSettings";

    // called in tv.twitch.android.feature.theatre.common.StreamSettingsViewDelegate.render()
    public static void setupBTTVStreamSettings(BaseViewDelegate viewDelegate) {
      try {
        Log.i(TAG, "setupBttvStreamSettings()");
        bttv.settings.StreamSettings.setupBTTVStreamSettings(viewDelegate.getContentView());
      } catch (Throwable t) {
        Log.e(TAG, "setupBttvStreamSettings: ", t);
      }
    }
}
