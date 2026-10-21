package bttv.commerce;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import bttv.Res;
import bttv.ResUtil;

/** Additive, screen-owned navigation links. No native checkout or eligibility mutation. */
public final class BrowserPurchaseUi {
    private static final String PROFILE = "bttv.web.profile.actions";
    private static final String VIEWER = "bttv.web.viewer.actions";

    /** Returns null when the bound native context is no longer valid. */
    public interface Source { String login(); }

    private BrowserPurchaseUi() {}

    public static boolean validChannelId(String id) {
        if (id == null || id.isEmpty() || id.length() > 20) return false;
        boolean positive = false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') return false;
            if (c != '0') positive = true;
        }
        return positive;
    }

    public static void profile(Activity activity, View anchor, Source source) {
        bind(activity, anchor, source, false);
    }

    public static void viewer(Activity activity, View root, Source source) {
        if (activity == null || root == null) return;
        try {
            int id = ResUtil.getResourceId(activity, "viewer_card_social_actions", "id");
            if (id != 0) bind(activity, root.findViewById(id), source, true);
        } catch (Throwable ignored) { /* Native card remains intact. */ }
    }

    private static void bind(Activity activity, View anchor, Source source, boolean viewer) {
        if (anchor == null || !(anchor.getParent() instanceof LinearLayout)) return;
        LinearLayout parent = (LinearLayout) anchor.getParent();
        String tag = viewer ? VIEWER : PROFILE;
        View old = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (tag.equals(child.getTag())) { old = child; break; }
        }
        // Rebinding replaces our own row only; never changes a native control/listener.
        if (old != null) parent.removeView(old);
        LinearLayout row = null;
        try {
            if (activity == null || activity.isFinishing() || activity.isDestroyed()
                    || source == null) return;
            String login = source.login();
            if (!WebPurchaseRedirect.validLogin(login)) return;
            row = new LinearLayout(anchor.getContext());
            row.setTag(tag);
            row.setOrientation(LinearLayout.VERTICAL);
            int margin = Math.round(16 * anchor.getResources().getDisplayMetrics().density);
            row.setPadding(margin, 0, margin, 0);
            if (!viewer) add(row, activity, parent, anchor, source, login,
                    Res.strings.bttv_web_subscribe);
            add(row, activity, parent, anchor, source, login, viewer
                    ? Res.strings.bttv_web_gift_manual : Res.strings.bttv_web_gift_subs);
            parent.addView(row, parent.indexOfChild(anchor) + 1,
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
        } catch (Throwable ignored) {
            if (row != null && row.getParent() == parent) parent.removeView(row);
        }
    }

    private static void add(LinearLayout row, Activity activity, LinearLayout parent,
                            View anchor, Source source, String login, Res.strings label) {
        Button button = new Button(anchor.getContext());
        button.setText(ResUtil.getLocaleString(activity, label));
        button.setAllCaps(false);
        row.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(view -> {
            try {
                // Detached/recycled/changed screens cannot navigate using captured identities.
                if (!view.isAttachedToWindow() || !view.isShown() || row.getParent() != parent
                        || anchor.getParent() != parent || !login.equals(source.login())
                        || !WebPurchaseRedirect.openChannelWeb(activity, login)) {
                    unavailable(activity);
                }
            } catch (Throwable ignored) { unavailable(activity); }
        });
    }

    private static void unavailable(Activity activity) {
        try {
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                Toast.makeText(activity, ResUtil.getLocaleString(activity,
                        Res.strings.bttv_web_navigation_unavailable), Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable ignored) { /* No fallback into native checkout. */ }
    }
}
