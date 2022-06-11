package bttv.glide.svg;

import android.graphics.Picture;
import android.graphics.drawable.PictureDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.caverock.androidsvg.SVG;

public class SvgDrawableTranscoder implements ResourceTranscoder<SVG, PictureDrawable> {
    @Nullable
    @Override
    public Resource<PictureDrawable> transcode(@NonNull Resource<SVG> svgResource, @NonNull Options options) {
        SVG svg = svgResource.get();
        Picture picture  = svg.renderToPicture();
        PictureDrawable drawable = new PictureDrawable(picture);
        return new SimpleResource<>(drawable);
    }
}
