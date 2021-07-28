package bttv.api;

import android.content.Context;
import android.util.Log;
import android.view.View;

public class SleepTimer {
    public static void onInit(final Context context, final View view) {
        try {
            bttv.SleepTimer.onInit(context, view);
        } catch (Throwable e) {
            Log.e("LBTTVSleepTimer", "onInit failed: ", e);
        }
    }
}
