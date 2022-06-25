package bttv.api;


import android.util.Log;

import tv.twitch.android.provider.chat.ChatMessageInterface;
import tv.twitch.android.shared.chat.ChatMessageDelegate;

public class Blacklist {
    public static boolean isBlocked(ChatMessageInterface chatMessageInterface) {
        if (chatMessageInterface instanceof ChatMessageDelegate) {
            ChatMessageDelegate delegate = (ChatMessageDelegate) chatMessageInterface;
            return delegate.mChatMessage.messageType.equals("bttv-blocked-message");
        } else {
            Log.w(
                    "LBTTV",
                    "isBlocked: interface it not a ChatMessageDelegate it's a "
                            + chatMessageInterface.getClass().getName());
            return false;
        }
    }
}
