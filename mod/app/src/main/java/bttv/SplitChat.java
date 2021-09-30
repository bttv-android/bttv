package bttv;

import android.view.ViewGroup;
import android.widget.TextView;

import bttv.settings.Settings;
import tv.twitch.android.adapters.social.MessageRecyclerItem.ChatMessageViewHolder;

public class SplitChat {
    private final static String TAG = "LBTTVSplitChat";

    public static void setBackgroundColor(int position, ChatMessageViewHolder viewHolder) {
        if (!ResUtil.getBooleanFromSettings(Settings.SplitChatEnabled)) {
            return;
        }
        TextView textView = viewHolder.getMessageTextView();
        int color = isDarkThemeEnabled() ? ResUtil.getColorValue("twitch_purple_5") : ResUtil.getColorValue("twitch_purple_12");

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

    private static boolean isDarkThemeEnabled() {
        return Data.ctx.getSharedPreferences("tv.twitch.bttvandroid.app_preferences", 0).getBoolean("dark_theme_enabled", false);
    }
}
