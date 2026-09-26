package bttv.emote;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;

/** Autocomplete-only IDs. Image URLs remain in the originating row's catalog metadata. */
final class AutocompleteSyntheticId {
    private static final String PREFIX = "bttv31ac://";

    private AutocompleteSyntheticId() {}

    static final class Decoded {
        final String provider, id;
        Decoded(String provider, String id) { this.provider = provider; this.id = id; }
    }

    static String encode(String provider, String id) {
        if ("7TV".equals(provider)) provider = "STV";
        if (!("BTTV".equals(provider) || "FFZ".equals(provider) || "STV".equals(provider))
                || id == null || id.isEmpty() || id.length() > 512) return null;
        for (int i = 0; i < id.length(); i++) {
            if (Character.isISOControl(id.charAt(i)) || Character.isWhitespace(id.charAt(i))) return null;
        }
        try {
            return PREFIX + provider + "/" + URLEncoder.encode(id, "UTF-8");
        } catch (Exception ignored) {
            return null;
        }
    }

    static Decoded decode(String value) {
        if (value == null || !value.startsWith(PREFIX) || value.length() > 5000) return null;
        int separator = value.indexOf('/', PREFIX.length());
        if (separator < 0) return null;
        try {
            String provider = value.substring(PREFIX.length(), separator);
            String id = URLDecoder.decode(value.substring(separator + 1), "UTF-8");
            // Only our canonical encoding is accepted; raw URL/query fragments are not IDs.
            if (!value.equals(encode(provider, id))) return null;
            return new Decoded(provider, id);
        } catch (Exception ignored) {
            return null;
        }
    }

    static boolean validImageUrl(String value) {
        if (value == null) return false;
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getRawUserInfo() == null;
        } catch (Exception ignored) {
            return false;
        }
    }
}
