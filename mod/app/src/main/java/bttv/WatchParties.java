package bttv;

import android.content.Context;
import android.content.ContextWrapper;

public class WatchParties {
  public static class CustomContext extends ContextWrapper {

    public CustomContext(Context base) {
      super(base);
    }

    @Override
    public String getPackageName() {
      return "tv.twitch.android.app";
    }
  }
}
