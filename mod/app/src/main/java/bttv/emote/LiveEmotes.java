package bttv.emote;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import bttv.Data;
import bttv.ResUtil;
import bttv.settings.Settings;

/** Text-only recognition for the ordinary live-chat presentation path. */
public final class LiveEmotes {
    private static final String TAG = "BTTV31TRACE";
    private static final String MARKER = "bttv31://";
    private static final long RETRY_AFTER_MS = 60_000L;
    private static long bttvRequestedAt = -RETRY_AFTER_MS;
    private static long ffzRequestedAt = -RETRY_AFTER_MS;
    private static long stvRequestedAt = -RETRY_AFTER_MS;
    private static volatile boolean lookupFailureLogged;

    private LiveEmotes() {}

    public static final class Part {
        public final String text;
        public final String marker;

        Part(String text, String marker) {
            this.text = text;
            this.marker = marker;
        }
    }

    private static final class Match {
        final int start;
        final int end;
        final Emote emote;

        Match(int start, int end, Emote emote) {
            this.start = start;
            this.end = end;
            this.emote = emote;
        }
    }

    /** Returns null when the original text token should be kept unchanged. */
    public static List<Part> split(String text) {
        if (text == null || text.isEmpty() || Data.ctx == null) {
            return null;
        }
        try {
            return splitLoaded(text);
        } catch (Throwable failure) {
            if (!lookupFailureLogged) {
                lookupFailureLogged = true;
                Log.w(TAG, "Third-party emote lookup failed; keeping original text", failure);
            }
            return null;
        }
    }

    private static List<Part> splitLoaded(String text) {
        requestGlobals();

        ArrayList<Part> parts = null;
        int copiedThrough = 0;
        int cursor = 0;
        while (cursor < text.length()) {
            int codePoint = text.codePointAt(cursor);
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                cursor += Character.charCount(codePoint);
                continue;
            }
            int runStart = cursor;
            do {
                cursor += Character.charCount(text.codePointAt(cursor));
            } while (cursor < text.length()
                    && !Character.isWhitespace(text.codePointAt(cursor))
                    && !Character.isSpaceChar(text.codePointAt(cursor)));
            int runEnd = cursor;
            String run = text.substring(runStart, runEnd);
            if (run.startsWith("@") || run.contains("://") || run.startsWith("www.")) {
                continue;
            }

            Match match = findMatch(text, runStart, runEnd);
            Emote emote = match == null ? null : match.emote;
            if (emote == null || emote.url == null || !emote.url.startsWith("https://")) {
                continue;
            }
            int codeStart = match.start;
            int codeEnd = match.end;
            if (parts == null) {
                parts = new ArrayList<>();
            }
            if (copiedThrough < codeStart) {
                parts.add(new Part(text.substring(copiedThrough, codeStart), null));
            }
            String code = text.substring(codeStart, codeEnd);
            String marker = MARKER + emote.source.name() + ":" + emote.id + ":" + emote.url;
            parts.add(new Part(code, marker));
            Log.i(TAG, "THIRD_PARTY_MATCH provider=" + emote.source
                    + " text=" + code + " id=" + emote.id);
            copiedThrough = codeEnd;
        }
        if (parts == null) {
            return null;
        }
        if (copiedThrough < text.length()) {
            parts.add(new Part(text.substring(copiedThrough), null));
        }
        return parts;
    }

    private static Match findMatch(String text, int runStart, int runEnd) {
        Match best = null;
        int left = runStart;
        do {
            int right = runEnd;
            do {
                if (left < right && (best == null || right - left > best.end - best.start)) {
                    Emote emote = Emotes.getEmote(Data.ctx, text.substring(left, right), -1);
                    if (emote != null) {
                        best = new Match(left, right, emote);
                    }
                }
                if (right <= left || !isOuterPunctuation(text.codePointBefore(right))) {
                    break;
                }
                right -= Character.charCount(text.codePointBefore(right));
            } while (true);
            if (left >= runEnd || !isOuterPunctuation(text.codePointAt(left))) {
                break;
            }
            left += Character.charCount(text.codePointAt(left));
        } while (true);
        return best;
    }

    private static boolean isOuterPunctuation(int codePoint) {
        switch (Character.getType(codePoint)) {
            case Character.DASH_PUNCTUATION:
            case Character.START_PUNCTUATION:
            case Character.END_PUNCTUATION:
            case Character.OTHER_PUNCTUATION:
            case Character.INITIAL_QUOTE_PUNCTUATION:
            case Character.FINAL_QUOTE_PUNCTUATION:
                return true;
            default:
                return false;
        }
    }

    /** A null result means f86.d must use the original Twitch URL path. */
    public static String urlForMarker(String marker, String text) {
        if (marker == null || !marker.startsWith(MARKER)) {
            return null;
        }
        int providerEnd = marker.indexOf(':', MARKER.length());
        int idEnd = providerEnd < 0 ? -1 : marker.indexOf(':', providerEnd + 1);
        if (providerEnd < 0 || idEnd < 0) {
            return null;
        }
        String provider = marker.substring(MARKER.length(), providerEnd);
        if (!provider.equals("BTTV") && !provider.equals("FFZ") && !provider.equals("STV")) {
            return null;
        }
        String url = marker.substring(idEnd + 1);
        if (!url.startsWith("https://")) {
            return null;
        }
        Log.i(TAG, "THIRD_PARTY_URL provider=" + provider
                + " text=" + text + " url=" + url);
        return url;
    }

    private static synchronized void requestGlobals() {
        long now = android.os.SystemClock.elapsedRealtime();
        if (ResUtil.getBooleanFromSettings(Settings.BTTVEmotesEnabled)
                && Emotes.globalEmotesBTTV.isEmpty() && now - bttvRequestedAt >= RETRY_AFTER_MS) {
            bttvRequestedAt = now;
            LiveEmoteGlobals.load(Emotes.Source.BTTV);
        }
        if (ResUtil.getBooleanFromSettings(Settings.FFZEmotesEnabled)
                && Emotes.globalEmotesFFZ.isEmpty() && now - ffzRequestedAt >= RETRY_AFTER_MS) {
            ffzRequestedAt = now;
            LiveEmoteGlobals.load(Emotes.Source.FFZ);
        }
        if (ResUtil.getBooleanFromSettings(Settings.SevenTVEmotesEnabled)
                && Emotes.globalEmotes7TV.isEmpty() && now - stvRequestedAt >= RETRY_AFTER_MS) {
            stvRequestedAt = now;
            LiveEmoteGlobals.load(Emotes.Source.STV);
        }
    }
}
