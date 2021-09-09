package bttv;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.util.Log;
import android.content.ClipboardManager;
import android.widget.Toast;

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

        Activity activity = presenter.activity;

        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("username", user.username);
        clipboard.setPrimaryClip(clip);

        Toast toast = Toast.makeText(activity, ResUtil.getLocaleString(activity, Res.strings.bttv_copied_to_clipboard), Toast.LENGTH_LONG);
        toast.show();

        String url = getUrl(Data.currentBroadcasterName + "");
        Util.launchBrowser(activity, url);
    }
}
