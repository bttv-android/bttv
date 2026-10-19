package bttv.emote;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.widget.EditText;
import android.content.SharedPreferences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import bttv.Res;
import bttv.ResUtil;
import bttvcompat.NativePickerBridge;

/** classes7 UI/Rx coordinator. Native models and obfuscated interfaces stay in the ABI bridge. */
public final class PickerRuntime {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicLong NEXT = new AtomicLong();
    private static final Map<Long, WeakReference<Session>> SESSIONS = new HashMap<>();
    private static final WeakHashMap<Object, WeakReference<Session>> PRESENTERS = new WeakHashMap<>();
    private static final PickerInsertion.Registry<Session> PENDING = new PickerInsertion.Registry<>();

    private static boolean main() { return Looper.myLooper() == Looper.getMainLooper(); }
    private static void onMain(Runnable action) { if (main()) action.run(); else MAIN.post(action); }

    private static final class Session {
        final long id = NEXT.incrementAndGet();
        final String channel;
        final WeakReference<Object> presenter;
        final LiveEmoteAutocomplete.Lifecycle lifecycle;
        final String viewer;
        Set<String> nativeCodes = Collections.emptySet();
        boolean nativeSeed;
        boolean invalid;
        Session(Object presenter, String channel, LiveEmoteAutocomplete.Lifecycle lifecycle) {
            this.presenter = new WeakReference<>(presenter); this.channel = channel; this.lifecycle = lifecycle;
            this.viewer = NativePickerBridge.viewer(presenter);
        }
        boolean valid() {
            if (!main() || invalid || presenter.get() == null || !lifecycle.matches(lifecycle.input.get())) return false;
            if (!java.util.Objects.equals(viewer, NativePickerBridge.viewer(presenter.get()))) return false;
            LiveEmoteAutocomplete.Lifecycle unique = LiveEmoteAutocomplete.uniqueLifecycle(channel);
            return unique != null && unique.controller.get() == lifecycle.controller.get();
        }
        void invalidate() { invalid = true; nativeCodes = Collections.emptySet(); SESSIONS.remove(id); }
    }

    /** Called at H2 creation, on main, before any native subscription is made. */
    public static Object bind(Object context) {
        try {
            if (!main()) return null;
            Object presenter = NativePickerBridge.presenter(context);
            String channel = NativePickerBridge.channel(context);
            if (presenter == null) return null;
            WeakReference<Session> previous = PRESENTERS.remove(presenter);
            if (previous != null && previous.get() != null) previous.get().invalidate();
            if (!PickerSyntheticId.positive(channel)) return null;
            LiveEmoteAutocomplete.Lifecycle lifecycle = LiveEmoteAutocomplete.uniqueLifecycle(channel);
            if (lifecycle == null) return null;
            Session session = new Session(presenter, channel, lifecycle);
            SESSIONS.entrySet().removeIf(entry -> entry.getValue().get() == null);
            SESSIONS.put(session.id, new WeakReference<>(session));
            PRESENTERS.put(presenter, new WeakReference<>(session));
            return session;
        } catch (Throwable ignored) { return null; }
    }

    public static void open(Object upstream, Object emitter, Object binding) {
        Stream stream = new Stream((Session) binding, emitter);
        NativePickerBridge.subscribe(upstream, emitter, stream);
    }

