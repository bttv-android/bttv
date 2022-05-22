package tv.twitch.android.shared.ui.menus;

import tv.twitch.android.shared.ui.menus.togglemenu.ToggleMenuModel;

public interface SettingsPreferencesController {
    void updatePreferenceBooleanState(ToggleMenuModel toggleMenuModel, boolean z);

    public enum SettingsPreference {
        BTTVEmotesEnabled,
    }

}
