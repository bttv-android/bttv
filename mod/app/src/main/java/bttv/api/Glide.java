package bttv.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.Log;
import android.widget.TextView;

import com.bumptech.glide.Registry;
import com.bumptech.glide.integration.webp.decoder.WebpDrawable;

import bttv.Data;
import tv.twitch.android.shared.ui.elements.span.GlideChatImageCustomTarget;
import tv.twitch.android.shared.ui.elements.span.GlideChatImageTarget;
import tv.twitch.android.shared.ui.elements.span.UrlDrawable;

public class Glide {

    public static boolean getIsBttvEmote(GlideChatImageCustomTarget target) {
        try {
            return bttv.emote.Glide.getIsBttvEmote(target);
        } catch (Throwable t) {
            Log.e("LBTTVGlide", "getIsBttvEmote: ", t);
            return false;
        }
    }

    public static void maybeSetBG(UrlDrawable urlDrawable, Canvas canvas, Drawable d) {
        Integer bg = Data.backgrounds.get(urlDrawable.getUrl());
        if (bg == null) {
            return;
        }
        float[] radii = new float[] { 3.0f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f };
        RoundRectShape shape = new RoundRectShape(radii, null, radii);

        ShapeDrawable drawable = new ShapeDrawable(shape);
        drawable.getPaint().setColor(bg);
        drawable.setBounds(d.getBounds());
        drawable.draw(canvas);
    }

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

    public static void startWebpDrawable(Drawable drawable) {
        try {
            bttv.glide.Webp.startWebpDrawable(drawable);
        } catch (Throwable t) {
            Log.e("LBTTVGlide", "startWebpDrawable: ", t);
        }
    }

    public static void stopWebpDrawable(Drawable drawable) {
        try {
            bttv.glide.Webp.stopWebpDrawable(drawable);
        } catch (Throwable t) {
            Log.e("LBTTVGlide", "stopWebpDrawable: ", t);
        }
    }

    public static void setWebpCallback(Drawable drawable, Drawable.Callback cb) {
        try {
            bttv.glide.Webp.setWebpCallback(drawable, cb);
        } catch (Throwable t) {
            Log.e("LBTTVGlide", "setWebpCallback: ", t);
        }
    }

    public static Bitmap getFirstFrame(Drawable drawable) {
        if (drawable instanceof WebpDrawable) {
            return ((WebpDrawable) drawable).getFirstFrame();
        }
        return null;
    }

    public static void registerWebpDecoder(Context context, com.bumptech.glide.Glide glide, Registry registry) {
        try {
            Log.d("LBTTVGlide", "registerWebpDecoder called");
            bttv.glide.Webp.registerWebpDecoder(context, glide, registry);
        } catch (Throwable t) {
            Log.e("LBTTVGlide", "failed to registerWebDecoder()", t);
        }
    }

    public static void registerSVGDecoder(Context context, com.bumptech.glide.Glide glide, Registry registry) {
        try {
            Log.d("LBTTVGlide", "registerSVGDecoder called");
            bttv.glide.svg.Svg.registerSvgDecoder(context, glide, registry);
        } catch (Throwable t) {
            Log.e("LBTTVGlide", "failed to registerSVGDecoder()", t);
        }
    }
}
