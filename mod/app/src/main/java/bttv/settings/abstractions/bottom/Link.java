package bttv.settings.abstractions.bottom;

import static bttv.ResUtil.getLocaleString;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

import bttv.Data;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;
import bttv.settings.UserPreferences;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import tv.twitch.android.shared.ui.menus.infomenu.InfoMenuViewDelegate;

public class Link {
    public static InfoMenuViewDelegate simple(Context ctx, ViewGroup container, Consumer<InfoMenuViewDelegate.Event> listener) {
        int id = ResUtil.getResourceId(Res.layouts.value_row_item);
        View view = LayoutInflater.from(ctx).inflate(id, container, false);
        InfoMenuViewDelegate delegate = new InfoMenuViewDelegate(view);

        // FIXME: this causes a leak as it is never unsubscribed from
        Disposable handle = delegate.eventObserver()
                .observeOn(Schedulers.computation())
                .subscribe(new Consumer<InfoMenuViewDelegate.Event>() {
                    @Override
                    public void accept(InfoMenuViewDelegate.Event event) throws Exception {
                        if (listener != null)
                            listener.accept(event);
                    }
                });
        return delegate;
    }

    public static void render(InfoMenuViewDelegate item, Settings setting) {
        String primaryText = getLocaleString(Data.ctx, setting.entry.primaryTextResource);

        int iconRes = ResUtil.getResourceId(Data.ctx, Res.drawables.ic_arrow_right);
        Drawable icon = ContextCompat.getDrawable(Data.ctx, iconRes);

        String secondaryText = getLocaleString(Data.ctx, setting.entry.secondaryTextResource);

        item.render(new InfoMenuViewDelegate.State(primaryText, icon, secondaryText, false));
    }
}