    /** One downstream subscription owns both the native subscription and notification listeners. */
    public static final class Stream {
        private final Session session;
        private final Object emitter;
        private volatile boolean disposed;
        private Object subscription, original;
        private EmoteCatalogEvents.Subscription catalogs, lifecycle, history;
        private SharedPreferences accountPreferences;
        private SharedPreferences.OnSharedPreferenceChangeListener accountListener;
        Stream(Session session, Object emitter) {
            this.session = session; this.emitter = emitter;
            onMain(() -> {
                if (disposed) return;
                try {
                    catalogs = EmoteCatalogEvents.subscribe((provider, channel, revision) -> {
                        if (channel == null || session.channel.equals(channel)) onMain(this::refresh);
                    });
                    lifecycle = LiveEmoteAutocomplete.observeLifecycle(this::refresh);
                    history = EmoteUsageStore.observe(() -> onMain(this::refresh));
                    Object prefs = NativePickerBridge.accountPreferences(session.presenter.get());
                    if (prefs instanceof SharedPreferences) {
                        accountPreferences = (SharedPreferences) prefs;
                        accountListener = (value, key) -> onMain(this::refresh);
                        accountPreferences.registerOnSharedPreferenceChangeListener(accountListener);
                    }
                } catch (Throwable ignored) { session.invalidate(); }
            });
        }
        public synchronized void subscribed(Object value) {
            if (disposed || subscription != null) { NativePickerBridge.cancel(value); return; }
            subscription = value;
            NativePickerBridge.request(value);
        }
        public void next(Object pair) { onMain(() -> { if (!disposed) { original = pair; refresh(); } }); }
        private void refresh() {
            if (disposed) return;
            Object output = original;
            try {
                if (!session.valid()) {
                    session.invalidate();
                    stopListeners();
                } else if (original != null) output = augment(session, original);
            } catch (Throwable ignored) { output = original; }
            // The native Pair is retained as the seed; provider-only errors do not terminate Twitch.
            if (!disposed && output != null) NativePickerBridge.emit(emitter, output);
        }
        public void error(Throwable failure) {
            onMain(() -> { if (!disposed) { NativePickerBridge.error(emitter, failure); dispose(); } });
        }
        public void complete() {
            onMain(() -> { if (!disposed) { NativePickerBridge.complete(emitter); dispose(); } });
        }
        private void stopListeners() {
            if (catalogs != null) { catalogs.dispose(); catalogs = null; }
            if (lifecycle != null) { lifecycle.dispose(); lifecycle = null; }
            if (history != null) { history.dispose(); history = null; }
            if (accountPreferences != null) {
                accountPreferences.unregisterOnSharedPreferenceChangeListener(accountListener);
                accountPreferences = null; accountListener = null;
            }
        }
        public void dispose() {
            Object cancel;
            synchronized (this) { if (disposed) return; disposed = true; cancel = subscription; subscription = null; }
            if (cancel != null) NativePickerBridge.cancel(cancel);
            onMain(() -> { stopListeners(); session.invalidate(); original = null; });
        }
        public boolean isDisposed() { return disposed; }
    }

    private static Object augment(Session session, Object original) {
        List<?> nativeSections = NativePickerBridge.sections(original);
        List<Object> sections = new ArrayList<>(nativeSections.size() + 6);
        Set<String> nativeCodes = new HashSet<>();
        for (Object section : nativeSections) {
            List<?> cells = NativePickerBridge.cells(section);
            List<Object> clean = null;
            boolean frequent = NativePickerBridge.frequent(section);
            for (Object cell : cells) {
                String id = NativePickerBridge.id(cell);
                boolean stale = frequent && id != null && id.startsWith(PickerSyntheticId.PREFIX);
                if (stale && clean == null) clean = new ArrayList<>();
                if (!stale) {
                    String code = NativePickerBridge.code(cell);
                    if (code != null && (id == null || !id.startsWith(PickerSyntheticId.PREFIX))) nativeCodes.add(code);
                }
            }
            if (clean != null) {
                for (Object cell : cells) {
                    String id = NativePickerBridge.id(cell);
                    if (id == null || !id.startsWith(PickerSyntheticId.PREFIX)) clean.add(cell);
                }
                sections.add(NativePickerBridge.copySection(section, clean));
            } else sections.add(section);
        }
        session.nativeCodes = Collections.unmodifiableSet(nativeCodes);
        session.nativeSeed = true;
        List<LiveEmotePicker.Entry> entries = LiveEmotePicker.snapshot(session.channel, nativeCodes);
        List<LiveEmotePicker.Entry> frequent = EmoteUsageStore.visible(session.viewer, session.channel, entries);
        if (!frequent.isEmpty()) {
            List<Object> saved = new ArrayList<>();
            for (LiveEmotePicker.Entry entry : frequent) {
                String id = PickerSyntheticId.encode(session.id, entry.channel, entry.provider,
                        entry.channel ? session.channel : "0", entry.id, entry.code);
                if (id != null) saved.add(NativePickerBridge.cell(id, entry.code, entry.animated));
            }
            boolean found = false;
            for (int i = 0; i < sections.size(); i++) if (NativePickerBridge.frequent(sections.get(i))) {
                List<Object> combined = new ArrayList<>(NativePickerBridge.cells(sections.get(i)));
                combined.addAll(saved);
                sections.set(i, NativePickerBridge.copySection(sections.get(i), combined));
                found = true; break;
            }
            if (!found && !saved.isEmpty()) sections.add(0, NativePickerBridge.frequentSection(saved));
        }
        int group = -1;
        List<Object> cells = new ArrayList<>();
        for (LiveEmotePicker.Entry entry : entries) {
            if (entry.group() != group) {
                if (!cells.isEmpty()) sections.add(NativePickerBridge.section(title(group), group < 3, cells));
                group = entry.group(); cells = new ArrayList<>();
            }
            String id = PickerSyntheticId.encode(session.id, entry.channel, entry.provider,
                    entry.channel ? session.channel : "0", entry.id, entry.code);
            if (id != null) cells.add(NativePickerBridge.cell(id, entry.code, entry.animated));
        }
        if (!cells.isEmpty()) sections.add(NativePickerBridge.section(title(group), group < 3, cells));
        return session.valid() ? NativePickerBridge.pair(original, sections) : original;
    }

