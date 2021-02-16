package tv.twitch.android.shared.ui.menus.core;

import java.util.List;

import tv.twitch.android.core.adapters.ContentAdapterSection;
import tv.twitch.android.core.adapters.TwitchSectionAdapter;
import tv.twitch.android.core.mvp.lifecycle.VisibilityProvider;
import tv.twitch.android.shared.ui.menus.SettingActionListener;

public class MenuAdapterBinder {
    public final TwitchSectionAdapter getAdapter() {
        return null;
    }

    public final <T extends MenuModel> void bindModels(List<T> list, SettingActionListener settingActionListener,
            ContentAdapterSection contentAdapterSection, VisibilityProvider visibilityProvider) {
    }

}
