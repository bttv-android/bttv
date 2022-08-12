package bttv.glide.svg;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;

public class Svg {
    public static void registerSvgDecoder(Context context, Glide glide, Registry registry) {
        new SvgModule().registerComponents(context, glide, registry);
    }
}
