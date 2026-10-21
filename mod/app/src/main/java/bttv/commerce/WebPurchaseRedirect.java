package bttv.commerce;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.SystemClock;

import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

import bttv.ResUtil;
import bttv.settings.Settings;

/** Optional navigation only: never submits, authenticates or completes a purchase. */
public final class WebPurchaseRedirect {
    private static final WeakHashMap<Activity, Launch> launches = new WeakHashMap<>();

    private WebPurchaseRedirect() {}

    public static boolean tryBits(Activity activity) {
        try {
            return ResUtil.getBooleanFromSettings(Settings.WebBitsPurchasesEnabled)
                    && open(activity, "https://www.twitch.tv/bits");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean trySubscribe(Activity activity, String channelLogin) {
        try {
            if (!ResUtil.getBooleanFromSettings(Settings.WebSubscriptionsEnabled)
                    || !validLogin(channelLogin)) return false;
            return open(activity, "https://www.twitch.tv/subs/"
                    + channelLogin.toLowerCase(Locale.ROOT));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Explicit web navigation: deliberately independent of native redirect preferences. */
    public static boolean openChannelWeb(Activity activity, String channelLogin) {
        try {
            return validLogin(channelLogin) && open(activity, "https://www.twitch.tv/subs/"
                    + channelLogin.toLowerCase(Locale.ROOT));
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean validLogin(String login) {
        if (login == null || login.isEmpty() || login.length() > 25) return false;
        boolean nonNumeric = false;
        for (int i = 0; i < login.length(); i++) {
            char c = login.charAt(i);
            if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '_') {
                nonNumeric = true;
            } else if (c < '0' || c > '9') {
                return false;
            }
        }
        // Conservatively fall back rather than treating a numeric TUID as a login.
        return nonNumeric;
    }

    private static boolean browser(ResolveInfo info, String ownPackage) {
        if (info == null || info.activityInfo == null || info.filter == null) return false;
        ActivityInfo ai = info.activityInfo;
        return ai.enabled && ai.exported && ai.applicationInfo != null
                && ai.applicationInfo.enabled && ai.packageName != null
                && !ai.packageName.equals(ownPackage)
                && info.filter.hasDataScheme("https")
                && info.filter.hasCategory(Intent.CATEGORY_BROWSABLE)
                && info.filter.countDataAuthorities() == 0;
    }

    private static synchronized boolean open(Activity activity, String url) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return false;
        long now = SystemClock.uptimeMillis();
        Launch previous = launches.get(activity);
        if (previous != null && previous.url.equals(url) && now - previous.at < 1000) {
            return true; // An immediately repeated event must not launch native checkout.
        }
        PackageManager pm = activity.getPackageManager();
        // A generic HTTPS host selects browser-capable handlers, not Twitch app links.
        Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.invalid/"))
                .addCategory(Intent.CATEGORY_BROWSABLE);
        int flags = PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER;
        List<ResolveInfo> handlers = pm.queryIntentActivities(probe, flags);
        if (handlers == null) return false;
        ResolveInfo preferred = pm.resolveActivity(probe, flags);
        String ownPackage = activity.getPackageName();
        ResolveInfo selected = null;
        for (ResolveInfo candidate : handlers) {
            if (!browser(candidate, ownPackage)) continue;
            if (selected == null) selected = candidate;
            if (preferred != null && preferred.activityInfo != null
                    && candidate.activityInfo.packageName.equals(preferred.activityInfo.packageName)) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) return false;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setComponent(new android.content.ComponentName(
                        selected.activityInfo.packageName, selected.activityInfo.name));
        // Record before launch so no fallible bookkeeping follows a successful start.
        launches.put(activity, new Launch(url, now));
        try {
            activity.startActivity(intent);
            return true;
        } catch (RuntimeException failure) {
            launches.remove(activity);
            return false;
        }
    }

    private static final class Launch {
        final String url;
        final long at;
        Launch(String url, long at) { this.url = url; this.at = at; }
    }
}
