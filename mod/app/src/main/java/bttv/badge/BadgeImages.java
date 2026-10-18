package bttv.badge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.Xml;

import com.caverock.androidsvg.SVG;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Startup-only, independent static badge assets. Row binding only reads ready memory. */
public final class BadgeImages {
    static final int MAX_ASSET = 256 * 1024;
    static final int MAX_DIMENSION = 256;
    static final int RASTER_SIZE = 72;
    static final int PROVIDER_CACHE_BYTES = 2 * 1024 * 1024;
    static final long EXPIRES_MS = TimeUnit.HOURS.toMillis(24);
    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private static final Slot BTTV = new Slot();
    private static final Slot FFZ = new Slot();

    /** Marks a span-preserving result; it contains no user, view or context. */
    public static final class Marker {}

    public static final class ReadyBadge {
        public final String title;
        public final Bitmap bitmap;
        ReadyBadge(String title, Bitmap bitmap) { this.title = title; this.bitmap = bitmap; }
    }

    static final class Ready {
        final BadgeCatalog.Snapshot snapshot;
        final Map<String, Bitmap> bitmaps;
        final long created;
        Ready(BadgeCatalog.Snapshot snapshot, Map<String, Bitmap> bitmaps, long created) {
            this.snapshot = snapshot;
            this.bitmaps = Collections.unmodifiableMap(new HashMap<>(bitmaps));
            this.created = created;
        }
    }

    /** Per-provider transaction guard; failed/old transactions cannot corrupt a good snapshot. */
    static final class Slot {
        volatile Ready ready;
        private long generation;
        private boolean loading;
        synchronized long begin() { if (loading) return -1; loading = true; return ++generation; }
        synchronized boolean finish(long ticket, Ready result) {
            if (!loading || generation != ticket) return false;
            if (result != null) ready = result;
            loading = false;
            return true;
        }
    }

    static final class Response {
        final byte[] bytes;
        final String mime;
        Response(byte[] bytes, String mime) { this.bytes = bytes; this.mime = mime; }
    }

    static final class HttpFailure extends IOException {
        final int status;
        final long retryMillis;
        HttpFailure(int status, long retryMillis) {
            super("Badge provider unavailable"); this.status = status; this.retryMillis = retryMillis;
        }
    }

    interface Fetcher { Response get(String url, int limit, String mime) throws IOException; }
    private BadgeImages() {}

