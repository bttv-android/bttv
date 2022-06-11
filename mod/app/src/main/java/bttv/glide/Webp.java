package bttv.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.integration.webp.decoder.WebpDrawable;

public class Webp {
    public static void registerWebpDecoder(Context context, Glide glide, Registry registry) {
        new com.bumptech.glide.integration.webp.WebpGlideLibraryModule().registerComponents(context, glide, registry);
    }

    public static void setWebpCallback(Drawable drawable, Drawable.Callback cb) {
        if (!(drawable instanceof WebpDrawable)) {
            return;
        }
        WebpDrawable webpDrawable = (WebpDrawable) drawable;
        webpDrawable.setCallback(cb);
    }

    public static void startWebpDrawable(Drawable drawable) {
        if (!(drawable instanceof WebpDrawable)) {
            return;
        }
        WebpDrawable webpDrawable = (WebpDrawable) drawable;
        if (!webpDrawable.isRunning())
            webpDrawable.start();
    }

    public static void stopWebpDrawable(Drawable drawable) {
        if (!(drawable instanceof WebpDrawable)) {
            return;
        }
        WebpDrawable webpDrawable = (WebpDrawable) drawable;
        if (webpDrawable.isRunning()) {
            webpDrawable.stop();
        }
    }
}
