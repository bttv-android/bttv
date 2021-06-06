package bttv;

import android.util.Log;
import android.view.View.OnClickListener;

import bttv.settings.UserPreferences;

public class ChannelPoints {
    public static void autoRedeem(OnClickListener onClickListener) {
        if (UserPreferences.getAutoRedeemChannelPointsEnabled(null)) {
            onClickListener.onClick(null);
        }
    }
}
