package bttv.settings;

import android.util.Log;
import androidx.fragment.app.FragmentActivity;
import tv.twitch.android.shared.settings.SettingsTracker;
import tv.twitch.android.shared.ui.menus.core.MenuAdapterBinder;

public class SettingsFragmentBindingFactory implements SettingsFragmentBinding.Factory {

    private static FragmentActivity fragmentActivity; // TODO ;fragmentActivityProvider.get();
    private static MenuAdapterBinder menuAdapterBinder; // TODO;
    private static SettingsTracker settingsTracker; // TODO;

    /** @noinspection unused */
    public static void setFragmentActivity(Object fragmentActivity) {
        Log.d("LBTTVDI", "got fragmentActivity: " + fragmentActivity.getClass().getName());
        SettingsFragmentBindingFactory.fragmentActivity = (FragmentActivity) fragmentActivity;
    }

    /** @noinspection unused */
    public static void setMenuAdapterBinder(MenuAdapterBinder menuAdapterBinder) {
        SettingsFragmentBindingFactory.menuAdapterBinder = menuAdapterBinder;
        Log.d("LBTTVDI", "got menuAdapterBinder");
    }

    /** @noinspection unused */
    public static void setSettingsTracker(SettingsTracker settingsTracker) {
        SettingsFragmentBindingFactory.settingsTracker = settingsTracker;
        Log.d("LBTTVDI", "got settingsTracker");
    }

    @Override
    public SettingsFragmentBinding create(SettingsFragment fragment) {
        if (fragmentActivity == null) {
            Log.e("LBTTV", "SettingsFragmentBinding.create will fail: fragmentActivity == null");
        }
        if (menuAdapterBinder == null) {
            Log.e("LBTTV", "SettingsFragmentBinding.create will fail: menuAdapterBinder == null");
        }
        if (settingsTracker == null) {
            Log.e("LBTTV", "SettingsFragmentBinding.create will fail: settingsTracker == null");
        }
        return new SettingsFragmentBindingImpl(fragment, fragmentActivity, menuAdapterBinder, settingsTracker);
    }

}
