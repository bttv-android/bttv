package bttv;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;

public class Util {

    public static int getResourceId(@NonNull Context context, @NonNull String name, @NonNull String type) {
        Resources res = context.getResources();
        String pkgName = context.getPackageName();
        return res.getIdentifier(name, type, pkgName);
    }

    public static int getResourceId(@NonNull String name, @NonNull String type) {
        Context context = Data.ctx;
        if (context == null) {
            Log.e("LBTTVUtil", "getResourceId: Data.ctx is null", new Exception());
            return -1;
        }
        return getResourceId(context, name, type);
    }

    public static int getStringId(@NonNull String name) {
        return getResourceId(name, "string");
    }
}
