package bttv;

import java.util.List;

import javax.inject.Inject;

import androidx.fragment.app.FragmentActivity;
import tv.twitch.android.settings.base.BaseSettingsPresenter;
import tv.twitch.android.settings.base.SettingsTracker;
import tv.twitch.android.shared.ui.menus.SettingsPreferencesController;
import tv.twitch.android.shared.ui.menus.core.MenuAdapterBinder;
import tv.twitch.android.shared.ui.menus.core.MenuModel;
import tv.twitch.android.shared.ui.menus.togglemenu.ToggleMenuModel;

public final class SettingsPresenter extends BaseSettingsPresenter {

        @Inject
        public SettingsPresenter(FragmentActivity fragmentActivity, MenuAdapterBinder menuAdapterBinder,
                        SettingsTracker settingsTracker) {
                super(fragmentActivity, menuAdapterBinder, settingsTracker);
        }

        @Override
        public void onActive() {
                super.onActive();
                // TODO: load settings from SP
        }

        @Override
        public String getToolbarTitle() {
                return "BTTV";
        }

        @Override
        public void updateSettingModels() {
                List<MenuModel> settingsModels = getSettingModels();
                settingsModels.clear();
                boolean enableBTTVEmoteDefaultValue = true; // TODO
                settingsModels.add(new ToggleMenuModel("Enable BTTV Emotes", "Why would you disable this?", null,
                                enableBTTVEmoteDefaultValue, false, null, null, false, null, null, null,
                                SettingsPreferencesController.SettingsPreference.BTTVEmotesEnabled, null,
                                0b1011111110100, null));

                // bindSettings();
        }
}
