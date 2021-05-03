package bttv;

import javax.inject.Provider;

import android.util.Log;
import androidx.fragment.app.FragmentActivity;
import tv.twitch.android.app.core.dagger.modules.ActivityModule;
import tv.twitch.android.app.core.dagger.modules.ActivityModule_ProvideMenuAdapterBinderFactory;
import tv.twitch.android.settings.base.SettingsTracker;
import tv.twitch.android.settings.dagger.SettingsActivityModule;
import tv.twitch.android.settings.dagger.SettingsActivityModule_ProvideSettingsTrackerFactory;
import tv.twitch.android.shared.analytics.PageViewTracker;
import tv.twitch.android.shared.ui.menus.core.MenuAdapterBinder;

public class SettingsFragmentBindingFactory implements SettingsFragmentBinding.Factory {

    private static Provider<FragmentActivity> fragmentActivityProvider;
    private static Provider<PageViewTracker> pageViewTrackerProvider;
    private static ActivityModule activityModule;
    private static SettingsActivityModule settingsActivityModule;

    public static void setFragmentActivityProvider(Provider<FragmentActivity> fragmentActivityProvider) {
        SettingsFragmentBindingFactory.fragmentActivityProvider = fragmentActivityProvider;
        Log.d("LBTTVDI", "got fragmentActivityProvider");
    }

    public static void setActivityModule(ActivityModule activityModule) {
        SettingsFragmentBindingFactory.activityModule = activityModule;
        Log.d("LBTTVDI", "got activityModule");
    }

    public static void setSettingsActivityModule(SettingsActivityModule activityModule) {
        SettingsFragmentBindingFactory.settingsActivityModule = activityModule;
        Log.d("LBTTVDI", "got settingsActivityModule");
    }

    public static void setPageViewTrackerProvider(Provider<PageViewTracker> pageViewTrackerProvider) {
        SettingsFragmentBindingFactory.pageViewTrackerProvider = pageViewTrackerProvider;
        Log.d("LBTTVDI", "got pageViewTrackerProvider");
    }

    private static MenuAdapterBinder getMenuAdapterBinder() {
        return ActivityModule_ProvideMenuAdapterBinderFactory.provideMenuAdapterBinder(
                SettingsFragmentBindingFactory.activityModule,
                SettingsFragmentBindingFactory.fragmentActivityProvider.get());
    }

    private static SettingsTracker getTracker() {
        return SettingsActivityModule_ProvideSettingsTrackerFactory.provideSettingsTracker(
                SettingsFragmentBindingFactory.settingsActivityModule, pageViewTrackerProvider.get());
    }

    @Override
    public SettingsFragmentBinding create(SettingsFragment fragment) {
        if (fragmentActivityProvider == null) {
            Log.e("LBTTVDI", "fragmentActivityProvider null");
            return null;
        }
        if (activityModule == null) {
            Log.e("LBTTVDI", "activityModule null");
            return null;
        }
        if (settingsActivityModule == null) {
            Log.e("LBTTVDI", "settingsActivityModule null");
            return null;
        }
        if (pageViewTrackerProvider == null) {
            Log.e("LBTTVDI", "pageViewTrackerProvider null");
            return null;
        }

        FragmentActivity activity = (FragmentActivity) fragmentActivityProvider.get();

        return new SettingsFragmentBindingImpl(fragment, activity, getMenuAdapterBinder(), getTracker());
    }

}