    private static int title(int group) {
        return ResUtil.getResourceId(titleResource(group));
    }
    private static Res.strings titleResource(int group) {
        switch (group) {
            case 0: return Res.strings.bttv_emote_picker_channel_7tv;
            case 1: return Res.strings.bttv_emote_picker_channel_bttv;
            case 2: return Res.strings.bttv_emote_picker_channel_ffz;
            case 3: return Res.strings.bttv_emote_picker_global_7tv;
            case 4: return Res.strings.bttv_emote_picker_global_bttv;
            default: return Res.strings.bttv_emote_picker_global_ffz;
        }
    }

    private static Session session(PickerSyntheticId id) {
        WeakReference<Session> ref = id == null ? null : SESSIONS.get(id.session);
        Session session = ref == null ? null : ref.get();
        return session != null && session.valid() ? session : null;
    }

    private static LiveEmotePicker.Entry resolve(Session session, PickerSyntheticId id) {
        if (session == null || id == null || id.session != session.id
                || id.channel && !session.channel.equals(id.channelId)) return null;
        LiveEmotePicker.Entry entry = LiveEmotePicker.winner(session.channel, session.nativeCodes, id.code);
        return entry != null && entry.id.equals(id.id) && entry.provider.equals(id.provider)
                && entry.channel == id.channel ? entry : null;
    }

    public static String url(String value) {
        if (value == null || !value.startsWith(PickerSyntheticId.PREFIX)) return null;
        try {
            if (!main()) return "";
            PickerSyntheticId id = PickerSyntheticId.decode(value);
            LiveEmotePicker.Entry entry = resolve(session(id), id);
            return entry == null ? "" : entry.url;
        } catch (Throwable ignored) { return ""; }
    }

    public static boolean click(Object presenter, String value) {
        if (value == null || !value.startsWith(PickerSyntheticId.PREFIX)) return false;
        try {
            if (!main()) return true;
            PickerSyntheticId id = PickerSyntheticId.decode(value);
            Session session = session(id);
            LiveEmotePicker.Entry entry = resolve(session, id);
            if (entry != null && session.presenter.get() == presenter) {
                String identity = PENDING.register(entry.code, session, value);
                NativePickerBridge.dispatch(presenter, identity, value);
            }
        } catch (Throwable ignored) { }
        return true;
    }

    /** Null means no trustworthy current native eligibility snapshot, not an empty native catalog. */
    static Set<String> usageNativeCodes(EditText input, String viewer, String channel) {
        if (!main()) return null;
        Session found = null;
        for (WeakReference<Session> ref : SESSIONS.values()) {
            Session s = ref.get();
            if (s != null && s.valid() && s.nativeSeed && channel.equals(s.channel)
                    && viewer.equals(s.viewer) && s.lifecycle.matches(input)) {
                if (found != null) return null;
                found = s;
            }
        }
        return found == null ? null : found.nativeCodes;
    }

    public static boolean insert(Object composer, String identity) {
        PickerInsertion.Pending<Session> pending = PENDING.find(identity);
        if (pending == null) return false;
        synchronized (pending) {
            if (pending.consumed) return true;
            pending.consumed = true;
        }
        if (!main()) return true; // Recognized requests never fall into off-main native editing.
        try {
            Session session = pending.owner.get();
            PickerSyntheticId id = PickerSyntheticId.decode(pending.syntheticId);
            if (session == null || !session.valid()) return true;
            EditText input = NativePickerBridge.input(composer);
            if (!session.lifecycle.matches(input)) return true;
            LiveEmotePicker.Entry entry = resolve(session, id);
            if (entry == null) return true;
            Editable text = input.getText();
            PickerInsertion.Plan plan = PickerInsertion.plan(text, input.getSelectionStart(), input.getSelectionEnd(), entry.code);
            if (plan == null) return true;
            text.replace(plan.start, plan.end, plan.replacement);
            input.setSelection(Math.min(plan.cursor, text.length()));
        } catch (Throwable ignored) { }
        return true;
    }
    private PickerRuntime() {}
}
