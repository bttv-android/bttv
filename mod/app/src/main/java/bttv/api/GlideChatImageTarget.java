package bttv.api;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.TextView;

import tv.twitch.android.shared.ui.elements.span.UrlDrawable;

public class GlideChatImageTarget {

    public static boolean getIsBttvEmote(tv.twitch.android.shared.ui.elements.span.GlideChatImageTarget target) {
        UrlDrawable urlDrawable = target.mUrlDrawable;
        if (urlDrawable == null) {
            Log.i("LBTTVDEBUG", "getIsBttvEmote: urlDrawable) is null");
            return false;
        }
        String url = urlDrawable.getUrl();
        return url.contains("betterttv.net") || url.contains("7tv.app"); // TODO: find better way of determining this
    }

    public static void setRenderingView(tv.twitch.android.shared.ui.elements.span.GlideChatImageTarget target, TextView view) {
        target.bttvView = view;
    }

    public static void invalidateView(tv.twitch.android.shared.ui.elements.span.GlideChatImageTarget target, Drawable drawable) {
        try {
            Rect bounds = drawable.getBounds();
            if (bounds.bottom >= bounds.right) {
                return;
            }
            if (!getIsBttvEmote(target)) {
                return;
            }
            if (target.bttvView == null) {
                Log.w("LBTTVGlideChatImgTarget", "invalidateView: bttvView is null");
                return;
            }
            target.bttvView.invalidate();
            target.bttvView.setText(target.bttvView.getText()); // dirty trick to force re-draw
            target.bttvView = null; // drop ref idk how it could impact GC, and onResourceReady is only called once anyway (I hope)
        } catch (Throwable e) {
            Log.e("LBTTVGlideChatImgTarget", "invalidateView: error", e);
        }
    }
}
