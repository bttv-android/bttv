package bttv.api;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.TextView;

import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;
import tv.twitch.android.shared.ui.elements.span.GlideChatImageTarget;
import tv.twitch.android.shared.ui.elements.span.UrlDrawable;

public class Glide {

    public static boolean getIsBttvEmote(GlideChatImageTarget target) {
        try {
            return bttv.emote.Glide.getIsBttvEmote(target);
        } catch (Throwable t) {
            Log.e("LBTTVGlide", "getIsBttvEmote: ", t);
            return false;
        }
    }

    public static boolean getIsBttvEmote(UrlDrawable urlDrawable) {
        try {
            return bttv.emote.Glide.getIsBttvEmote(urlDrawable);
        } catch (Throwable t) {
            Log.e("LBTTVGlide", "getIsBttvEmote: ", t);
            return false;
        }
    }

    public static boolean shouldAnimateEmotes(UrlDrawable urlDrawable) {
        try {
            return bttv.emote.Glide.shouldAnimateEmotes(urlDrawable);
        } catch (Throwable t) {
            Log.e("LBTTVGlide", "shouldAnimateEmotes: ", t);
            return false;
        }
    }

    public static void setRenderingView(GlideChatImageTarget target, TextView view) {
        try {
            bttv.emote.Glide.setRenderingView(target, view);
        } catch (Throwable t) {
            Log.e("LBTTVGlide", "setRenderingView: ", t);
        }
    }

    public static void invalidateView(GlideChatImageTarget target, Drawable drawable) {
        try {
            bttv.emote.Glide.invalidateView(target, drawable);
        } catch (Throwable e) {
            Log.e("LBTTVGlide", "invalidateView", e);
        }
    }

    public static void startWebpDrawable(Drawable drawable, Drawable.Callback callback) {
        Log.d("LBTTVGlide", "startWebpDrawable called ");
        try {
            bttv.emote.Glide.startWebpDrawable(drawable, callback);
        } catch (Throwable t) {
            Log.e("LBTTVGlide", "startWebpDrawable: ", t);
        }
    }
}
