package bttv;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.util.Map;
import java.util.WeakHashMap;

import bttv.settings.Settings;

public class SplitChat {
    /**
     * The Buffer backing the Chat has a fixed Capacity and is cleared when it overflows;
     * Old messages get removed. This means we can't rely on the position parameter in
     * onBindViewHolder() to determine the background color:
     *
     * Example:
     *   Capacity is 3.
     *   1 new message arrive:
     * - Old Message | bound with position 0 (even) (will be removed)
     * - Old Message | bound with position 1 (odd)
     * - Old Message | bound with position 2 (even)
     * - New Message | binding with position 2 (even)
     *
     * So now two even elements are next to each other.
     *
     * To counter this we continue to count the "absolute" position by keeping track of the
     * total removed messages.
     *
     * totalPosition = positionInAdapter + totalRemovedMessages
     */
    private static int totalRemovedMessages = 0;
    private static final Map<View, Drawable> originalBackgrounds = new WeakHashMap<>();

    public static void setBackgroundColor(int position, View view) {
        Context context = view.getContext();

        // g1d is the Twitch 31 ordinary live-chat holder. Keep an ID guard at
        // the Java boundary as well so no unrelated RecyclerView root is tinted.
        int chatMessageItemResId = ResUtil.getResourceId(context, "chat_message_item", "id");
        if (chatMessageItemResId == 0 || view.findViewById(chatMessageItemResId) == null) {
            return;
        }

        if (!ResUtil.getBooleanFromSettings(Settings.SplitChatEnabled) || position < 0) {
            reset(view);
            return;
        }

        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        int color = nightMode == Configuration.UI_MODE_NIGHT_YES
                ? ResUtil.getColorValue("material_grey_900")
                : ResUtil.getColorValue("material_grey_300");

        if (shouldTintBG(position)) {
            if (!originalBackgrounds.containsKey(view)) {
                originalBackgrounds.put(view, view.getBackground());
            }
            view.setBackgroundColor(color);
        } else {
            reset(view);
        }
    }

    /** called after Twitch removes the first N Messages from the buffer */
    public static void removedNMessages(int n) {
        totalRemovedMessages += n;
    }

    private static boolean shouldTintBG(int positionInAdapter) {
        // see comment on totalRemovedMessages to see why we do this
        int totalPosition = positionInAdapter + totalRemovedMessages;
        return totalPosition % 2 == 1;
    }

    private static void reset(View view) {
        if (originalBackgrounds.containsKey(view)) {
            view.setBackground(originalBackgrounds.remove(view));
        }
    }

}
