package bttv.api;

import android.util.Log;

import tv.twitch.android.feature.theatre.common.PlayerCoordinatorPresenter;
import tv.twitch.android.feature.theatre.metadata.MetadataViewEvent;

public class SubscribeRedirect {
    public static final String TAG = "LBTTVSubRedir";

    public static void subscribe(PlayerCoordinatorPresenter presenter, MetadataViewEvent.SubscribeButtonClicked event) {
        try {
            Log.d(TAG, "subscribe()");
            bttv.SubscribeRedirect.openSubscribePage(presenter, event);
        } catch (Throwable e) {
            Log.e(TAG, "subscribe: ", e);
        }
    }
}
