package bttv.emote;

import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import bttvcompat.NativeAutocompleteBridge;

/** Native colon autocomplete integration. Twitch owns filtering, rows, completion and sending. */
public final class LiveEmoteAutocomplete {
    private static final String TAG = "BTTV-Autocomplete";
    // Main-thread-only. Neither registry can root an input, Activity or presenter.
    private static final WeakHashMap<EditText, WeakReference<Controller>> CONTROLLERS =
            new WeakHashMap<>();
    private static final WeakHashMap<Object, WeakReference<Controller>> ADAPTERS =
            new WeakHashMap<>();

    private LiveEmoteAutocomplete() {}

    private static boolean mainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private static Controller controller(EditText input) {
        WeakReference<Controller> reference = CONTROLLERS.get(input);
        return reference == null ? null : reference.get();
    }

    public static void attach(EditText input) {
        try {
            if (input == null || !mainThread()) return;
            Controller existing = controller(input);
            if (existing != null) {
                if (!existing.duplicateReported) {
                    existing.duplicateReported = true;
                    existing.log("ATTACH duplicate ignored");
                }
                return;
            }
            Controller created = new Controller(input);
            input.addOnAttachStateChangeListener(created);
            CONTROLLERS.put(input, new WeakReference<>(created));
            created.log("ATTACH new active=" + created.active);
        } catch (Throwable failure) {
            failure("attach", failure);
        }
    }

    public static void setChannel(EditText input, String channelId) {
        try {
            if (input == null || !mainThread()) return;
            Controller owner = controller(input);
            if (owner == null) {
                attach(input);
                owner = controller(input);
                if (owner == null) return;
            }
            String next = validChannelId(channelId) ? channelId : null;
            boolean changed = owner.channelId == null
                    ? next != null : !owner.channelId.equals(next);
            if (!changed && owner.channelAssigned) return;
            String previous = owner.channelId;
            owner.channelId = next;
            owner.channelAssigned = true;
            if (changed) owner.generation++;
            owner.invalidate();
            owner.log("CHANNEL " + previous + " -> " + next);
        } catch (Throwable failure) {
            failure("setChannel", failure);
        }
    }

    public static void bindNativeAdapter(Object adapter, EditText input) {
        try {
            if (adapter == null || !(input instanceof MultiAutoCompleteTextView) || !mainThread()) return;
            if (((MultiAutoCompleteTextView) input).getAdapter() != adapter) return;
            Controller owner = controller(input);
            if (owner == null) {
                attach(input);
                owner = controller(input);
                if (owner == null) return;
            }
            Object previous = owner.adapterRef == null ? null : owner.adapterRef.get();
            if (previous == adapter) return;
            owner.invalidate();
            if (previous != null) ADAPTERS.remove(previous);
            owner.adapterRef = new WeakReference<>(adapter);
            ADAPTERS.put(adapter, new WeakReference<>(owner));
            owner.log("NATIVE bound");
        } catch (Throwable failure) {
            failure("bindNativeAdapter", failure);
        }
    }

    /**
     * Called at Filter.publishResults entry (UI thread), including native zero-result queries.
     * Object avoids Java's protected FilterResults type and obfuscated Twitch compile stubs.
     */
    public static void mergeNativeResults(Object adapter, CharSequence query, Object results) {
        try {
            if (!mainThread() || adapter == null || query == null || results == null) return;
            WeakReference<Controller> reference = ADAPTERS.get(adapter);
            Controller owner = reference == null ? null : reference.get();
            if (owner == null || !owner.active || !validChannelId(owner.channelId)
                    || owner.adapterRef == null || owner.adapterRef.get() != adapter) return;
            EditText input = owner.inputRef.get();
            if (!(input instanceof MultiAutoCompleteTextView) || !input.isAttachedToWindow()
                    || !input.hasFocus()
                    || ((MultiAutoCompleteTextView) input).getAdapter() != adapter
                    || !NativeAutocompleteBridge.isEmoteAdapter(adapter)) return;
            EmoteAutocompleteLogic.Token token = EmoteAutocompleteLogic.token(
                    input.getText(), input.getSelectionStart(), input.getSelectionEnd());
            if (token == null || !query.toString().equals(":" + token.prefix)) return;

            final long generation = owner.generation;
            final String channel = owner.channelId;
            int count = NativeAutocompleteBridge.count(results);
            Object values = NativeAutocompleteBridge.values(results);
            if (count < 0 || (values == null && count != 0)
                    || (values != null && !(values instanceof ArrayList))) return;
            List<?> nativeItems = values == null ? new ArrayList<>() : (List<?>) values;
            if (nativeItems.size() != count) return;
            ArrayList<EmoteAutocompleteLogic.Suggestion> nativeMatches = new ArrayList<>();
            for (Object item : nativeItems) {
                String code = NativeAutocompleteBridge.code(item);
                if (!EmoteAutocompleteLogic.validCode(code)) return;
                nativeMatches.add(EmoteAutocompleteLogic.Suggestion.nativeItem(code, item));
            }
            List<EmoteAutocompleteLogic.Suggestion> ranked = EmoteAutocompleteLogic.merge(
                    token.prefix, nativeMatches, EmoteAutocompleteCatalog.find(channel, token.prefix));
            ArrayList<Object> merged = new ArrayList<>(ranked.size());
            IdentityHashMap<Object, String> images = new IdentityHashMap<>();
            for (EmoteAutocompleteLogic.Suggestion candidate : ranked) {
                Object item = candidate.nativeItem;
                if (item == null) {
                    String id = AutocompleteSyntheticId.encode(candidate.provider, candidate.id);
                    if (id == null || !AutocompleteSyntheticId.validImageUrl(candidate.url)) return;
                    item = NativeAutocompleteBridge.create(id, candidate.code,
                            "gif".equals(candidate.imageType));
                    if (item == null) return;
                    images.put(item, candidate.url);
                }
                merged.add(item);
            }
            if (!owner.active || generation != owner.generation || !channel.equals(owner.channelId)
                    || !token.same(EmoteAutocompleteLogic.token(input.getText(),
                            input.getSelectionStart(), input.getSelectionEnd()))) return;
            String event = "NATIVE merge native=" + count + " thirdParty=" + images.size()
                    + " total=" + merged.size();
            // All fallible construction/validation precedes these field-only commits.
            NativeAutocompleteBridge.publish(results, merged);
            // ot1 keeps its old (hidden) dataset when count is zero. Retain its row
            // identities until replacement so channel/detach invalidation can still remove them.
            if (!merged.isEmpty()) owner.images = images;
            owner.log(event);
        } catch (Throwable failure) {
            // Never expose partial results, provider failures or linkage errors to Twitch input.
            failure("mergeNativeResults", failure);
        }
    }

