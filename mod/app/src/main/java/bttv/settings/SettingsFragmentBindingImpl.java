package bttv.settings;

import androidx.fragment.app.FragmentActivity;
import tv.twitch.android.settings.base.SettingsTracker;
import tv.twitch.android.shared.ui.menus.core.MenuAdapterBinder;

public class SettingsFragmentBindingImpl implements SettingsFragmentBinding {

    FragmentActivity settingsActivity;
    MenuAdapterBinder menuAdapterBinder;
    SettingsTracker settingsTracker;

    public SettingsFragmentBindingImpl(SettingsFragment fragment, FragmentActivity activity,
            MenuAdapterBinder menuAdapterBinder, SettingsTracker settingsTracker) {
        this.settingsActivity = activity;
        this.menuAdapterBinder = menuAdapterBinder;
        this.settingsTracker = settingsTracker;
    }

    private SettingsPresenter getPresenter() {
        return new SettingsPresenter(settingsActivity, menuAdapterBinder, settingsTracker);
    }

    @Override
    public void inject(SettingsFragment fragment) {
        fragment.presenter = getPresenter();
    }
}
