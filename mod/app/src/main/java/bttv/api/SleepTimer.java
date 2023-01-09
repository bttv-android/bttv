package bttv.api;

import android.content.Context;
import android.util.Log;
import android.view.View;

import tv.twitch.android.shared.player.overlay.RxBottomPlayerControlOverlayViewDelegate;

public class SleepTimer {
    /** used for VODs */
    public static void onInit(final Context context, final View view) {
        try {
            bttv.SleepTimer.onInit(context, view);
        } catch (Throwable e) {
            Log.e("LBTTVSleepTimer", "onInit failed: ", e);
        }
    }

    /** used for live */
    public static void onInit(final Context context, final RxBottomPlayerControlOverlayViewDelegate playerViewDelegate) {
        try {
            bttv.SleepTimer.onInit(context, playerViewDelegate.viewBinding.rootView);
        } catch (Throwable e) {
            Log.e("LBTTVSleepTimer", "onInit failed: ", e);
        }
    }

}
