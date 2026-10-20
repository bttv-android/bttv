package bttv.badge;

import com.bttv.badgehttp.okhttp3.Authenticator;
import com.bttv.badgehttp.okhttp3.ConnectionPool;
import com.bttv.badgehttp.okhttp3.CookieJar;
import com.bttv.badgehttp.okhttp3.OkHttpClient;
import com.bttv.badgehttp.okhttp3.Request;
import com.bttv.badgehttp.okhttp3.Response;
import com.bttv.badgehttp.okhttp3.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Java-only facade for two public, allowlisted Chatterino resources. No account state. */
final class ChatterinoHttp {
    private static final class Client {
        static final OkHttpClient INSTANCE = newClient();
    }

    private ChatterinoHttp() {}

    private static OkHttpClient newClient() {
        return new OkHttpClient.Builder()
                .cookieJar(CookieJar.NO_COOKIES)
                .authenticator(Authenticator.NONE).proxyAuthenticator(Authenticator.NONE)
                .proxy(Proxy.NO_PROXY).cache(null)
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.SECONDS))
                .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
                .connectTimeout(7, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build();
    }

    static BadgeImages.Response get(String url, int limit, String mime) throws IOException {
        return get(Client.INSTANCE, url, limit, mime);
    }

    // Package-private production body, also exercised with controlled, isolated HTTPS fixtures.
    // The URL/mime/size policy still runs when a fixture supplies its own client.
    static BadgeImages.Response get(OkHttpClient client, String url, int limit, String mime) throws IOException {
        boolean metadata = BadgeCatalog.Provider.CHATTERINO.endpoint.equals(url);
        if (!metadata) BadgeCatalog.assetUrl(BadgeCatalog.Provider.CHATTERINO, url);
        if (limit <= 0 || limit > (metadata ? BadgeCatalog.MAX_METADATA : BadgeImages.MAX_ASSET)
                || !(metadata ? "application/json" : "image/png").equals(mime))
            throw new IOException("Invalid badge request");
        Request request = new Request.Builder().url(url).header("Accept", mime)
                .header("User-Agent", "bttv-android").build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) throw new BadgeImages.HttpFailure(response.code(), 0);
            ResponseBody body = response.body();
            String type = response.header("Content-Type");
            if (body == null || type == null
                    || !type.split(";", 2)[0].trim().toLowerCase(Locale.ROOT).equals(mime)
                    || body.contentLength() > limit) throw new IOException("Invalid badge response");
            try (InputStream input = body.byteStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if ((long) output.size() + n > limit) throw new IOException("Badge response too large");
                    output.write(buffer, 0, n);
                }
                return new BadgeImages.Response(output.toByteArray(), mime);
            }
        }
    }
}
