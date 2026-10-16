package bttv.emote;

import android.util.Log;
import android.view.View;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The react native chat gives every emote a square slot, emotes that are wider than high
 * would be shrunk to fit. Three parts work together to show them at full height:
 * - IrcEmotes puts an invisible marker behind a wide emote, its length tells how wide the emote is,
 *   and gives the emote an id that starts with EmoteUrlUtil.WIDE_PREFIX
 * - the text layout reserves a wider slot, if the inline view is followed by the marker
 * - the image view of the emote is made as wide as the slot
 */
public class WideEmotes {
    private static final String TAG = "LBTTVWideEmotes";
    // word joiner, has no width and is not a place to break a line
    private static final char MARKER = '⁠';
    private static final float MIN_RATIO = 1.2f;
    private static final float MAX_RATIO = 4f;
    private static final int STEPS_PER_HEIGHT = 4;
    private static final String EMOTE_URL_MARKER = "/emoticons/v2/";

    // image views that currently show a wide emote
    private static final Map<View, Float> wideViews = Collections.synchronizedMap(new WeakHashMap<View, Float>());
    private static Method getViewExists;
    private static Method getView;
    private static int logged = 0;
    private static final String CHAT_EMOTE_TEST_ID = "chat-emote-";
    // the emote card has a square slot for the emote, the text is next to it
    private static final String EMOTE_CARD_TEST_ID = "chat-emote-card";
    private static int testIdKey = 0;

    /** marker to put behind the emote in the message, empty if it is not wide */
    public static String marker(Emote emote) {
        int steps = steps(emote.aspectRatio);
        if (steps == 0) {
            return "";
        }
        StringBuilder marker = new StringBuilder(steps);
        for (int i = 0; i < steps; i++) {
            marker.append(MARKER);
        }
        return marker.toString();
    }

    private static int steps(float aspectRatio) {
        if (aspectRatio < MIN_RATIO) {
            return 0;
        }
        float ratio = Math.min(aspectRatio, MAX_RATIO);
        return Math.max(1, Math.round((ratio - 1f) * STEPS_PER_HEIGHT));
    }

    private static float ratio(int steps) {
        return 1f + (float) steps / STEPS_PER_HEIGHT;
    }

    /**
     * @param text text an inline view is part of
     * @param end index behind the inline view
     * @param width width of the slot for the inline view
     * @return width to reserve
     */
    public static int inlineViewWidth(CharSequence text, int end, int width) {
        if (text == null || end < 0 || end >= text.length() || text.charAt(end) != MARKER) {
            return width;
        }
        int steps = 0;
        while (end + steps < text.length() && text.charAt(end + steps) == MARKER) {
            steps++;
        }
        int newWidth = Math.round(width * ratio(steps));
        if (logged++ < 5) {
            Log.d(TAG, "inlineViewWidth: " + width + " -> " + newWidth);
        }
        return newWidth;
    }

    /**
     * @param view react native image view
     * @param sources ReadableArray of maps with an "uri"
     */
    public static void onImageViewSource(Object view, Object sources) throws Exception {
        if (!(view instanceof View)) {
            return;
        }
        Float ratio = null;
        String uri = firstUri(sources);
        if (uri != null) {
            int start = uri.indexOf(EMOTE_URL_MARKER);
            if (start != -1) {
                start += EMOTE_URL_MARKER.length();
                int end = uri.indexOf('/', start);
                String id = end == -1 ? uri.substring(start) : uri.substring(start, end);
                // only where the slot is wider, e.g. not in messages the chat shows before they are sent
                String realId = id.startsWith(EmoteUrlUtil.WIDE_PREFIX) ? EmoteUrlUtil.extractBTTVId(id) : null;
                Emote emote = realId == null ? null : Emotes.getEmoteById(realId);
                int steps = emote == null ? 0 : steps(emote.aspectRatio);
                if (steps > 0) {
                    ratio = ratio(steps);
                }
            }
        }
        if (ratio == null) {
            // views get reused
            if (!wideViews.isEmpty()) {
                wideViews.remove(view);
            }
        } else {
            wideViews.put((View) view, ratio);
        }
    }

    // react native stores the testID of a view as a tag
    private static boolean isChatEmote(View view) {
        if (testIdKey == 0) {
            testIdKey = view.getResources().getIdentifier("react_test_id", "id", view.getContext().getPackageName());
            if (testIdKey == 0) {
                testIdKey = -1;
            }
        }
        if (testIdKey == -1) {
            return false;
        }
        Object testId = view.getTag(testIdKey);
        if (!(testId instanceof String)) {
            return false;
        }
        String id = (String) testId;
        return id.startsWith(CHAT_EMOTE_TEST_ID) && !id.startsWith(EMOTE_CARD_TEST_ID);
    }

    private static String firstUri(Object sources) throws Exception {
        if (sources == null) {
            return null;
        }
        Class<?> array = Class.forName("com.facebook.react.bridge.ReadableArray");
        if ((Integer) array.getMethod("size").invoke(sources) != 1) {
            return null;
        }
        Object map = array.getMethod("getMap", int.class).invoke(sources, 0);
        if (map == null) {
            return null;
        }
        Class<?> readableMap = Class.forName("com.facebook.react.bridge.ReadableMap");
        if (!(Boolean) readableMap.getMethod("hasKey", String.class).invoke(map, "uri")) {
            return null;
        }
        return (String) readableMap.getMethod("getString", String.class).invoke(map, "uri");
    }

    /**
     * @param manager react native SurfaceMountingManager
     * @param tag tag of the view that gets its layout
     * @return width the view should get
     */
    public static int layoutWidth(Object manager, int tag, int width, int height) throws Exception {
        if (wideViews.isEmpty()) {
            return width;
        }
        if (getView == null) {
            getViewExists = manager.getClass().getMethod("getViewExists", int.class);
            getView = manager.getClass().getMethod("getView", int.class);
        }
        if (!(Boolean) getViewExists.invoke(manager, tag)) {
            return width;
        }
        View view = (View) getView.invoke(manager, tag);
        Float ratio = wideViews.get(view);
        if (ratio == null || !isChatEmote(view)) {
            // the emote menu and the autocomplete show emotes in square slots
            return width;
        }
        return Math.round(height * ratio);
    }
}
