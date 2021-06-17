package bttv.settings.abstractions;

import android.content.Context;

import bttv.Util;
import bttv.settings.UserPreferences;
import kotlin.jvm.internal.DefaultConstructorMarker;
import tv.twitch.android.shared.ui.menus.SettingsPreferencesController;
import tv.twitch.android.shared.ui.menus.togglemenu.ToggleMenuModel;

public class Switch {
    public static ToggleMenuModel simple (Context ctx, String primaryText, String secondaryText, String auxiliaryText, String key, boolean defaultValue) {
        return new ToggleMenuModel(
                primaryText,
                secondaryText,
                auxiliaryText,
                UserPreferences.getBoolean(ctx, key, defaultValue),
                false,
                null,
                key,
                false,
                null,
                null,
                null,
                SettingsPreferencesController.SettingsPreference.BTTVEmotesEnabled,
                null,
                0b1011110110100,
                null
        );
    }

    public static ToggleMenuModel fromEntry(Context ctx, UserPreferences.Entry.BoolEntry entry) {
        return Switch.simple(
                ctx,
                Util.getLocaleString(ctx, entry.primaryTextResource),
                Util.getLocaleString(ctx, entry.secondaryTextResource),
                Util.getLocaleString(ctx, entry.auxiliaryTextResource),
                entry.key,
                ((UserPreferences.Entry.BoolValue) entry.defaultValue).get()
        );
    }
}
