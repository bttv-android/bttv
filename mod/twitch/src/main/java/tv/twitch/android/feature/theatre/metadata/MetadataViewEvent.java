package tv.twitch.android.feature.theatre.metadata;

public class MetadataViewEvent {
    public static final class SubscribeButtonClicked extends MetadataViewEvent {
        public tv.twitch.android.models.channel.ChannelModel channelModel;
    }
}
