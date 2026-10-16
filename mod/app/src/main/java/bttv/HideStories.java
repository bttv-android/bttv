package bttv;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

import bttv.settings.Settings;
import bttvcompat.NativeStoriesBridge;

/** Filters only the verified native Home Stories presentation flag. */
public final class HideStories {
    // Weak owners prevent this setting from retaining destroyed Home delegates.
    // Values are the original immutable w1e state, never an altered copy.
    private static final Map<Object, Object> states = new WeakHashMap<>();

    private HideStories() {}

    public static boolean onBind(Object owner, Object originalState, boolean stockHasStories) {
        try {
            if (owner != null && originalState != null) {
                synchronized (states) {
                    states.put(owner, originalState);
                }
            }
            return stockHasStories
                    && !ResUtil.getBooleanFromSettings(Settings.HideTwitchStoriesEnabled);
        } catch (Throwable ignored) {
            return stockHasStories;
        }
    }

    /** Called on the settings UI thread, after the existing preference is saved. */
    public static void onSettingChanged() {
        ArrayList<Object> owners;
        synchronized (states) {
            owners = new ArrayList<>(states.keySet());
        }
        for (Object owner : owners) {
            try {
                Object state;
                synchronized (states) {
                    state = states.get(owner);
                }
                if (state != null) {
                    NativeStoriesBridge.rebind(owner, state);
                }
            } catch (Throwable ignored) {
                // Preserve the saved setting; a later native bind reevaluates it.
            }
        }
    }
}
