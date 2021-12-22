package bttv;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.util.Log;

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

  private static boolean checkedBefore = false;

  @SuppressLint("PackageManagerGetSignatures")
  public static void checkIfTwitchInstalledOrNotifyUser(Context context) {
    if (checkedBefore) return;
    checkedBefore = true;

    PackageManager pm = context.getPackageManager();
    if (pm == null) {
      Log.w("LBTTVWatchParties", "context.getPackageManager() returned null!! This will cause WatchParties not to work!");
      return;
    }
    try {
      pm.getPackageInfo("tv.twitch.android.app", PackageManager.GET_SIGNATURES);
    } catch (PackageManager.NameNotFoundException e) {
      notifyUserAboutMissingTwitch(context);
    }
  }

  private static void notifyUserAboutMissingTwitch(Context context) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder
            .setTitle("bttv_android")
            .setMessage(ResUtil.getLocaleString(context, "bttv_watchparty_twitch_missing"))
            .setPositiveButton(ResUtil.getLocaleString(context, "ok_confirmation"), null)
            .setCancelable(true);
    builder.show();
  }
}
