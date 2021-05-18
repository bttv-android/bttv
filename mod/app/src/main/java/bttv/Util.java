package bttv;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;

public class Util {
    public static int getResourceId(@NonNull String name, @NonNull String type) {
        Context context = Data.ctx;
        if (context == null) {
            Log.e("LBTTVUtil", "getResourceId: context is null", new Exception());
            return -1;
        }
        Resources res = context.getResources();
        String pkgName = context.getPackageName();

        return res.getIdentifier(name, type, pkgName);
    }
}
