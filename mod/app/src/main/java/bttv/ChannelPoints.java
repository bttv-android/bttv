package bttv;

import android.view.View.OnClickListener;

import bttv.settings.UserPreferences;

public class ChannelPoints {
    public static void autoRedeem(OnClickListener onClickListener) {
        if (UserPreferences.getAutoRedeemChannelPointsEnabled(Data.ctx)) {
            onClickListener.onClick(null);
        }
    }
}
