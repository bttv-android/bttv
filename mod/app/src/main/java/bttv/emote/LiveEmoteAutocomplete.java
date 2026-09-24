package bttv.emote;

import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.util.WeakHashMap;

/** Stage 5D-B1: per-composer lifecycle/identity only; no text or suggestion handling. */
public final class LiveEmoteAutocomplete {
    private static final String TAG = "BTTV-Autocomplete";
    // Main-thread-only. Values contain no View/Context/owner reference back to the weak key.
    // The view's attach-state listener keeps its controller alive while that view exists.
    private static final WeakHashMap<EditText, Controller> CONTROLLERS = new WeakHashMap<>();

    private LiveEmoteAutocomplete() {}

    public static void attach(EditText input) {
        try {
            if (input == null || Looper.myLooper() != Looper.getMainLooper()) {
                return;
            }
            Controller existing = CONTROLLERS.get(input);
            if (existing != null) {
                if (!existing.duplicateReported) {
                    existing.duplicateReported = true;
                    existing.log("ATTACH duplicate ignored");
                }
                return;
            }
            Controller controller = new Controller(input);
            input.addOnAttachStateChangeListener(controller);
            CONTROLLERS.put(input, controller);
            controller.log("ATTACH new active=" + controller.active);
        } catch (Throwable failure) {
            failure("attach", failure);
        }
    }

    public static void setChannel(EditText input, String channelId) {
        try {
            if (input == null || Looper.myLooper() != Looper.getMainLooper()) {
                return;
            }
            Controller controller = CONTROLLERS.get(input);
            if (controller == null) {
                // Unexpected ordering is safe; later constructor attachment is idempotent.
                attach(input);
                controller = CONTROLLERS.get(input);
                if (controller == null) {
                    return;
                }
            }
            String next = validChannelId(channelId) ? channelId : null;
            boolean changed = controller.channelId == null
                    ? next != null : !controller.channelId.equals(next);
            if (!changed && controller.channelAssigned) {
                return;
            }
            String previous = controller.channelId;
            controller.channelId = next;
            controller.channelAssigned = true;
            if (changed) {
                controller.generation++;
            }
            controller.log("CHANNEL " + previous + " -> " + next);
        } catch (Throwable failure) {
            failure("setChannel", failure);
        }
    }

    // Same positive-decimal contract as Stage 5C, without initializing any catalog/network code.
    private static boolean validChannelId(String id) {
        if (id == null || id.isEmpty() || id.length() > 32) {
            return false;
        }
        boolean positive = false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            positive |= c != '0';
        }
        return positive;
    }

    private static final class Controller implements View.OnAttachStateChangeListener {
        final int inputIdentity;
        String channelId;
        long generation;
        boolean channelAssigned;
        boolean duplicateReported;
        boolean active;
        boolean previouslyAttached;

        Controller(EditText input) {
            inputIdentity = System.identityHashCode(input);
            active = input.isAttachedToWindow();
            previouslyAttached = active;
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            try {
                if (active) {
                    return;
                }
                active = true;
                log(previouslyAttached ? "REATTACH" : "ATTACHED");
                previouslyAttached = true;
            } catch (Throwable failure) {
                failure("reattach", failure);
            }
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            try {
                if (!active) {
                    return;
                }
                active = false;
                // Invalidate any future generation-bound work. B1 owns no UI/work or watcher.
                // Retain only channel identity for this same view's possible reattachment.
                generation++;
                log("DETACH");
            } catch (Throwable failure) {
                failure("detach", failure);
            }
        }

        void log(String event) {
            logSafely(event + " input=" + Integer.toHexString(inputIdentity)
                    + " controller=" + Integer.toHexString(System.identityHashCode(this))
                    + " channel=" + channelId + " generation=" + generation);
        }
    }

    private static void failure(String operation, Throwable failure) {
        logSafely("unavailable operation=" + operation + " error="
                + failure.getClass().getSimpleName());
    }

    private static void logSafely(String message) {
        try {
            Log.i(TAG, message);
        } catch (Throwable ignored) {
            // Diagnostics must never affect Twitch chat.
        }
    }
}
