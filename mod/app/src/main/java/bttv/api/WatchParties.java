package bttv.api;

import android.content.Context;
import android.util.Log;

public class WatchParties {
  private final static String TAG = "LBTTVWatchParties";

  public static Context wrap(Context context) {
    try {
      return new bttv.WatchParties.CustomContext(context);
    } catch (Throwable t) {
      Log.e(TAG, "wrap: ", t);
      return null;
    }
  }

}
