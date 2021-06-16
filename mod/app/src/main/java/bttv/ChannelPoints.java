package bttv;

import android.view.View.OnClickListener;

import bttv.settings.Settings;
import bttv.settings.UserPreferences;

public class ChannelPoints {
    public static void autoRedeem(OnClickListener onClickListener) {
        if (Settings.AutoRedeemChannelPointsEnabled.entry.get(Data.ctx).get()) {
            onClickListener.onClick(null);
        }
    }
}
