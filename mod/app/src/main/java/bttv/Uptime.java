package bttv;

import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import tv.twitch.android.models.streams.StreamModel;
import tv.twitch.android.models.streams.StreamType;
import tv.twitch.android.shared.player.overlay.BottomPlayerControlOverlayViewDelegate;
import tv.twitch.android.shared.player.overlay.PlayerOverlayViewDelegate;

public class Uptime {
  private final static String TAG = "LBTTVUptime";
  private final static Clock clock = new Clock();

  public static void replaceLiveIndicatorWithUptime(StreamModel streamModel, PlayerOverlayViewDelegate playerOverlayViewDelegate) throws NullPointerException {
    if (!prepareClock(streamModel, playerOverlayViewDelegate)) {
      clock.cancelExistingSchedule();
      playerOverlayViewDelegate.BTTVgetBottomPlayerControlOverlayViewDelegate().updateStreamType(StreamType.LIVE_VIDEO);
    }
  }

  public static boolean prepareClock(StreamModel streamModel, PlayerOverlayViewDelegate playerOverlayViewDelegate) {
    if (streamModel == null) {
      Log.i(TAG, "replaceLiveIndicatorWithUptime: streamModel is null");
      return false;
    }

    if (!streamModel.getStreamType().equals(StreamType.LIVE_VIDEO)) {
      Log.d(TAG, "replaceLiveIndicatorWithUptime: stream type is not LIVE_VIDEO");
      return false;
    }

    if (playerOverlayViewDelegate == null) {
      Log.i(TAG, "replaceLiveIndicatorWithUptime: playerOverlayViewDelegate is null");
      return false;
    }

    BottomPlayerControlOverlayViewDelegate bottomVD = playerOverlayViewDelegate.BTTVgetBottomPlayerControlOverlayViewDelegate();

    if (bottomVD == null) {
      Log.i(TAG, "replaceLiveIndicatorWithUptime: playerOverlayViewDelegate.getBottomPlayerControlOverlayViewDelegate() returned null");
      return false;
    }

    if (bottomVD.mLiveIndicatorLeftText == null) {
      Log.i(TAG, "replaceLiveIndicatorWithUptime: mLiveIndicatorLeftText is null");
      return false;
    }

    Date date = parseDate(streamModel.createdAt);

    if (date == null) {
      Log.w(TAG, "replaceLiveIndicatorWithUptime: date is null (" + streamModel.createdAt + ")");
      return false;
    }

    Log.i(TAG, "replaceLiveIndicatorWithUptime: " + streamModel.createdAt + " " + date);

    clock.textViewUpdated(bottomVD.mLiveIndicatorLeftText, secondsSince(date));
    return true;
  }

  private static Date parseDate(String dateString) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.US);
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    try {
      return simpleDateFormat.parse(dateString);
    } catch (ParseException e) {
      return altParseDate(dateString);
    }
  }

  private static Date altParseDate(String dateString) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    try {
      return simpleDateFormat.parse(dateString);
    } catch (ParseException e) {
      return null;
    }
  }

  private static long secondsSince(Date date) {
    return (new Date().getTime() - date.getTime()) / 1000;
  }

  private static class Clock {
    private Handler handler = new Handler();
    private TextView textView;
    private long secondsSinceStart;

    private Runnable task = new Runnable() {
      @Override
      public void run() {
        Clock.this.textView.setText(secondsToString(secondsSinceStart++));
        Clock.this.scheduleNextTick();
      }
    };

    public void textViewUpdated(TextView textView, long secondsSinceStart) {
      this.cancelExistingSchedule();
      this.textView = textView;
      this.secondsSinceStart = secondsSinceStart;
      this.task.run();
    }

    private void scheduleNextTick() {
      handler.postDelayed(task, 1000);
    }

    private void cancelExistingSchedule() {
      handler.removeCallbacks(task);
    }

    private static String secondsToString(long secs) {
      return DateUtils.formatElapsedTime(secs);
    }
  }
}
