package bttv;

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
        int color = isDarkThemeEnabled()
                ? ResUtil.getColorValue("material_grey_900")
                : ResUtil.getColorValue("material_grey_300");

        boolean doChange = position % 2 == 1;

        // fix VODs
        if (view.getId() == ResUtil.getResourceId(Data.ctx, "chomment_root_view", "id")) {
            LinearLayout linearLayout = (LinearLayout) view;
            view = linearLayout.getChildAt(0);
        }

        if (doChange) {
            view.setBackgroundColor(color);
        } else {
            view.setBackground(null);
        }

        // fix width
        boolean matchParent = doChange || viewHolder instanceof ChommentItemViewHolder;
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = matchParent ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        view.setLayoutParams(layoutParams);
    }

    private static boolean isDarkThemeEnabled() {
        return Data.ctx
            .getSharedPreferences("tv.twitch.bttvandroid.app_preferences", 0)
            .getBoolean("dark_theme_enabled", false);
    }
}
