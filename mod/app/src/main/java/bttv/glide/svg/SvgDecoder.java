package bttv.glide.svg;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;

import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.caverock.androidsvg.SVG;

import java.io.IOException;
import java.io.InputStream;

public class SvgDecoder implements ResourceDecoder<InputStream, SVG> {
    private static final String TAG = "LBTTVSVG";

    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
        // TODO
        return true;
    }

    @Nullable
    @Override
    public Resource<SVG> decode(@NonNull InputStream source, int width, int height, @NonNull Options options) throws IOException {
        try {
            SVG svg = SVG.getFromInputStream(source);
            Log.d(TAG, "decode: width: " + width + (width == SIZE_ORIGINAL ? " (SIZE_ORIGINAL)" : ""));
            Log.d(TAG, "decode: height: " + height + (height == SIZE_ORIGINAL ? " (SIZE_ORIGINAL)" : ""));

            float w = width != SIZE_ORIGINAL ? (float) width : dpToPx(18.0f);
            float h = height != SIZE_ORIGINAL ? (float) height : dpToPx(18.0f);
            svg.setDocumentWidth(w);
            svg.setDocumentHeight(h);

            return new SimpleResource<>(svg);
        } catch (Exception e) {
            throw new IOException("Cannot load SVG from stream", e);
        }
    }

    private static float dpToPx(float f) {
        return android.util.TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, f, android.content.res.Resources.getSystem().getDisplayMetrics());
    }
}
