package bttv;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;

public class Webp {
    public static void registerWebpDecoder(Context context, Glide glide, Registry registry) {
        new com.bumptech.glide.integration.webp.WebpGlideLibraryModule().registerComponents(context, glide, registry);
    }
}
