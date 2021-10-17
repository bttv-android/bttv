package bttv;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import bttv.settings.Settings;
import tv.twitch.android.adapters.social.MessageRecyclerItem.ChatMessageViewHolder;
import tv.twitch.android.core.adapters.RecyclerAdapterItem;
import tv.twitch.android.shared.chat.chomments.ChommentRecyclerItem.ChommentItemViewHolder;

public class SplitChat {
    private final static String TAG = "LBTTVSplitChat";

    public static void setBackgroundColor(int position, RecyclerAdapterItem viewHolder) {
        if (!ResUtil.getBooleanFromSettings(Settings.SplitChatEnabled)) {
            return;
        }
        TextView textView = getTextView(viewHolder);
        if (textView == null) {
            return;
        }
        int color = isDarkThemeEnabled() ? ResUtil.getColorValue("material_grey_900") : ResUtil.getColorValue("material_grey_300");

        boolean doChange = position % 2 == 1;

        if (doChange) {
            textView.setBackgroundColor(color);
        } else {
            textView.setBackground(null);
        }

        // fix width
        ViewGroup.LayoutParams layoutParams = textView.getLayoutParams();
        layoutParams.width = doChange ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        textView.setLayoutParams(layoutParams);
    }

    private static TextView getTextView(RecyclerAdapterItem item) {
        if (item instanceof ChatMessageViewHolder) {
            ChatMessageViewHolder viewHolder = (ChatMessageViewHolder) item;
            return viewHolder.getMessageTextView();
        } else if (item instanceof ChommentItemViewHolder) {
            ChommentItemViewHolder viewHolder = (ChommentItemViewHolder) item;
            return viewHolder.getChommentTextView();
        }
        Log.w(TAG, "getTextView: RecyclerAdapterItem type unknown", new Exception());
        return null;
    }

    private static boolean isDarkThemeEnabled() {
        return Data.ctx
                .getSharedPreferences("tv.twitch.bttvandroid.app_preferences", 0)
                .getBoolean("dark_theme_enabled", false);
    }
}
