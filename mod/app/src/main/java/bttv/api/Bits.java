package bttv.api;

import android.util.Log;

import bttv.BitsRedirect;
import tv.twitch.android.shared.bits.BitsSpendingPresenter;

public class Bits {
    public static void onBuyBitsButtonClick(BitsSpendingPresenter bitsSpendingPresenter) {
        try {
            BitsRedirect.openBitsPage(bitsSpendingPresenter);
        } catch (Throwable t) {
            Log.e("LBTTVBits", "onBuyBitsButtonClick: ", t);
        }
    }
}
