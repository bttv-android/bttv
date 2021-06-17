package bttv;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;

public class Util {

    public static int getResourceId(@NonNull Context context, @NonNull Res.strings res) {
        return getResourceId(context, res.name(), "string");
    }

    public static int getResourceId(@NonNull Context context, @NonNull String name, @NonNull String type) {
        Resources res = context.getResources();
        String pkgName = context.getPackageName();
        return res.getIdentifier(name, type, pkgName);
    }

    public static int getResourceId(@NonNull Res.strings res) {
        return getResourceId(res.name(), "string");
    }

    public static int getResourceId(@NonNull Res.colors res) {
        return getResourceId(res.name(), "color");
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

    public static String getLocaleString(@NonNull Context context, String key) {
        if(key == null) {
            return null;
        }
        int id = getResourceId(context, key, "string");
        if (id == 0) {
            Log.e("LBTTVUtil", "String " + key + " not found", new Exception());
            throw new IllegalStateException("String " + key + " not found");
        }
        Resources res = context.getResources();
        return res.getString(id);
    }

    public static String getLocaleString(@NonNull Context context, Res.strings key) {
        if(key == null) {
            return null;
        }
        return getLocaleString(context, key.name());
    }
}
