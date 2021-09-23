package tv.twitch.android.shared.ui.menus.togglemenu;

import android.graphics.drawable.Drawable;
import android.view.View;
import kotlin.jvm.internal.BTTVDefaultConstructorMarker;
import tv.twitch.android.shared.ui.menus.SettingsPreferencesController;
import tv.twitch.android.shared.ui.menus.formvalue.FormValueDelegate;

public class ToggleMenuModel extends FormValueDelegate<Boolean> {

    public ToggleMenuModel(String primaryText, String secondaryText, String auxiliaryText, boolean z, boolean z2,
                           Drawable icon, String eventName, boolean includeBackground, String pillText, Integer pillColor,
                           Integer pillTextColor, SettingsPreferencesController.SettingsPreference settingsPreference,
                           View.OnClickListener titleClickListener, int i, BTTVDefaultConstructorMarker BTTVDefaultConstructorMarker) {
    }

    public final String getEventName() {
        return null;
    }

}
