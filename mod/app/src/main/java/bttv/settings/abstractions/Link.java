package bttv.settings.abstractions;

import android.content.Context;

import bttv.ResUtil;
import bttv.settings.UserPreferences;
import tv.twitch.android.shared.ui.menus.subscription.SubMenuModel;

public class Link {
    public static SubMenuModel fromEntry(Context context, UserPreferences.Entry.LinkEntry entry) {
        return new SubMenuModel(
                ResUtil.getLocaleString(context, entry.primaryTextResource),
                ResUtil.getLocaleString(context, entry.secondaryTextResource),
                ResUtil.getLocaleString(context, entry.auxiliaryTextResource),
                entry.destination,
                true
        );
    }
}
