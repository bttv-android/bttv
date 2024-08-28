package tv.twitch.android.shared.settings;

import android.os.Bundle;

import tv.twitch.android.models.settings.SettingsDestination;

public interface SettingsNavigationController {
    void navigateToSettingFragment(SettingsDestination settingsDestination, Bundle bundle);
}
