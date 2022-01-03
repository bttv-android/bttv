package bttv.api;

import android.util.Log;
import android.view.View;

public class StreamSettings {
    private static final String TAG = "LBTTVStreamSettings";

    // called in
    public static void setupBTTVStreamSettings(View view) {
      try {
        Log.i(TAG, "setupBttvStreamSettings()");
        bttv.settings.StreamSettings.setupBTTVStreamSettings(view);
      } catch (Throwable t) {
        Log.e(TAG, "setupBttvStreamSettings: ", t);
      }
    }
}
