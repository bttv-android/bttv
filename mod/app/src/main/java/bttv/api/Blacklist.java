package bttv.api;


import android.util.Log;

import tv.twitch.android.shared.chat.pub.messages.ui.ChatMessageInterface;
import tv.twitch.android.shared.chat.ChatMessageDelegate;

/** @noinspection ALL */
public class Blacklist {
    /** @noinspection unused*/
    public static boolean isBlocked(ChatMessageInterface chatMessageInterface) {
        if (chatMessageInterface instanceof ChatMessageDelegate) {
            ChatMessageDelegate delegate = (ChatMessageDelegate) chatMessageInterface;
            if (delegate.chatMessage.messageType != null) {
                return delegate.chatMessage.messageType.equals("bttv-blocked-message");
            } else {
                Log.w("LBTTV", "isBlocked: messageType == null");
                return false;
            }
        } else {
            Log.w(
                    "LBTTV",
                    "isBlocked: interface it not a ChatMessageDelegate it's a "
                            + chatMessageInterface.getClass().getName());
            return false;
        }
    }
}
