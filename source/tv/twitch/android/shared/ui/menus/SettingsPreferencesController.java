package tv.twitch.android.shared.ui.menus;

import tv.twitch.android.shared.ui.menus.checkable.CheckableGroupModel;
import tv.twitch.android.shared.ui.menus.togglemenu.ToggleMenuModel;

public interface SettingsPreferencesController {
    void updatePreferenceBooleanState(ToggleMenuModel toggleMenuModel, boolean z);

    void updatePreferenceCheckedState(CheckableGroupModel checkableGroupModel);

    public enum SettingsPreference {
        BTTVEmotesEnabled,
    }

}
