package bttv.settings.abstractions;

import android.content.Context;

import bttv.Util;
import bttv.settings.UserPreferences;
import tv.twitch.android.shared.ui.menus.subscription.SubMenuModel;

public class Link {
    public static SubMenuModel fromEntry(Context context, UserPreferences.Entry.LinkEntry entry) {
        return new SubMenuModel(
                Util.getLocaleString(context, entry.primaryTextResource),
                Util.getLocaleString(context, entry.secondaryTextResource),
                Util.getLocaleString(context, entry.auxiliaryTextResource),
                entry.destination,
                true
        );
    }
}
