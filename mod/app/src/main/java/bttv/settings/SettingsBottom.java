package bttv.settings;

import static bttv.ResUtil.getBooleanFromSettings;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import bttv.AnonChat;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.abstractions.bottom.Toggle;
import io.reactivex.functions.Consumer;
import tv.twitch.android.core.mvp.viewdelegate.BaseViewDelegate;
import tv.twitch.android.shared.ui.menus.togglemenu.SimpleToggleRowViewDelegate;

public class SettingsBottom {
    private static final String TAG = "LBTTVSettingsBottom";
    private static Pair<BaseViewDelegate, Settings>[] LAST_ITEMS = null;

    public static void fillSettingsContainer(BaseViewDelegate chatSettingsViewDelegate) {
        ViewGroup container = getContainer(chatSettingsViewDelegate);
        Pair<BaseViewDelegate, Settings>[] items = getItems(chatSettingsViewDelegate.getContext(), container);

        for (Pair<BaseViewDelegate, Settings> pair : items) {
            BaseViewDelegate itemDelegate = pair.first;
            container.addView(itemDelegate.getContentView());
        }
    }

    public static void renderBTTV() {
        if (LAST_ITEMS == null) {
            throw new IllegalStateException("LAST_ITEMS is null");
        }
        for (Pair<BaseViewDelegate, Settings> pair : LAST_ITEMS) {
            BaseViewDelegate item = pair.first;
            Settings setting = pair.second;
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

    private static Pair<BaseViewDelegate, Settings>[] getItems(Context ctx, ViewGroup container) {
        Pair<BaseViewDelegate, Settings>[] items = new Pair[]{
                new Pair<>(Toggle.fromSetting(ctx, container, Settings.BTTVEmotesEnabled), Settings.BTTVEmotesEnabled),
                new Pair<>(Toggle.fromSetting(ctx, container, Settings.FFZEmotesEnabled), Settings.FFZEmotesEnabled),
                new Pair<>(Toggle.fromSetting(ctx, container, Settings.SevenTVEmotesEnabled), Settings.SevenTVEmotesEnabled),
                new Pair<>(Toggle.fromSetting(ctx, container, Settings.AutoRedeemChannelPointsEnabled), Settings.AutoRedeemChannelPointsEnabled),
                new Pair<>(Toggle.fromSetting(ctx, container, Settings.ShowDeletedMessagesEnabled), Settings.ShowDeletedMessagesEnabled),
                getAnonToggle(ctx, container),
        };

        LAST_ITEMS = items;
        return items;
    }

    private static Pair<BaseViewDelegate, Settings> getAnonToggle(Context ctx, ViewGroup container) {
        return new Pair<>(Toggle.fromSetting(ctx, container, Settings.AnonChatEnabled, new Consumer<SimpleToggleRowViewDelegate.ToggleSwitched>() {
            @Override
            public void accept(SimpleToggleRowViewDelegate.ToggleSwitched toggleSwitched) {
                AnonChat.reconnect(toggleSwitched.isToggled());
            }
        }), Settings.AnonChatEnabled);
    }
}
