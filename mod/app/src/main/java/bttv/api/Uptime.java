package bttv.api;

import android.util.Log;

import tv.twitch.android.models.streams.StreamModel;
import tv.twitch.android.shared.player.overlay.PlayerOverlayViewDelegate;

public class Uptime {
  private final static String TAG = "LBTTVUptime";

  // called in tv/twitch/android/shared/player/overlay/PlayerOverlayPresenter.bindStream()
  public static void replaceLiveIndicatorWithUptime(StreamModel streamModel, PlayerOverlayViewDelegate playerOverlayViewDelegate) {
    Log.i(TAG, "replaceLiveIndicatorWithUptime()");
    try {
      bttv.Uptime.replaceLiveIndicatorWithUptime(streamModel, playerOverlayViewDelegate);
    } catch (Throwable t) {
      Log.e(TAG, "replaceLiveIndicatorWithUptime: ", t);
    }
  }
}
