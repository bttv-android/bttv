package tv.twitch.android.shared.chat.observables;

import tv.twitch.android.models.channel.ChannelInfo;
import tv.twitch.android.models.streams.StreamType;
import tv.twitch.android.sdk.ChatController;

public class ChatConnectionController {
    public ChatController chatController;
    public int viewerId;
    public String screenName;
    public ChannelInfo broadcaster;
    public StreamType streamType;

    public final void bttvDisconnect(int chId) {

    }

    public final void setActiveChannel(ChannelInfo info, StreamType type) {
        
    }
}
