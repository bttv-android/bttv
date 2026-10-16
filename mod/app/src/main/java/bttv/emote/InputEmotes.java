package bttv.emote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import bttv.Data;

/**
 * Our emotes in the chat input box (TwitchEmoteInputView).
 * The input draws every emote as a square span with a still image, and downloads that image itself.
 * - the span gets the width of the emote, from its aspect ratio
 * - the image is read from the disk cache of the chat (Fresco), so emotes that were shown in chat show right away
 * - animated emotes are drawn with an AnimatedImageDrawable instead of the still image
 */
public class InputEmotes {
    private static final String TAG = "LBTTVInputEmotes";
    private static final String CACHE_DIR = "bttv-input-emotes";
    private static final float MIN_RATIO = 0.25f;
    private static final float MAX_RATIO = 4f;

    // Fresco's DefaultDiskStorage, with the directory of the main cache (DiskCacheConfig "image_cache"):
    // <cache dir>/image_cache/v2.ols100.<version>/<abs(id.hashCode() % 100)>/<id>.cnt
    // with id = base64 (url safe, no padding, no wrap) of the SHA-1 of the url
    private static final String FRESCO_DIR = "image_cache";
    private static final String FRESCO_VERSION_PREFIX = "v2.ols100.";
    private static final String FRESCO_EXTENSION = ".cnt";

    // image urls of our emotes in the input
    private static final Map<String, Emote> emotes = new ConcurrentHashMap<>();
    // still image the input got -> animation drawn in its place
    private static final Map<Bitmap, Drawable> animations = Collections.synchronizedMap(new WeakHashMap<Bitmap, Drawable>());
    // only used on the main thread
    private static final Set<View> inputViews = Collections.newSetFromMap(new WeakHashMap<View, Boolean>());
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static Method getBitmap;
    private static int logged = 0;

    /**
     * @param uri url the input built for an emote
     * @return url to load instead
     */
    public static String imageUrl(String uri) {
        String id = EmoteUrlUtil.idFromTwitchUrl(uri);
        String realId = id == null ? null : EmoteUrlUtil.extractBTTVId(id);
        Emote emote = realId == null ? null : Emotes.getEmoteById(realId);
        if (emote == null) {
            return uri;
        }
        emotes.put(emote.url, emote);
        return emote.url;
    }

    private static Float ratio(String url) {
        Emote emote = url == null ? null : emotes.get(url);
        return emote == null ? null : Math.max(MIN_RATIO, Math.min(MAX_RATIO, emote.aspectRatio));
    }

    /** @return width of the span of the emote with this image url */
    public static int width(String url, int size) {
        Float ratio = ratio(url);
        return ratio == null ? size : Math.round(size * ratio);
    }

    /** @return right edge of the emote, drawn at left with the height size */
    public static float right(String url, float left, float size) {
        Float ratio = ratio(url);
        return left + (ratio == null ? size : size * ratio);
    }

    public static void onInputViewAttached(Object view) {
        if (view instanceof TextView) {
            inputViews.add((View) view);
        }
    }

    /**
     * Called on a background thread of the input's image loader.
     * @return image of one of our emotes, null to let the input load it
     */
    public static Bitmap bitmap(String url) {
        Emote emote = url == null ? null : emotes.get(url);
        if (emote == null) {
            return null;
        }
        try {
            String source = "chat cache";
            byte[] bytes = readFile(frescoFile(url));
            if (bytes == null) {
                source = "bttv cache";
                bytes = readFile(cacheFile(url));
            }
            if (bytes == null) {
                source = "network";
                bytes = download(url);
                if (bytes == null) {
                    return null;
                }
                File file = cacheFile(url);
                if (file != null) {
                    save(file, bytes);
                }
            }
            if (logged++ < 20) {
                Log.d(TAG, "bitmap: " + emote.code + " from " + source);
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap != null && emote.imageType.equals("gif")) {
                Drawable animation = animation(bytes);
                if (animation != null) {
                    animations.put(bitmap, animation);
                }
            }
            return bitmap;
        } catch (Throwable t) {
            Log.e(TAG, "bitmap: " + url, t);
            return null;
        }
    }

