package bttv;

import android.view.View.OnClickListener;

import bttv.settings.Settings;

public class ChannelPoints {
    public static void autoRedeem(OnClickListener onClickListener) {
        if (ResUtil.getBooleanFromSettings(Settings.AutoRedeemChannelPointsEnabled)) {
            onClickListener.onClick(null);
        }
    }
}
