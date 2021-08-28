package bttv;

import android.util.Log;

import tv.twitch.android.feature.theatre.common.PlayerCoordinatorPresenter;
import tv.twitch.android.feature.theatre.metadata.MetadataViewEvent;
import tv.twitch.android.models.social.ChatUser;
import tv.twitch.android.shared.chat.chatuserdialog.ChatUserDialogPresenter;

public class SubscribeRedirect {
    public static void openSubscribePage(PlayerCoordinatorPresenter presenter, MetadataViewEvent.SubscribeButtonClicked event) {
        String channel = event.channelModel.component2();
        String url = getUrl(channel);
        Util.launchBrowser(presenter.activity, url);
    }

    private static String getUrl(String channel) {
        return "https://www.twitch.tv/subs/" + channel;
    }

    public static void giftSubscription(ChatUserDialogPresenter presenter, ChatUser user) {
        Log.i("LBTTVSubscribeRedirect", "giftSubscription: chatUser.username: " + user.username);
        String url = getUrl(presenter.info.getChannelId() + "");
        Util.launchBrowser(presenter.activity, url);
    }
}
