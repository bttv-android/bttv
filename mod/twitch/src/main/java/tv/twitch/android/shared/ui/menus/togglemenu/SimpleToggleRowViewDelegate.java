package tv.twitch.android.shared.ui.menus.togglemenu;

import android.view.View;

import tv.twitch.android.core.mvp.viewdelegate.RxViewDelegate;
import tv.twitch.android.core.mvp.viewdelegate.ViewDelegateEvent;
import tv.twitch.android.core.mvp.viewdelegate.ViewDelegateState;

public class SimpleToggleRowViewDelegate extends RxViewDelegate<SimpleToggleRowViewDelegate.ToggleState, SimpleToggleRowViewDelegate.ToggleSwitched> {
    public SimpleToggleRowViewDelegate(View view, int titleRes) {
        super();
    }

    public static final class ToggleState implements ViewDelegateState {
        public ToggleState(boolean z) {}
    }

    public static final class ToggleSwitched implements ViewDelegateEvent {
        public final boolean isToggled() {
            return false;
        }
    }

    public void render(ToggleState toggleState) {
    }
}
