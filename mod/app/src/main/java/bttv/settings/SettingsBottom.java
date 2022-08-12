package bttv.settings;

import static bttv.ResUtil.getBooleanFromSettings;
import static bttv.ResUtil.getLocaleString;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import bttv.AnonChat;
import bttv.Data;
import bttv.Res;
import bttv.ResUtil;
import bttv.highlight.Blacklist;
import bttv.highlight.Highlight;
import bttv.highlight.StringSetUI;
import bttv.settings.abstractions.bottom.Link;
import bttv.settings.abstractions.bottom.Toggle;
import io.reactivex.functions.Consumer;
import tv.twitch.android.core.mvp.viewdelegate.BaseViewDelegate;
import tv.twitch.android.shared.ui.menus.infomenu.InfoMenuViewDelegate;
import tv.twitch.android.shared.ui.menus.togglemenu.SimpleToggleRowViewDelegate;

class DelegateSettingsPair {
    BaseViewDelegate delegate;
    Settings settings;
    DelegateSettingsPair(BaseViewDelegate delegate, Settings settings) {
        this.delegate = delegate;
        this.settings = settings;
    }
}

public class SettingsBottom {
    private static final String TAG = "LBTTVSettingsBottom";
    private static DelegateSettingsPair[] LAST_ITEMS = null;

    public static void fillSettingsContainer(BaseViewDelegate chatSettingsViewDelegate) {
        ViewGroup container = getContainer(chatSettingsViewDelegate);
        DelegateSettingsPair[] items = getItems(chatSettingsViewDelegate.getContext(), container);

        for (DelegateSettingsPair pair : items) {
            container.addView(pair.delegate.getContentView());
        }
    }

    public static void renderBTTV() {
        if (LAST_ITEMS == null) {
            throw new IllegalStateException("LAST_ITEMS is null");
        }
        for (DelegateSettingsPair pair : LAST_ITEMS) {
            BaseViewDelegate item = pair.delegate;
            Settings setting = pair.settings;
            if (item instanceof SimpleToggleRowViewDelegate) {
                SimpleToggleRowViewDelegate castedItem = (SimpleToggleRowViewDelegate) item;
                castedItem.render(new SimpleToggleRowViewDelegate.ToggleState(getBooleanFromSettings(setting)));
            } else if (item instanceof InfoMenuViewDelegate) {
                Link.render((InfoMenuViewDelegate) item, setting);
            }
        }
    }

    private static ViewGroup getContainer(BaseViewDelegate chatSettingsViewDelegate) {
        Context cx = chatSettingsViewDelegate.getContext();
        View view = chatSettingsViewDelegate.getContentView();
        int id = ResUtil.getResourceId(cx, Res.ids.bttv_chat_settings_bs_container);
        return view.findViewById(id);
    }

    private static DelegateSettingsPair[] getItems(Context ctx, ViewGroup container) {
        DelegateSettingsPair[] items = new DelegateSettingsPair[]{
                new DelegateSettingsPair(Toggle.fromSetting(ctx, container, Settings.BTTVEmotesEnabled), Settings.BTTVEmotesEnabled),
                new DelegateSettingsPair(Toggle.fromSetting(ctx, container, Settings.FFZEmotesEnabled), Settings.FFZEmotesEnabled),
                new DelegateSettingsPair(Toggle.fromSetting(ctx, container, Settings.SevenTVEmotesEnabled), Settings.SevenTVEmotesEnabled),
                new DelegateSettingsPair(Toggle.fromSetting(ctx, container, Settings.AutoRedeemChannelPointsEnabled), Settings.AutoRedeemChannelPointsEnabled),
                new DelegateSettingsPair(Toggle.fromSetting(ctx, container, Settings.ShowDeletedMessagesEnabled), Settings.ShowDeletedMessagesEnabled),
                getAnonToggle(ctx, container),
                getOpenHighlightLink(ctx, container),
                getOpenBlacklistLink(ctx, container)
        };

        LAST_ITEMS = items;
        return items;
    }

    private static DelegateSettingsPair getAnonToggle(Context ctx, ViewGroup container) {
        return new DelegateSettingsPair(Toggle.fromSetting(ctx, container, Settings.AnonChatEnabled, new Consumer<SimpleToggleRowViewDelegate.ToggleSwitched>() {
            @Override
            public void accept(SimpleToggleRowViewDelegate.ToggleSwitched toggleSwitched) {
                AnonChat.reconnect(toggleSwitched.isToggled());
            }
        }), Settings.AnonChatEnabled);
    }

    private static DelegateSettingsPair getOpenStringSetLink(Context ctx, ViewGroup container, StringSetUI setUI, Settings setting) {
        Log.i(TAG, "getOpenStringSetLink: " + ctx);
        Consumer<InfoMenuViewDelegate.Event> onClick = new Consumer<InfoMenuViewDelegate.Event>() {
            @Override
            public void accept(InfoMenuViewDelegate.Event event) throws Exception {
                Log.d(TAG, "onClick: ");
                Activity activity = (Activity) ctx;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setUI.openDialog(activity);
                    }
                });
            }
        };
        return new DelegateSettingsPair(
                Link.simple(ctx, container, onClick),
                setting
        );
    }

    private static DelegateSettingsPair getOpenHighlightLink(Context ctx, ViewGroup container) {
        return getOpenStringSetLink(ctx, container, Highlight.getInstance(), Settings.OpenHighlightedWordsButton);
    }

    private static DelegateSettingsPair getOpenBlacklistLink(Context ctx, ViewGroup container) {
        return getOpenStringSetLink(ctx, container, Blacklist.getInstance(), Settings.OpenBlacklistButton);
    }
}
