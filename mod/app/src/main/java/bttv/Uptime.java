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

import bttv.settings.Settings;
import tv.twitch.android.models.streams.StreamModel;
import tv.twitch.android.models.streams.StreamType;
import tv.twitch.android.shared.player.overlay.BottomPlayerControlOverlayViewDelegate;
import tv.twitch.android.shared.player.overlay.PlayerOverlayViewDelegate;

public class Uptime {
  private final static String TAG = "LBTTVUptime";
  private final static Clock clock = new Clock();

  private static enum ReplaceOutcome {
    Success,
    Error,
    NotLive
  }

  public static void replaceLiveIndicatorWithUptime(StreamModel streamModel, PlayerOverlayViewDelegate playerOverlayViewDelegate) throws NullPointerException {
    if(!ResUtil.getBooleanFromSettings(Settings.ShowUptime)) {
      clock.cancelExistingSchedule();
      return;
    }

    ReplaceOutcome result = prepareClock(streamModel, playerOverlayViewDelegate);

    if (!result.equals(ReplaceOutcome.Success)) {
      clock.cancelExistingSchedule();
      if (!result.equals(ReplaceOutcome.NotLive)) {
        playerOverlayViewDelegate.BTTVgetBottomPlayerControlOverlayViewDelegate().updateStreamType(StreamType.LIVE_VIDEO);
      }
    }
  }

  public static ReplaceOutcome prepareClock(StreamModel streamModel, PlayerOverlayViewDelegate playerOverlayViewDelegate) {
    if (streamModel == null) {
      Log.i(TAG, "replaceLiveIndicatorWithUptime: streamModel is null");
      return ReplaceOutcome.Error;
    }

    if (!streamModel.getStreamType().equals(StreamType.LIVE_VIDEO)) {
      Log.d(TAG, "replaceLiveIndicatorWithUptime: stream type is not LIVE_VIDEO");
      return ReplaceOutcome.NotLive;
    }

    if (playerOverlayViewDelegate == null) {
      Log.i(TAG, "replaceLiveIndicatorWithUptime: playerOverlayViewDelegate is null");
      return ReplaceOutcome.Error;
    }

    BottomPlayerControlOverlayViewDelegate bottomVD = playerOverlayViewDelegate.BTTVgetBottomPlayerControlOverlayViewDelegate();

    if (bottomVD == null) {
      Log.i(TAG, "replaceLiveIndicatorWithUptime: playerOverlayViewDelegate.getBottomPlayerControlOverlayViewDelegate() returned null");
      return ReplaceOutcome.Error;
    }

    Log.i(TAG, "replaceLiveIndicatorWithUptime: mLiveIndicatorLeftText is null");
    return ReplaceOutcome.Error;
    /*

    Date date = parseDate(streamModel.createdAt);

    if (date == null) {
      Log.w(TAG, "replaceLiveIndicatorWithUptime: date is null (" + streamModel.createdAt + ")");
      return ReplaceOutcome.Error;
    }

    Log.i(TAG, "replaceLiveIndicatorWithUptime: " + streamModel.createdAt + " " + date);

    clock.textViewUpdated(bottomVD.mLiveIndicatorLeftText, secondsSince(date));
    return ReplaceOutcome.Success;
     */
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
