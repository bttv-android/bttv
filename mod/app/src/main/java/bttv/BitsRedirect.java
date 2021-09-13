package bttv;

import android.app.Activity;

import tv.twitch.android.shared.bits.BitsSpendingPresenter;

public class BitsRedirect {
    public static void openBitsPage(BitsSpendingPresenter presenter) {
        Activity activity = presenter.activity;
        Util.launchBrowser(activity, "https://twitch.tv/bits/");
    }
}
