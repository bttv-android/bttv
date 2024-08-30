package tv.twitch.android.shared.settings;

import java.util.List;

import androidx.fragment.app.FragmentActivity;

import tv.twitch.android.shared.ui.menus.SettingActionListener;
import tv.twitch.android.shared.ui.menus.SettingsPreferencesController;
import tv.twitch.android.shared.ui.menus.core.MenuAdapterBinder;
import tv.twitch.android.shared.ui.menus.core.MenuModel;

public abstract class BaseSettingsPresenter {

    public abstract SettingsNavigationController getNavController();

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

    public void bindSettings() {

    }

    public final MenuAdapterBinder getAdapterBinder() {
        return null;
    }

    public final FragmentActivity getActivity() {
        return null;
    }

    public final SettingActionListener getMSettingActionListener() {
        return null;
    }
}