    public static void initialize(Context context) {
        try {
            if (context == null || context.getApplicationContext() == null
                    || !STARTED.compareAndSet(false, true)) return;
            ThreadPoolExecutor workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(2), new ThreadFactory() {
                @Override public Thread newThread(Runnable task) {
                    Thread worker = new Thread(task, "bttv-badge-startup");
                    worker.setDaemon(true);
                    return worker;
                }
            });
            for (final BadgeCatalog.Provider provider : BadgeCatalog.Provider.values()) {
                workers.execute(new Runnable() {
                    @Override public void run() {
                        load(provider, provider == BadgeCatalog.Provider.BTTV ? BTTV : FFZ,
                                new Fetcher() {
                            @Override public Response get(String url, int limit, String mime) throws IOException {
                                return fetch(url, limit, mime);
                            }
                        });
                    }
                });
            }
            // Exactly two startup jobs; no scheduler, per-message requests or permanent poller.
            workers.shutdown();
        } catch (Throwable ignored) {
            // Initialization must never interrupt Twitch startup.
        }
    }

    static void load(BadgeCatalog.Provider provider, Slot slot, Fetcher fetcher) {
        long ticket = slot.begin();
        if (ticket < 0) return;
        Ready result = null;
        try {
            Response metadata = null;
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    metadata = fetcher.get(provider.endpoint, BadgeCatalog.MAX_METADATA, "application/json");
                    break;
                } catch (IOException unavailable) {
                    long wait = retryDelay(unavailable, attempt);
                    if (wait < 0) return;
                    Thread.sleep(wait);
                }
            }
            if (metadata == null) return;
            BadgeCatalog.Snapshot snapshot = BadgeCatalog.parse(provider, utf8(metadata.bytes));
            Map<String, Bitmap> bitmaps = new HashMap<>();
            long deadline = SystemClock.elapsedRealtime() + 60_000;
            int bytes = 0;
            for (BadgeCatalog.Badge badge : snapshot.definitions.values()) {
                if (SystemClock.elapsedRealtime() > deadline) break;
                try {
                    String mime = provider == BadgeCatalog.Provider.BTTV ? "image/svg+xml" : "image/png";
                    Response asset = fetcher.get(badge.url, MAX_ASSET, mime);
                    Bitmap bitmap = decode(provider, asset.bytes, badge.background);
                    int allocation = bitmap.getAllocationByteCount();
                    if ((long) bytes + allocation > PROVIDER_CACHE_BYTES) break;
                    bytes += allocation;
                    bitmaps.put(badge.key(), bitmap);
                } catch (Exception ignored) {
                    // Unready assets create no span or blank space. No per-row retry.
                }
            }
            result = new Ready(snapshot, bitmaps, SystemClock.elapsedRealtime());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Throwable ignored) {
            // No raw provider data, IDs or exceptions are logged.
        } finally {
            slot.finish(ticket, result);
        }
    }

    static long retryDelay(IOException failure, int attempt) {
        if (attempt >= 2) return -1;
        if (failure instanceof HttpFailure) {
            HttpFailure http = (HttpFailure) failure;
            if (http.status != 429 && http.status < 500) return -1;
            if (http.retryMillis > 60_000) return -1; // Do not retry earlier than the server permits.
            return Math.max(http.retryMillis, attempt == 0 ? 1_000 : 4_000);
        }
        return attempt == 0 ? 1_000 : 4_000;
    }

    /** Memory-only, deterministic provider order; caller owns the message author's ID. */
    public static List<ReadyBadge> ready(String authorId) {
        String author = BadgeCatalog.authorId(authorId);
        if (author == null) return Collections.emptyList();
        return ready(author, BTTV.ready, FFZ.ready, SystemClock.elapsedRealtime());
    }

    static List<ReadyBadge> ready(String author, Ready bttv, Ready ffz, long now) {
        List<ReadyBadge> found = null;
        for (Ready provider : new Ready[] {bttv, ffz}) {
            if (provider == null || now < provider.created || now - provider.created > EXPIRES_MS) continue;
            for (BadgeCatalog.Badge badge : provider.snapshot.forAuthor(author)) {
                Bitmap bitmap = provider.bitmaps.get(badge.key());
                if (bitmap == null) return Collections.emptyList();
                if (found == null) found = new ArrayList<>();
                if (found.size() < BadgeCatalog.MAX_PER_AUTHOR) found.add(new ReadyBadge(badge.title, bitmap));
            }
        }
        return found == null ? Collections.<ReadyBadge>emptyList() : found;
    }

    static Response fetch(String value, int limit, String expectedMime) throws IOException {
        boolean metadata = value.equals(BadgeCatalog.Provider.BTTV.endpoint)
                || value.equals(BadgeCatalog.Provider.FFZ.endpoint);
        if (!metadata) {
            BadgeCatalog.assetUrl(value.startsWith("https://cdn.betterttv.net/")
                    ? BadgeCatalog.Provider.BTTV : BadgeCatalog.Provider.FFZ, value);
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(value).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(7_000);
        connection.setReadTimeout(10_000);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", expectedMime);
        connection.setRequestProperty("User-Agent", "bttv-android");
        try {
            int status = connection.getResponseCode();
            if (status != 200) {
                long delay = 0;
                String retry = connection.getHeaderField("Retry-After");
                if (retry != null) {
                    try {
                        long seconds = Long.parseLong(retry);
                        delay = seconds < 0 || seconds > 60 ? Long.MAX_VALUE : seconds * 1000;
                    }
                    catch (RuntimeException invalid) { delay = Long.MAX_VALUE; }
                    if (delay < 0) delay = Long.MAX_VALUE;
                }
                throw new HttpFailure(status, delay);
            }
            String mime = connection.getContentType();
            if (mime == null || !mime.split(";", 2)[0].trim().toLowerCase(Locale.ROOT).equals(expectedMime)
                    || connection.getContentLengthLong() > limit) throw new IOException("Invalid badge response");
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if ((long) output.size() + n > limit) throw new IOException("Badge response too large");
                    output.write(buffer, 0, n);
                }
                return new Response(output.toByteArray(), expectedMime);
            }
        } finally { connection.disconnect(); }
    }

    static String utf8(byte[] bytes) throws IOException {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }

    /** Reconstruct a tiny static subset, never pass arbitrary remote SVG to AndroidSVG. */
    static String safeSvg(byte[] bytes) throws Exception {
        if (bytes.length == 0 || bytes.length > MAX_ASSET) throw new IOException("Invalid badge SVG");
        String xml = utf8(bytes);
        if (xml.contains("<!") || xml.contains("<?") || xml.indexOf('&') >= 0)
            throw new IOException("Unsupported badge SVG");
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(xml));
        StringBuilder safe = new StringBuilder();
        int paths = 0, pathChars = 0;
        boolean root = false, ended = false;
        for (int event = parser.nextToken(); event != XmlPullParser.END_DOCUMENT; event = parser.nextToken()) {
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if (ended || !"http://www.w3.org/2000/svg".equals(parser.getNamespace())) throw new IOException("Unsupported badge SVG");
                boolean svg = "svg".equals(name) && parser.getDepth() == 1 && !root;
                boolean path = "path".equals(name) && parser.getDepth() == 2 && root;
                if (!svg && !path) throw new IOException("Unsupported badge SVG");
                if (svg) { root = true; safe.append("<svg xmlns=\"http://www.w3.org/2000/svg\""); }
                else { if (++paths > 64) throw new IOException("Badge SVG too complex"); safe.append("<path"); }
                boolean width = false, height = false, viewBox = false, data = false;
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    String attribute = parser.getAttributeName(i), value = parser.getAttributeValue(i);
                    String namespace = parser.getAttributeNamespace(i);
                    if (namespace != null && !namespace.isEmpty()) throw new IOException("Unsupported badge SVG");
                    if (svg && ("width".equals(attribute) || "height".equals(attribute))) {
                        if (!value.matches("[0-9]{1,3}(\\.[0-9]{1,3})?")) throw new IOException("Invalid badge size");
                        float size = Float.parseFloat(value);
                        if (size <= 0 || size > MAX_DIMENSION) throw new IOException("Invalid badge size");
                        if ("width".equals(attribute)) width = true; else height = true;
                    } else if (svg && "viewBox".equals(attribute)) {
                        String[] bounds = value.trim().split("[ ,]+");
                        if (bounds.length != 4) throw new IOException("Invalid badge viewBox");
                        for (String bound : bounds) {
                            if (!bound.matches("-?[0-9]{1,4}(\\.[0-9]{1,3})?")
                                    || Math.abs(Float.parseFloat(bound)) > 4096) throw new IOException("Invalid badge viewBox");
                        }
                        if (Float.parseFloat(bounds[2]) <= 0 || Float.parseFloat(bounds[3]) <= 0) throw new IOException("Invalid badge viewBox");
                        viewBox = true;
                    } else if (path && "d".equals(attribute)) {
                        pathChars += value.length();
                        if (value.isEmpty() || pathChars > 24_000 || !value.matches("[MmLlHhVvCcSsQqTtAaZz0-9eE+., \\t\\r\\n-]+")) throw new IOException("Unsupported badge path");
                        data = true;
                    } else if (path && "fill".equals(attribute)) {
                        if (!value.matches("#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?|white|black|none")) throw new IOException("Unsupported badge fill");
                    } else throw new IOException("Unsupported badge SVG attribute");
                    safe.append(' ').append(attribute).append("=\"").append(value).append('"');
                }
                if (svg && (!width || !height || !viewBox) || path && !data) throw new IOException("Incomplete badge SVG");
                safe.append('>');
            } else if (event == XmlPullParser.END_TAG) {
                safe.append("</").append(parser.getName()).append('>');
                if (parser.getDepth() == 1) ended = true;
            } else if ((event != XmlPullParser.TEXT && event != XmlPullParser.IGNORABLE_WHITESPACE)
                    || !parser.isWhitespace()) throw new IOException("Unsupported badge SVG content");
        }
        if (!root || !ended || paths == 0) throw new IOException("Incomplete badge SVG");
        return safe.toString();
    }

    static Bitmap decode(BadgeCatalog.Provider provider, byte[] bytes, Integer background) throws Exception {
        if (bytes.length == 0 || bytes.length > MAX_ASSET) throw new IOException("Invalid badge asset");
        Bitmap target = Bitmap.createBitmap(RASTER_SIZE, RASTER_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        if (background != null) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(background);
            canvas.drawRoundRect(0, 0, RASTER_SIZE, RASTER_SIZE, 3, 3, paint);
        }
        if (provider == BadgeCatalog.Provider.BTTV) {
            SVG svg = SVG.getFromString(safeSvg(bytes));
            svg.setDocumentWidth(RASTER_SIZE);
            svg.setDocumentHeight(RASTER_SIZE);
            svg.renderToCanvas(canvas);
        } else {
            validatePng(bytes);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (options.outWidth <= 0 || options.outHeight <= 0 || options.outWidth > MAX_DIMENSION
                    || options.outHeight > MAX_DIMENSION) throw new IOException("Invalid badge raster size");
            options.inJustDecodeBounds = false;
            Bitmap raster = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (raster == null) throw new IOException("Invalid badge raster");
            canvas.drawBitmap(raster, null, new android.graphics.Rect(0, 0, RASTER_SIZE, RASTER_SIZE),
                    new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        }
        Bitmap immutable = target.copy(Bitmap.Config.ARGB_8888, false);
        if (immutable == null) throw new IOException("Badge image unavailable");
        return immutable;
    }

    static void validatePng(byte[] bytes) throws IOException {
        byte[] magic = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length < 33) throw new IOException("Invalid badge PNG");
        for (int i = 0; i < magic.length; i++) if (bytes[i] != magic[i]) throw new IOException("Invalid badge PNG");
        int offset = 8, chunks = 0;
        while (offset + 12 <= bytes.length && ++chunks <= 128) {
            int length = ByteBuffer.wrap(bytes, offset, 4).getInt();
            if (length < 0 || (long) offset + length + 12 > bytes.length) throw new IOException("Invalid badge PNG");
            String type = new String(bytes, offset + 4, 4, StandardCharsets.US_ASCII);
            if ("acTL".equals(type) || "fcTL".equals(type) || "fdAT".equals(type)) throw new IOException("Animated badges unsupported");
            offset += length + 12;
            if ("IEND".equals(type)) {
                if (offset != bytes.length) throw new IOException("Invalid badge PNG");
                return;
            }
        }
        throw new IOException("Invalid badge PNG");
    }
}
