package bttv;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import bttv.settings.Settings;
import tv.twitch.android.adapters.social.MessageRecyclerItem.ChatMessageViewHolder;
import tv.twitch.android.shared.chat.chomments.ChommentRecyclerItem.ChommentItemViewHolder;
import tv.twitch.android.shared.chat.messagefactory.adapteritem.UserNoticeRecyclerItem.UserNoticeViewHolder;

public class SplitChat {
    private final static String TAG = "LBTTVSplitChat";

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

        // make sure we only change chat message items
        boolean hasChatMessageId = view.getId() == ResUtil.getResourceId(view.getContext(), "chat_message_item", "id");
        boolean hasChommentRootId = view.getId() == ResUtil.getResourceId(view.getContext(), "chomment_root_view", "id");

        if (!hasChatMessageId && !hasChommentRootId) {
            Log.i(TAG, "view skipped, as it's not a chat message or chomment" + viewHolder.toString());
            reset(view);
            return;
        }

        int color = isDarkThemeEnabled()
                ? ResUtil.getColorValue("material_grey_900")
                : ResUtil.getColorValue("material_grey_300");

        boolean doChange = position % 2 == 1;

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

    private static boolean isDarkThemeEnabled() {
        return Data.ctx
            .getSharedPreferences("tv.twitch.bttvandroid.app_preferences", 0)
            .getBoolean("dark_theme_enabled", false);
    }
}