    /** Only constructed rows in a live controller's current publication may supply custom URLs. */
    public static String resolveNativeImageUrl(Object suggestion) {
        try {
            if (!mainThread() || suggestion == null
                    || AutocompleteSyntheticId.decode(NativeAutocompleteBridge.id(suggestion)) == null) return null;
            for (WeakReference<Controller> reference : CONTROLLERS.values()) {
                Controller owner = reference.get();
                if (owner != null && owner.active) {
                    String url = owner.images.get(suggestion);
                    if (url != null) return url;
                }
            }
        } catch (Throwable failure) {
            failure("resolveNativeImageUrl", failure);
        }
        return null;
    }

    private static boolean validChannelId(String id) {
        if (id == null || id.isEmpty() || id.length() > 32) return false;
        boolean positive = false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') return false;
            positive |= c != '0';
        }
        return positive;
    }

    private static final class Controller implements View.OnAttachStateChangeListener {
        final int inputIdentity;
        final WeakReference<EditText> inputRef;
        WeakReference<Object> adapterRef;
        // Per-publication row metadata, not a provider cache. At most ten entries.
        Map<Object, String> images = new IdentityHashMap<>();
        String channelId;
        long generation;
        boolean channelAssigned, duplicateReported, active, previouslyAttached;

        Controller(EditText input) {
            inputIdentity = System.identityHashCode(input);
            inputRef = new WeakReference<>(input);
            active = input.isAttachedToWindow();
            previouslyAttached = active;
        }

        void invalidate() {
            if (images.isEmpty()) return;
            try {
                EditText input = inputRef.get();
                Object adapter = adapterRef == null ? null : adapterRef.get();
                if (input instanceof MultiAutoCompleteTextView
                        && ((MultiAutoCompleteTextView) input).getAdapter() == adapter) {
                    ((MultiAutoCompleteTextView) input).dismissDropDown();
                }
                if (adapter != null) NativeAutocompleteBridge.removeItems(adapter, images.keySet());
            } catch (Throwable failure) {
                failure("invalidate", failure);
            } finally {
                images.clear();
            }
        }

        @Override public void onViewAttachedToWindow(View view) {
            try {
                if (active) return;
                active = true;
                log(previouslyAttached ? "REATTACH" : "ATTACHED");
                previouslyAttached = true;
            } catch (Throwable failure) {
                failure("reattach", failure);
            }
        }

        @Override public void onViewDetachedFromWindow(View view) {
            try {
                if (!active) return;
                active = false;
                generation++;
                invalidate();
                log("DETACH");
            } catch (Throwable failure) {
                failure("detach", failure);
            }
        }

        void log(String event) {
            try {
                logSafely(event + " input=" + Integer.toHexString(inputIdentity)
                        + " controller=" + Integer.toHexString(System.identityHashCode(this))
                        + " channel=" + channelId + " generation=" + generation);
            } catch (Throwable ignored) { }
        }
    }

    private static void failure(String operation, Throwable failure) {
        logSafely("unavailable operation=" + operation + " error=" + failure.getClass().getSimpleName());
    }

    private static void logSafely(String message) {
        try { Log.i(TAG, message); } catch (Throwable ignored) { }
    }
}
