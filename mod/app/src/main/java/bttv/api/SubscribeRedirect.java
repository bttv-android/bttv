package bttv.api;

import android.util.Log;
import android.view.View;

import tv.twitch.android.feature.theatre.common.PlayerCoordinatorPresenter;
import tv.twitch.android.models.social.ChatUser;
import tv.twitch.android.shared.chat.chatuserdialog.ChatUserDialogPresenter;

public class SubscribeRedirect {
    public static final String TAG = "LBTTVSubRedir";

    public static View.OnClickListener giftSubscriptionListener(ChatUserDialogPresenter presenter, ChatUser user) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Log.d(TAG, "giftSubscriptionListener()");
                    bttv.SubscribeRedirect.giftSubscription(presenter, user);
                } catch (Throwable e) {
                    Log.e(TAG, "giftSubscriptionListener: ", e);
                }
            }
        };
    }


}
