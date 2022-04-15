package bttv.settings.abstractions.bottom;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    public static tv.twitch.android.shared.ui.menus.infomenu.InfoMenuViewDelegate simple(Context ctx, ViewGroup container, View.OnClickListener listener) {
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
                            listener.onClick(view);
                    }
                });
        return delegate;
    }
}
