package tv.twitch.android.settings.base;

import java.util.List;

import androidx.fragment.app.FragmentActivity;
import tv.twitch.android.shared.ui.menus.SettingsPreferencesController;
import tv.twitch.android.shared.ui.menus.core.MenuAdapterBinder;
import tv.twitch.android.shared.ui.menus.core.MenuModel;

public class BaseSettingsPresenter {

    public BaseSettingsPresenter(FragmentActivity fragmentActivity, MenuAdapterBinder menuAdapterBinder,
            SettingsTracker settingsTracker) {
    }

    public void onActive() {
    }

    public String getToolbarTitle() {
        return "";
    }

    public SettingsPreferencesController getPrefController() {
        return null;
    }

    public void updateSettingModels() {

    }

    public List<MenuModel> getSettingModels() {
        return null;
    }
}
