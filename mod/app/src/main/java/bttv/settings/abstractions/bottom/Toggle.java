package bttv.settings.abstractions.bottom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;
import bttv.settings.UserPreferences;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import tv.twitch.android.shared.ui.menus.togglemenu.SimpleToggleRowViewDelegate;

public class Toggle {
    public static SimpleToggleRowViewDelegate fromSetting(Context ctx, ViewGroup container, Settings setting) {
        if (!(setting.entry instanceof UserPreferences.Entry.BoolEntry)) {
            throw new IllegalArgumentException("setting's entry is not a BoolEntry: " + setting);
        }
        UserPreferences.Entry.BoolEntry entry = (UserPreferences.Entry.BoolEntry) setting.entry;

        int toggleItemRes = ResUtil.getResourceId(Res.layouts.toggle_row_item);
        int textRes = ResUtil.getResourceId(entry.primaryTextResource);
        View view = LayoutInflater.from(ctx).inflate(toggleItemRes, container, false);
        SimpleToggleRowViewDelegate toggle = new SimpleToggleRowViewDelegate(view, textRes);
        toggle.eventObserver()
                .observeOn(Schedulers.computation())
                .doOnNext(new Consumer<SimpleToggleRowViewDelegate.ToggleSwitched>() {
                    @Override
                    public void accept(SimpleToggleRowViewDelegate.ToggleSwitched toggleSwitched) throws Exception {
                        Log.i("LBTTVToggle", "accept: " + toggleSwitched);
                        entry.set(
                            ctx,
                            new UserPreferences.Entry.BoolValue(toggleSwitched.isToggled())
                        );
                    }
                })
                .subscribe();
        return toggle;
    }
}
