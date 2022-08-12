package bttv.emote;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.TextView;

import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;
import tv.twitch.android.shared.ui.elements.span.GlideChatImageCustomTarget;
import tv.twitch.android.shared.ui.elements.span.GlideChatImageTarget;
import tv.twitch.android.shared.ui.elements.span.UrlDrawable;

public class Glide {

    public static boolean getIsBttvEmote(GlideChatImageCustomTarget target) {
        UrlDrawable urlDrawable = target.mUrlDrawable;
        return getIsBttvEmote(urlDrawable);
    }

    public static boolean getIsBttvEmote(GlideChatImageTarget target) {
        UrlDrawable urlDrawable = target.mUrlDrawable;
        return getIsBttvEmote(urlDrawable);
    }

    public static boolean getIsBttvEmote(UrlDrawable urlDrawable) {
        if (urlDrawable == null) {
            Log.i("LBTTVChatImageTarget", "getIsBttvEmote: urlDrawable) is null");
            return false;
        }
        String url = urlDrawable.getUrl();
        return url.contains("betterttv.net") || url.contains("7tv.app"); // TODO: find better way of determining this
    }

    public static boolean shouldAnimateEmotes(UrlDrawable urlDrawable) {
        if (!getIsBttvEmote(urlDrawable)) {
            return true;
        }
        if (urlDrawable.getUrl().endsWith("?static=true") && !ResUtil.selectedDropdownFromSettingsIs(Settings.GifRenderMode, Res.strings.bttv_settings_gif_render_mode_animate_forever)) {
            return false;
        }
        return ResUtil.selectedDropdownFromSettingsIs(Settings.GifRenderMode, Res.strings.bttv_settings_gif_render_mode_animate)
                || ResUtil.selectedDropdownFromSettingsIs(Settings.GifRenderMode, Res.strings.bttv_settings_gif_render_mode_animate_forever);
    }

    public static void setRenderingView(GlideChatImageTarget target, TextView view) {
        target.bttvView = view;
    }

    public static void invalidateView(GlideChatImageTarget target, Drawable drawable) {
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
    }
}
