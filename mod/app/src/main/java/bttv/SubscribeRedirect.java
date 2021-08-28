package bttv;

import tv.twitch.android.feature.theatre.common.PlayerCoordinatorPresenter;
import tv.twitch.android.feature.theatre.metadata.MetadataViewEvent;
import tv.twitch.android.models.channel.ChannelModel;

public class SubscribeRedirect {
    public static void openSubscribePage(PlayerCoordinatorPresenter presenter, MetadataViewEvent.SubscribeButtonClicked event) {
        ChannelModel channel = event.channelModel;
        String url = getUrl(channel);
        Util.launchBrowser(presenter.activity, url);
    }

    private static String getUrl(ChannelModel channel) {
        String name = channel.component2();
        return "https://www.twitch.tv/subs/" + name;
    }
}
