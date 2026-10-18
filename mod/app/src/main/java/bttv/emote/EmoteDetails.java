package bttv.emote;

import java.net.URI;
import java.util.Map;

/** Local card data for a bound chat emote. Never resolves by the current channel. */
public final class EmoteDetails {
    private static final String PREFIX = "bttv31://";
    public final String code;
    public final String provider;
    public final String id;
    public final String url;
    public final String owner;
    public final Boolean animated;

    private EmoteDetails(String code, String provider, String id, String url,
                         String owner, Boolean animated) {
        this.code = code;
        this.provider = provider;
        this.id = id;
        this.url = url;
        this.owner = owner;
        this.animated = animated;
    }

    /** Reserved chat scheme only; picker/autocomplete and native IDs pass through. */
    public static boolean isSynthetic(String marker) {
        return marker != null && marker.startsWith(PREFIX);
    }

    public static EmoteDetails create(String marker, String boundCode) {
        EmoteDetails data = parse(marker, boundCode);
        if (data == null || data.url == null) return data;
        try {
            Map<String, Emote> values = "FFZ".equals(data.provider) ? Emotes.globalEmotesFFZ
                    : "BTTV".equals(data.provider) ? Emotes.globalEmotesBTTV : Emotes.globalEmotes7TV;
            // Channel provenance is not present in the bound token. Do not guess it,
            // enumerate other channels, or use the legacy provider-agnostic ID map.
            return withCached(data, values == null ? null : values.get(data.code));
        } catch (Throwable unavailable) {
            return data;
        }
    }

    static EmoteDetails parse(String marker, String boundCode) {
        if (!isSynthetic(marker)) return null;
        String code = boundCode == null || boundCode.isEmpty() ? "Emote unavailable" : boundCode;
        int providerEnd = marker.indexOf(':', PREFIX.length());
        int idEnd = providerEnd < 0 ? -1 : marker.indexOf(':', providerEnd + 1);
        if (providerEnd < 0 || idEnd < 0 || marker.length() > 8192) return unavailable(code);
        String source = marker.substring(PREFIX.length(), providerEnd);
        if (!("BTTV".equals(source) || "FFZ".equals(source) || "STV".equals(source))) {
            return unavailable(code);
        }
        String provider = "STV".equals(source) ? "7TV" : source;
        String id = marker.substring(providerEnd + 1, idEnd);
        if (id.isEmpty() || id.length() > 512) return unavailable(code);
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9' || c == '_' || c == '-')) return unavailable(code);
        }
        String url = marker.substring(idEnd + 1);
        return new EmoteDetails(code, provider, id, safeUrl(source, id, url) ? url : null, null, null);
    }

    private static EmoteDetails unavailable(String code) {
        return new EmoteDetails(code, "Third-party emote", null, null, null, null);
    }

    private static boolean safeUrl(String source, String id, String value) {
        try {
            URI uri = new URI(value);
            String host = "BTTV".equals(source) ? "cdn.betterttv.net"
                    : "FFZ".equals(source) ? "cdn.frankerfacez.com" : "cdn.7tv.app";
            String path = uri.getRawPath();
            if (!"https".equals(uri.getScheme()) || !host.equals(uri.getHost())
                    || uri.getRawUserInfo() != null || uri.getPort() != -1
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || path == null || !path.startsWith("/emote/" + id + "/")
                    || path.endsWith("/") || !uri.normalize().equals(uri)) return false;
            for (int i = 0; i < path.length(); i++) {
                char c = path.charAt(i);
                if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                        || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.' || c == '/')) return false;
            }
            return true;
        } catch (Exception malformed) {
            return false;
        }
    }

    static EmoteDetails withCached(EmoteDetails data, Emote entry) {
        if (data == null || data.url == null || entry == null || entry.source == null
                || !data.provider.equals(entry.source == Emotes.Source.STV ? "7TV" : entry.source.name())
                || !data.id.equals(entry.id) || !data.code.equals(entry.code) || !data.url.equals(entry.url)) return data;
        String owner = entry.owner;
        if ("BTTV".equals(data.provider) || owner == null || owner.trim().isEmpty()) owner = null;
        Boolean animated = "gif".equals(entry.imageType) ? Boolean.TRUE
                : "png".equals(entry.imageType) ? Boolean.FALSE : null;
        return new EmoteDetails(data.code, data.provider, data.id, data.url, owner, animated);
    }

    public String description() {
        String text = provider;
        if (owner != null) text += "\nProvider owner: " + owner;
        if (animated != null) text += animated ? "\nAnimated emote" : "\nStatic emote";
        if (url == null) text += "\nPreview unavailable";
        return text;
    }
}
