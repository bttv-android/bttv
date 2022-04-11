package bttv.settings;

import static bttv.ResUtil.getBooleanFromSettings;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import bttv.Data;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.abstractions.bottom.Toggle;
import tv.twitch.android.core.mvp.viewdelegate.BaseViewDelegate;
import tv.twitch.android.shared.ui.menus.togglemenu.SimpleToggleRowViewDelegate;

public class SettingsBottom {
    private static final String TAG = "LBTTVSettingsBottom";
    private static BaseViewDelegate[] ITEMS = null;
    private static Settings[] SETTINGS = null;

    public static void fillSettingsContainer(BaseViewDelegate chatSettingsViewDelegate) {
        ViewGroup container = getContainer(chatSettingsViewDelegate);
        BaseViewDelegate[] items = getItems(chatSettingsViewDelegate.getContext(), container);

        for (BaseViewDelegate itemDelegate : items) {
            container.addView(itemDelegate.getContentView());
        }
    }

    public static void renderBTTV() {
        if (ITEMS == null) {
            throw new IllegalStateException("ITEMS is null");
        }
        for (int i = 0; i < ITEMS.length; i++) {
            BaseViewDelegate item = ITEMS[i];
            Settings setting = SETTINGS[i];
            if (item instanceof SimpleToggleRowViewDelegate) {
                SimpleToggleRowViewDelegate castedItem = (SimpleToggleRowViewDelegate) item;
                castedItem.render(new SimpleToggleRowViewDelegate.ToggleState(getBooleanFromSettings(setting)));
            }
        }
    }

    private static ViewGroup getContainer(BaseViewDelegate chatSettingsViewDelegate) {
        Context cx = chatSettingsViewDelegate.getContext();
        View view = chatSettingsViewDelegate.getContentView();
        int id = ResUtil.getResourceId(cx, Res.ids.bttv_chat_settings_bs_container);
        return view.findViewById(id);
    }

    private static BaseViewDelegate[] getItems(Context context, ViewGroup container) {
        BaseViewDelegate[] items = new BaseViewDelegate[] {
                Toggle.fromSetting(context, container, Settings.BTTVEmotesEnabled),
                Toggle.fromSetting(context, container, Settings.FFZEmotesEnabled),
                Toggle.fromSetting(context, container, Settings.SevenTVEmotesEnabled),
                Toggle.fromSetting(context, container, Settings.AutoRedeemChannelPointsEnabled),
                Toggle.fromSetting(context, container, Settings.ShowDeletedMessagesEnabled),
        };

        ITEMS = items;
        SETTINGS = new Settings[] {
                Settings.BTTVEmotesEnabled,
                Settings.FFZEmotesEnabled,
                Settings.SevenTVEmotesEnabled,
                Settings.AutoRedeemChannelPointsEnabled,
                Settings.ShowDeletedMessagesEnabled,
        };
        return items;
    }
}
