package bttv.settings;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.view.Menu;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import bttv.Res;
import bttv.ResUtil;

/** Navigation shared by the two native main Settings row implementations. */
public final class SettingsNavigation {
    // Only final native a120 models are registered; they use Object identity.
    // Boolean values cannot retain an Activity, presenter, or their weak key.
    private static final Map<Object, Boolean> nativeRows = new WeakHashMap<>();

    private SettingsNavigation() {
    }

    public static Activity findActivity(Context context) {
        try {
            Set<Context> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            while (context != null && seen.add(context)) {
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    return activity.isFinishing() || activity.isDestroyed() ? null : activity;
                }
                if (!(context instanceof ContextWrapper)) {
                    return null;
                }
                context = ((ContextWrapper) context).getBaseContext();
            }
        } catch (Throwable ignored) {
            // Invalid/detached contexts must not launch a different task.
        }
        return null;
    }

    public static boolean open(Context context) {
        Activity activity = findActivity(context);
        if (activity == null) {
            return false;
        }
        try {
            activity.startActivity(new Intent(activity, SettingsActivity.class));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String nativeEntryTitle(Context context) {
        return ResUtil.getLocaleString(context, Res.strings.bttv_settings_entry_title);
    }

    public static synchronized void markNativeRow(Object row) {
        if (row != null) {
            nativeRows.put(row, Boolean.TRUE);
        }
    }

    public static synchronized boolean isNativeRow(Object row) {
        return row != null && nativeRows.containsKey(row);
    }

    public static synchronized boolean hasNativeRow(List<?> rows) {
        if (rows == null) {
            return true;
        }
        for (Object row : rows) {
            if (isNativeRow(row)) {
                return true;
            }
        }
        return false;
    }

    public static void install(final Activity activity, Menu menu) {
        // Compatibility entry point for the existing settings-Activity patch.
        // The native main Settings row is now the sole BTTV entry. Do not add
        // or remove any native toolbar items here.
    }
}
