package bttv;

import javax.inject.Inject;
import android.util.Log;
import tv.twitch.android.settings.base.BaseSettingsFragment;
import tv.twitch.android.settings.base.BaseSettingsPresenter;

public class SettingsFragment extends BaseSettingsFragment {

    @Inject
    public SettingsPresenter presenter;

    @Override
    public BaseSettingsPresenter createPresenter() {
        if (this.presenter != null) {
            return this.presenter;
        }
        Log.e("BTTVSettingsFragment", "Presenter not initialized");
        throw null;
    }
}
