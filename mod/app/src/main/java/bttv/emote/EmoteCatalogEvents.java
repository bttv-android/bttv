package bttv.emote;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/** Publication metadata only, not a catalog or a request scheduler. */
final class EmoteCatalogEvents {
    interface Listener { void published(String provider, String channel, long revision); }
    interface Subscription { void dispose(); }
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final AtomicLong REVISION = new AtomicLong();
    static Subscription subscribe(Listener listener) {
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }
    static void published(String provider, String channel) {
        long revision = REVISION.incrementAndGet();
        for (Listener listener : LISTENERS) {
            try { listener.published(provider, channel, revision); } catch (Throwable ignored) { }
        }
    }
    private EmoteCatalogEvents() {}
}
