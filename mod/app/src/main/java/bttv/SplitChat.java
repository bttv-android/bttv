package bttv;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import bttv.settings.Settings;
import tv.twitch.android.app.core.ThemeManager;
import tv.twitch.android.shared.chat.adapter.item.ChatMessageViewHolder;
import tv.twitch.android.shared.chat.chomments.ChommentRecyclerItem.ChommentItemViewHolder;
import tv.twitch.android.shared.chat.messagefactory.adapteritem.UserNoticeRecyclerItem.UserNoticeViewHolder;

public class SplitChat {
    private final static String TAG = "LBTTVSplitChat";

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

    public static void setBackgroundColor(int position, RecyclerView.ViewHolder viewHolder) {
        if (!ResUtil.getBooleanFromSettings(Settings.SplitChatEnabled)) {
            return;
        }

        // TwitchAdapter is used all over the place,
        // so make sure not to do anything when
        // not a ViewHolder used in Chat
        if (
            !(viewHolder instanceof ChatMessageViewHolder)
            && !(viewHolder instanceof ChommentItemViewHolder)
            && !(viewHolder instanceof UserNoticeViewHolder)
        ) {
            Log.i(TAG, "viewHolder is not known: " + viewHolder.toString());
            return;
        }
        View view = viewHolder.itemView;
        Context context = view.getContext();

        // make sure we only change chat message items
        // Other elements like redeems or Sub Anniversaries are ConstraintLayouts

        // If view is a LinearLayout, check the childrens to find chat_message_item
        if(view instanceof LinearLayout) {
            Log.d(TAG, "view is LinearLayout: " + view.toString());
            LinearLayout linearLayout = (LinearLayout) view;
            for (int j = 0; j < linearLayout.getChildCount(); j++) {
                View nestedChild = linearLayout.getChildAt(j);
                if (nestedChild.getId() == ResUtil.getResourceId(context, "chat_message_item", "id")) {
                    Log.d(TAG, "found chat_message_item: " + nestedChild.toString());
                    view = nestedChild;
                    // Increase width of linearLayout to match parent to color the whole item
                    linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    break;
                }
            }
        }
        boolean hasChatMessageId = view.getId() == ResUtil.getResourceId(context, "chat_message_item", "id");
        boolean hasChommentRootId = view.getId() == ResUtil.getResourceId(context, "chomment_root_view", "id");

        if (!(hasChatMessageId || hasChommentRootId)) {
            Log.i(TAG, "view skipped, as it's not a chat message or chomment, " + viewHolder.toString() + " ID: " + view.getId() + " View: " + view.toString() + " Expected ID: " + ResUtil.getResourceId(context, "chat_message_item", "id") + " or " + ResUtil.getResourceId(context, "chomment_root_view", "id"));
            reset(view);
            return;
        }

        int color = ThemeManager.Companion.isNightModeEnabled(context)
                ? ResUtil.getColorValue("material_grey_900")
                : ResUtil.getColorValue("material_grey_300");

        boolean doChange = shouldTintBG(position);

        // for some reason we can't set the background for the whole view for Chomments (VODs)
        // so we just highlight the TextView (first child)
        if (hasChommentRootId) {
            LinearLayout linearLayout = (LinearLayout) view;
            view = linearLayout.getChildAt(0);
        }

        if (doChange) {
            view.setBackgroundColor(color);
        } else {
            reset(view);
        }

        //
        // fix width
        //

        // MATCH_PARENT is default for Chomments, WRAP_CONTENT for Comments
        boolean matchParent = doChange || viewHolder instanceof ChommentItemViewHolder;
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = matchParent ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        view.setLayoutParams(layoutParams);
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
        Drawable background = view.getBackground();
        if (background == null) {
            return;
        }
        if (!(background instanceof ColorDrawable)) {
            return;
        }

        ColorDrawable colorDrawable = (ColorDrawable) background;
        if (colorDrawable.getColor() == ResUtil.getColorValue("material_grey_900")
            || colorDrawable.getColor() == ResUtil.getColorValue("material_grey_300")) {
            view.setBackground(null);
        }
    }

}
