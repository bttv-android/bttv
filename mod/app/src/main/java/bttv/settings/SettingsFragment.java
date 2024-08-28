package bttv.settings;

import javax.inject.Inject;
import android.util.Log;
import tv.twitch.android.shared.settings.BaseSettingsFragment;
import tv.twitch.android.shared.settings.BaseSettingsPresenter;

public class SettingsFragment extends BaseSettingsFragment {

    @Inject
    public SettingsPresenter presenter;

    @Override
    public BaseSettingsPresenter createPresenter() {
        if (this.presenter != null) {
            return this.presenter;
        }
        Log.e("LBTTVSettingsFragment", "Presenter not initialized");
        throw null;
    }
}
