package bttv;

import android.view.View.OnClickListener;

import bttv.settings.Settings;
import bttv.settings.UserPreferences;

public class ChannelPoints {
    public static void autoRedeem(OnClickListener onClickListener) {
        if (Util.getBooleanFromSettings(Settings.AutoRedeemChannelPointsEnabled)) {
            onClickListener.onClick(null);
        }
    }
}