    private static Drawable animation(byte[] bytes) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null;
        }
        try {
            Drawable drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(bytes)));
            if (!(drawable instanceof AnimatedImageDrawable)) {
                return null;
            }
            drawable.setCallback(ANIMATION_CALLBACK);
            return drawable;
        } catch (Throwable t) {
            Log.w(TAG, "animation: ", t);
            return null;
        }
    }

    /**
     * Called by the span of an emote in place of drawing its still image.
     * @return true if the emote was drawn
     */
    public static boolean draw(Bitmap bitmap, Canvas canvas, RectF destination, Paint paint) {
        Drawable animation = bitmap == null ? null : animations.get(bitmap);
        if (animation == null) {
            return false;
        }
        animation.setBounds(Math.round(destination.left), Math.round(destination.top),
                Math.round(destination.right), Math.round(destination.bottom));
        animation.setAlpha(paint.getAlpha());
        animation.draw(canvas);
        if (!((Animatable) animation).isRunning()) {
            ((Animatable) animation).start();
        }
        return true;
    }

    // a TextView keeps what its text drew, invalidating the view does not draw a span again.
    // Setting the span again does, the input does the same when an image has loaded.
    private static void redraw(Drawable animation) {
        for (View view : new ArrayList<>(inputViews)) {
            if (!view.isAttachedToWindow()) {
                continue;
            }
            CharSequence text = ((TextView) view).getText();
            if (!(text instanceof Editable)) {
                continue;
            }
            Editable editable = (Editable) text;
            for (ReplacementSpan span : editable.getSpans(0, editable.length(), ReplacementSpan.class)) {
                if (animations.get(spanBitmap(span)) != animation) {
                    continue;
                }
                int start = editable.getSpanStart(span);
                int end = editable.getSpanEnd(span);
                if (start != -1 && end != -1) {
                    editable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    // EmoteImageSpan.getBitmap
    private static Bitmap spanBitmap(ReplacementSpan span) {
        try {
            if (getBitmap == null || getBitmap.getDeclaringClass() != span.getClass()) {
                Method method = span.getClass().getMethod("getBitmap");
                if (method.getReturnType() != Bitmap.class) {
                    return null;
                }
                getBitmap = method;
            }
            return (Bitmap) getBitmap.invoke(span);
        } catch (Throwable t) {
            return null;
        }
    }

    // drawables only keep a weak reference to their callback
    private static final Drawable.Callback ANIMATION_CALLBACK = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            redraw(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            mainHandler.postAtTime(what, who, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            mainHandler.removeCallbacks(what, who);
        }
    };

    private static File frescoFile(String url) throws Exception {
        Context ctx = Data.ctx;
        File[] versions = ctx == null ? null : new File(ctx.getCacheDir(), FRESCO_DIR).listFiles();
        if (versions == null) {
            return null;
        }
        byte[] hash = MessageDigest.getInstance("SHA-1").digest(url.getBytes("UTF-8"));
        String id = Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        String bucket = String.valueOf(Math.abs(id.hashCode() % 100));
        for (File version : versions) {
            if (!version.getName().startsWith(FRESCO_VERSION_PREFIX)) {
                continue;
            }
            File file = new File(new File(version, bucket), id + FRESCO_EXTENSION);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private static File cacheFile(String url) {
        Context ctx = Data.ctx;
        return ctx == null ? null : new File(new File(ctx.getCacheDir(), CACHE_DIR), fileName(url));
    }

    private static byte[] readFile(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try (InputStream in = new FileInputStream(file)) {
            return readAll(in);
        } catch (IOException e) {
            Log.w(TAG, "readFile: " + file, e);
            return null;
        }
    }

    private static byte[] download(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestProperty("User-Agent", "bttv-android");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                Log.w(TAG, "download: " + code + " for " + url);
                return null;
            }
            try (InputStream in = connection.getInputStream()) {
                return readAll(in);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    // written to a temporary file first, so a half written file is never read
    private static void save(File file, byte[] bytes) {
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            return;
        }
        File tmp = new File(dir, file.getName() + ".tmp" + Thread.currentThread().getId());
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(bytes);
        } catch (IOException e) {
            Log.w(TAG, "save: ", e);
            tmp.delete();
            return;
        }
        if (!tmp.renameTo(file)) {
            tmp.delete();
        }
    }

    private static String fileName(String url) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(url.getBytes("UTF-8"));
            StringBuilder name = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                name.append(String.format("%02x", b));
            }
            return name.toString();
        } catch (Exception e) {
            return Integer.toHexString(url.hashCode());
        }
    }
}
