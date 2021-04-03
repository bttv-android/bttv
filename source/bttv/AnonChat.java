package bttv;

import java.util.Set;
import java.util.HashSet;

import android.content.Context;
import android.util.Log;
import tv.twitch.android.sdk.ChatController;
import tv.twitch.android.shared.chat.observables.ChatConnectionController;

public class AnonChat {

    private static ChatConnectionController connectionController = null;

    public static boolean ableToChat = true; // used to disable message field
    private static boolean joinedAnyway = false; // true when the user joined manualy

    private static Set<Integer> set = new HashSet<>();

    // called in tv.twitch.android.sdk.ChatController.connect()
    public static boolean shouldAnonymize(int viewerId, int channelId, String screenName) {
        Context ctx = Data.ctx;
        boolean anonEnabled = UserPreferences.getAnonChatEnabled(ctx);
        boolean result = anonEnabled && !joinedAnyway;
        ableToChat = !result;
        joinedAnyway = false;
        Log.d("LBTTVAnonChat", "result: " + result);
        if (result) {
            set.add(channelId);
        }
        return result;
    }

    public static boolean _disconnect(ChatController this$, int viewerId, int channelId) {
        int newViewerId = set.remove(channelId) ? 0 : viewerId; // if in set, use 0
        return this$._disconnect(newViewerId, channelId);
    }

    public static void onSendMessage(int channelID, String message) {
        if (set.contains(channelID)) {
            if (message.trim().equals("/join")) {
                joinAnyway(channelID);
            } else {
                // TODO: notify user they cant chat
            }
        }
    }

    public static void setConnectionController(ChatConnectionController controller) {
        Log.d("LBTTVAnonChat", "got connectionController");
        connectionController = controller;
    }

    private static void joinAnyway(int channelId) {
        if (connectionController == null) {
            Log.w("LBTTVAnonChat", "connectionController is null", new Exception());
            return;
        }

        connectionController.chatController.bttvDisconnect(connectionController.viewerId, channelId,
                connectionController.screenName);
        Log.d("LBTTVAnonChat", "disconnected");
        joinedAnyway = true;
        connectionController.chatController.connect(connectionController.viewerId, channelId,
                connectionController.screenName);
        Log.d("LBTTVAnonChat", "connected");

    }
}
