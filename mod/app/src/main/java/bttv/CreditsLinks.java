package bttv;

import java.net.URI;
import java.net.URISyntaxException;

/** Validate supplied historical destinations; never synthesize a profile URL. */
public final class CreditsLinks {
    private CreditsLinks() {
    }

    public static String destination(String original) {
        if (original == null || original.isEmpty()) {
            return null;
        }
        // One historical profile has a trailing space. Trim surrounding
        // whitespace only; never infer a host/path from a contributor name.
        String supplied = original.trim();
        try {
            URI uri = new URI(supplied);
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && uri.getUserInfo() == null ? supplied : null;
        } catch (URISyntaxException invalid) {
            return null;
        }
    }
}
