package bttv;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

/** Chat mode is frozen before the application-scoped native SDK can be built. */
public final class AnonChat {
    public static final int ANON_VIEWER_ID = 0;
    private static final String KEY = "enable_anon_chat";
    private static volatile boolean initialized;
    private static boolean appliedGuest = true; // Unavailable preferences must not enable authenticated chat.
    private static boolean initializationFailed;
    private static SharedPreferences preferences;
    private static Context context;

    /** Legacy binary compatibility only; no controller is retained or invoked. */
    public interface Controller {
        void setAnonymous(boolean anonymous);
    }

    /** Legacy flag is not evidence of an established server identity. */
    public static boolean isAnonRightNow;

    private AnonChat() {
    }

    /** Called by ilg.j before hgc (and its native chat providers) can be constructed. */
    public static synchronized void initialize(Context application) {
        if (initialized) return;
        try {
            preferences = application.getSharedPreferences("BTTV", 0);
            appliedGuest = preferences.getBoolean(KEY, false);
            context = application;
        } catch (RuntimeException unavailable) {
            initializationFailed = true;
            appliedGuest = true;
        }
        initialized = true;
    }

    public static boolean isAppliedAnonymous() {
        return !initialized || appliedGuest;
    }

    public static boolean isRestartPending() {
        return initialized && !initializationFailed && preferences.getBoolean(KEY, false) != appliedGuest;
    }

    public static String status() {
        if (!initialized || initializationFailed) {
            return "Chat session not verified: startup setting unavailable. Sending disabled. Restart Twitch.";
        }
        String applied = appliedGuest ? "Guest mode applied; sending disabled."
                : "Native account mode applied.";
        return applied + " Connection identity not verified. " + (isRestartPending()
                ? "Restart pending: completely close and restart Twitch to apply Anonymous Chat."
                : "Changes require a complete app restart.");
    }

    /** Settings already persisted the desired value. This never contacts a native chat owner. */
    public static void reconnect(boolean ignored) {
        if (context != null) Toast.makeText(context, isRestartPending()
                ? "Restart pending: fully stop Twitch, then reopen. Current chat mode is unchanged."
                : "Current chat mode is unchanged. No restart is pending.", Toast.LENGTH_LONG).show();
    }

    /** Preserves every stock visibility restriction and adds the anonymous-mode gate. */
    public static boolean allowComposer(boolean twitchVisible) {
        return twitchVisible && !isAppliedAnonymous();
    }

    public static boolean attach(Controller ignored) {
        return isAppliedAnonymous();
    }

    public static int getViewerId(int viewerId) {
        return isAppliedAnonymous() ? ANON_VIEWER_ID : viewerId;
    }

    /** Legacy binary-compatible entry point; Twitch 31 attaches through {@link #attach}. */
    public static void setChatViewPresenter(Object ignored) {
    }

}
