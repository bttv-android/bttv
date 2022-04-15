package tv.twitch.android.shared.ui.menus.infomenu;

import android.graphics.drawable.Drawable;
import android.view.View;

import tv.twitch.android.core.mvp.viewdelegate.RxViewDelegate;
import tv.twitch.android.core.mvp.viewdelegate.ViewDelegateEvent;
import tv.twitch.android.core.mvp.viewdelegate.ViewDelegateState;

public class InfoMenuViewDelegate extends RxViewDelegate<InfoMenuViewDelegate.State, InfoMenuViewDelegate.Event> {
    public static final class State implements ViewDelegateState {
        public State(String title, Drawable icon, String description) { }
    }
    public static abstract class Event implements ViewDelegateEvent {
        public static final class InfoMenuItemClicked extends Event { }
    }

    public void render(InfoMenuViewDelegate.State state) { }

    public InfoMenuViewDelegate(View root) { }
}
