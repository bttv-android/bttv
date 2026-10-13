package bttv;

import java.lang.ref.WeakReference;

import bttv.settings.Settings;

/** Coordinates Twitch's native chat identity without retaining a stale presenter. */
public final class AnonChat {
    public static final int ANON_VIEWER_ID = 0;

    public interface Controller {
        void setAnonymous(boolean anonymous);
    }

    private static WeakReference<Controller> controller = new WeakReference<>(null);
    private static Integer originalViewerID;
    public static boolean isAnonRightNow;

    private AnonChat() {
    }

    /** Attaches the newest Twitch chat owner and returns its required initial mode. */
    public static synchronized boolean attach(Controller value) {
        controller = new WeakReference<>(value);
        boolean enabled = isAnonChatEnabled();
        isAnonRightNow = enabled;
        return enabled;
    }

    /** Requests a native identity switch on the current chat owner, when one exists. */
    public static synchronized void reconnect(boolean asAnon) {
        Controller value = controller.get();
        if (value != null) {
            value.setAnonymous(asAnon);
        }
        isAnonRightNow = asAnon;
    }

    /** Preserves every stock visibility restriction and adds the anonymous-mode gate. */
    public static boolean allowComposer(boolean twitchVisible) {
        return twitchVisible && !isAnonChatEnabled();
    }

    /** Legacy compatibility for callers which still supply a numeric viewer ID. */
    public static synchronized int getViewerId(int viewerId) {
        if (viewerId != ANON_VIEWER_ID) {
            originalViewerID = viewerId;
        }
        if (isAnonChatEnabled()) {
            isAnonRightNow = true;
            return ANON_VIEWER_ID;
        }
        isAnonRightNow = false;
        return viewerId;
    }

    /** Legacy binary-compatible entry point; Twitch 31 attaches through {@link #attach}. */
    public static void setChatViewPresenter(Object ignored) {
    }

    private static boolean isAnonChatEnabled() {
        return ResUtil.getBooleanFromSettings(Settings.AnonChatEnabled);
    }
}
