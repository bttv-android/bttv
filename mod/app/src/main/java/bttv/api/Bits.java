package bttv.api;

import android.util.Log;

import bttv.BitsRedirect;
import tv.twitch.android.shared.bits.BitsSpendingPresenter;

public class Bits {
    private static final String TAG = "LBTTVBits";

    public static void onBuyBitsButtonClick(BitsSpendingPresenter bitsSpendingPresenter) {
        try {
            BitsRedirect.openBitsPage(bitsSpendingPresenter);
        } catch (Throwable t) {
            Log.e(TAG, "onBuyBitsButtonClick: ", t);
        }
    }
}
