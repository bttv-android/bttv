package bttv.api;

import android.util.Log;

import java.util.List;

import tv.twitch.android.models.chat.MessageBadge;
import tv.twitch.chat.ChatMessageInfo;

public class Badges {
    public static final String TAG = "LBTTVBadges";
    public static void appendToBadges(ChatMessageInfo chatMessageInfo, List<MessageBadge> originalBadges) {
        try {
            bttv.Badges.appendToBadges(chatMessageInfo, originalBadges);
        } catch(Throwable t) {
            Log.e(TAG, "appendToBadges: ", t);
        }
    }

    public static String getUrl(int channelId, String badgeName, String badgeVersion) {
        try {
            return bttv.Badges.getUrl(channelId, badgeName, badgeVersion);
        } catch (Throwable t) {
            Log.e(TAG, "getUrl: ", t);
            return null;
        }
    }
}
