package bttv;

import android.util.Log;

import tv.twitch.android.models.streams.StreamModel;
import tv.twitch.android.models.streams.StreamType;
import tv.twitch.android.shared.player.overlay.BottomPlayerControlOverlayViewDelegate;
import tv.twitch.android.shared.player.overlay.PlayerOverlayViewDelegate;

public class Uptime {
  private final static String TAG = "LBTTVUptime";

  public static void replaceLiveIndicatorWithUptime(StreamModel streamModel, PlayerOverlayViewDelegate playerOverlayViewDelegate) {
    if (streamModel == null) {
      Log.i(TAG, "replaceLiveIndicatorWithUptime: streamModel is null");
      return;
    }

    if (!streamModel.getStreamType().equals(StreamType.LIVE_VIDEO)) {
      Log.d(TAG, "replaceLiveIndicatorWithUptime: stream type is not LIVE_VIDEO");
      return;
    }

    if (playerOverlayViewDelegate == null) {
      Log.i(TAG, "replaceLiveIndicatorWithUptime: playerOverlayViewDelegate is null");
      return;
    }

    BottomPlayerControlOverlayViewDelegate bottomVD = playerOverlayViewDelegate.BTTVgetBottomPlayerControlOverlayViewDelegate();

    if (bottomVD == null) {
      Log.i(TAG, "replaceLiveIndicatorWithUptime: playerOverlayViewDelegate.getBottomPlayerControlOverlayViewDelegate() returned null");
      return;
    }

    if (bottomVD.mLiveIndicatorLeftText == null) {
      Log.i(TAG, "replaceLiveIndicatorWithUptime: mLiveIndicatorLeftText is null");
      return;
    }

    Log.i(TAG, "replaceLiveIndicatorWithUptime: " + streamModel.createdAt);

    bottomVD.mLiveIndicatorLeftText.setText(streamModel.createdAt);
  }
}
