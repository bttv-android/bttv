package bttv.emote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure, cursor-local autocomplete rules. No Android state or catalog cache. */
final class EmoteAutocompleteLogic {
    private EmoteAutocompleteLogic() {}

    static final class Token {
        final int start, end, cursor;
        final String prefix;

        Token(int start, int end, int cursor, String prefix) {
            this.start = start;
            this.end = end;
            this.cursor = cursor;
            this.prefix = prefix;
        }

        boolean same(Token other) {
            return other != null && start == other.start && end == other.end
                    && cursor == other.cursor && prefix.equals(other.prefix);
        }
    }

    static boolean whitespace(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    static Token token(CharSequence text, int selectionStart, int selectionEnd) {
        if (text == null || selectionStart != selectionEnd || selectionStart < 0
                || selectionStart > text.length()) {
            return null;
        }
        int start = selectionStart;
        while (start > 0 && !whitespace(text.charAt(start - 1))) start--;
        if (selectionStart - start < 3 || text.charAt(start) != ':') return null;
        int end = selectionStart;
        while (end < text.length() && !whitespace(text.charAt(end))) end++;
        return new Token(start, end, selectionStart,
                text.subSequence(start + 1, selectionStart).toString());
    }

    static final class Suggestion {
        final String code, provider;
        final boolean channel;
        final String id, url, imageType;
        final Object nativeItem;

        Suggestion(String code, String provider, boolean channel) {
            this(code, provider, channel, null, null, null, null);
        }

        Suggestion(String code, String provider, boolean channel, String id,
                   String url, String imageType, Object nativeItem) {
            this.code = code;
            this.provider = provider;
            this.channel = channel;
            this.id = id;
            this.url = url;
            this.imageType = imageType;
            this.nativeItem = nativeItem;
        }

        static Suggestion nativeItem(String code, Object item) {
            return new Suggestion(code, "TWITCH", false, null, null, null, item);
        }
    }

    static boolean validCode(String code) {
        if (code == null || code.isEmpty()) return false;
        for (int i = 0; i < code.length(); i++) {
            if (whitespace(code.charAt(i)) || Character.isISOControl(code.charAt(i))) return false;
        }
        return true;
    }

    static List<Suggestion> match(String prefix, Iterable<Suggestion> candidates) {
        return match(prefix, candidates, 8);
    }

    static List<Suggestion> match(String prefix, Iterable<Suggestion> candidates, int limit) {
        if (prefix == null || prefix.length() < 2) return Collections.emptyList();
        Map<String, Suggestion> unique = new HashMap<>();
        for (Suggestion candidate : candidates) {
            if (candidate == null || !validCode(candidate.code)
                    || !candidate.code.regionMatches(true, 0, prefix, 0, prefix.length())) continue;
            Suggestion previous = unique.get(candidate.code);
            // For same-scope duplicates the caller's existing provider order wins.
            if (previous == null || (candidate.channel && !previous.channel)) {
                unique.put(candidate.code, candidate);
            }
        }
        ArrayList<Suggestion> result = new ArrayList<>(unique.values());
        Collections.sort(result, (left, right) -> {
            int order = Integer.compare(left.code.length(), right.code.length());
            if (order == 0) order = left.code.compareToIgnoreCase(right.code);
            if (order == 0) order = Boolean.compare(right.channel, left.channel);
            if (order == 0) order = left.code.compareTo(right.code);
            return order;
        });
        return new ArrayList<>(result.subList(0, Math.min(limit, result.size())));
    }

    /** Native exact, provider exact, mixed prefixes, then native substring results. */
    static List<Suggestion> merge(String prefix, List<Suggestion> nativeMatches,
                                  List<Suggestion> thirdParty) {
        Map<String, Suggestion> unique = new java.util.LinkedHashMap<>();
        for (Suggestion item : nativeMatches) {
            if (!unique.containsKey(item.code)) unique.put(item.code, item);
        }
        for (Suggestion item : match(prefix, thirdParty, Integer.MAX_VALUE)) {
            Suggestion old = unique.get(item.code);
            if (old == null || (old.nativeItem == null && item.channel && !old.channel)) {
                unique.put(item.code, item);
            }
        }
        List<Suggestion> result = new ArrayList<>(unique.values());
        Collections.sort(result, (left, right) -> {
            int leftGroup = group(prefix, left), rightGroup = group(prefix, right);
            int order = Integer.compare(leftGroup, rightGroup);
            if (order != 0) return order;
            // Preserve native substring ordering, including Twitch's own tie policy.
            if (leftGroup == 3) return 0;
            order = Integer.compare(left.code.length(), right.code.length());
            if (order == 0) order = left.code.compareToIgnoreCase(right.code);
            if (order == 0) order = Boolean.compare(right.nativeItem != null, left.nativeItem != null);
            if (order == 0) order = Boolean.compare(right.channel, left.channel);
            if (order == 0) order = left.code.compareTo(right.code);
            return order;
        });
        return new ArrayList<>(result.subList(0, Math.min(10, result.size())));
    }

    private static int group(String prefix, Suggestion item) {
        if (item.code.equalsIgnoreCase(prefix)) return item.nativeItem != null ? 0 : 1;
        return item.code.regionMatches(true, 0, prefix, 0, prefix.length()) ? 2 : 3;
    }

    static final class Insertion {
        final int start, end, cursor;
        final String replacement;

        Insertion(int start, int end, int cursor, String replacement) {
            this.start = start;
            this.end = end;
            this.cursor = cursor;
            this.replacement = replacement;
        }
    }

    static Insertion insertion(CharSequence text, Token token, String code) {
        if (token == null || !validCode(code)
                || !token.same(token(text, token.cursor, token.cursor))) return null;
        boolean hasWhitespace = token.end < text.length() && whitespace(text.charAt(token.end));
        String replacement = code + (hasWhitespace ? "" : " ");
        return new Insertion(token.start, token.end,
                token.start + replacement.length() + (hasWhitespace ? 1 : 0), replacement);
    }
}
