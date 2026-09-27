package bttv.emote;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Picker-only, canonical identities. Never interpreted as a CDN URL. */
final class PickerSyntheticId {
    static final String PREFIX = "bttv31pk:";
    final long session;
    final boolean channel;
    final String provider, channelId, id, code;

    private PickerSyntheticId(long session, boolean channel, String provider,
                              String channelId, String id, String code) {
        this.session = session; this.channel = channel; this.provider = provider;
        this.channelId = channelId; this.id = id; this.code = code;
    }

    static boolean positive(String value) {
        if (value == null || value.isEmpty() || value.length() > 19 || value.charAt(0) == '0') return false;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) < '0' || value.charAt(i) > '9') return false;
        try { return Long.parseLong(value) > 0; } catch (NumberFormatException ignored) { return false; }
    }

    static boolean token(String value) {
        if (value == null || value.isEmpty() || value.length() > 512) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) || Character.isISOControl(c) || Character.isSurrogate(c)) return false;
        }
        return true;
    }

    static String encode(long session, boolean channel, String provider, String channelId,
                         String id, String code) {
        if (session <= 0 || !("FFZ".equals(provider) || "BTTV".equals(provider) || "STV".equals(provider))
                || (channel ? !positive(channelId) : !"0".equals(channelId)) || !token(id) || !token(code)) return null;
        String value = PREFIX + "v1:" + session + ":" + (channel ? "C" : "G") + ":" + provider + ":" + channelId
                + ":" + b64(id) + ":" + b64(code);
        return value.length() <= 1800 ? value : null;
    }

    private static String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static PickerSyntheticId decode(String value) {
        if (value == null || value.length() > 1800 || !value.startsWith(PREFIX)) return null;
        try {
            String[] p = value.split(":", -1);
            if (p.length != 8 || !"v1".equals(p[1]) || !("C".equals(p[3]) || "G".equals(p[3]))) return null;
            long session = Long.parseLong(p[2]);
            String id = new String(Base64.getUrlDecoder().decode(p[6]), StandardCharsets.UTF_8);
            String code = new String(Base64.getUrlDecoder().decode(p[7]), StandardCharsets.UTF_8);
            boolean channel = "C".equals(p[3]);
            if (!value.equals(encode(session, channel, p[4], p[5], id, code))) return null;
            return new PickerSyntheticId(session, channel, p[4], p[5], id, code);
        } catch (IllegalArgumentException ignored) { return null; }
    }
}
