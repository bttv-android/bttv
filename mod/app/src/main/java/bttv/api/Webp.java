package bttv.api;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;

public class Webp {
    public static void registerWebpDecoder(Context context, Glide glide, Registry registry) {
        try {
            Log.d("LBTTVWebP", "registerWebpDecoder called");
            bttv.Webp.registerWebpDecoder(context, glide, registry);
        } catch (Throwable t) {
            Log.e("LBTTVWebP", "failed to registerWebDecoder()", t);
        }
    }
}
