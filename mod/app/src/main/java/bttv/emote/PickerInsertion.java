package bttv.emote;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/** Pure edit planning and weak identity transactions. No equal-string matching. */
final class PickerInsertion {
    static final class Plan {
        final int start, end, cursor;
        final String replacement;
        Plan(int start, int end, String replacement) {
            this.start = start; this.end = end; this.replacement = replacement;
            cursor = start + replacement.length();
        }
    }

    static Plan plan(CharSequence text, int a, int b, String code) {
        int s = Math.min(a, b), e = Math.max(a, b);
        if (text == null || !PickerSyntheticId.token(code) || s < 0 || e > text.length()) return null;
        String leading = s > 0 && !Character.isWhitespace(Character.codePointBefore(text, s)) ? " " : "";
        if (e < text.length() && text.charAt(e) == ' ') e++;
        return new Plan(s, e, leading + code + " ");
    }

    static final class Pending<T> {
        final WeakReference<T> owner;
        final String syntheticId;
        boolean consumed;
        Pending(T owner, String id) { this.owner = new WeakReference<>(owner); syntheticId = id; }
    }

    private static final class Key extends WeakReference<String> {
        final int hash;
        Key(String value, ReferenceQueue<String> queue) { super(value, queue); hash = System.identityHashCode(value); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof Key && get() != null && get() == ((Key) other).get();
        }
    }

    static final class Registry<T> {
        private final ReferenceQueue<String> queue = new ReferenceQueue<>();
        private final Map<Key, Pending<T>> requests = new HashMap<>();
        private void clean() { Key key; while ((key = (Key) queue.poll()) != null) requests.remove(key); }
        synchronized String register(String canonicalCode, T owner, String id) {
            clean();
            String identity = new String(canonicalCode);
            requests.put(new Key(identity, queue), new Pending<>(owner, id));
            return identity;
        }
        synchronized Pending<T> find(String identity) { clean(); return requests.get(new Key(identity, null)); }
        // Consumed/stale records stay until their identity key is collected. Never fall back to stock.
    }
}
