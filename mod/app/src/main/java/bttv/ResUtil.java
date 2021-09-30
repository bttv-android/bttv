package bttv;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Set;

import bttv.settings.Settings;
import bttv.settings.UserPreferences;

public class ResUtil {

    public static int getResourceId(@NonNull Context context, @NonNull Res.strings res) {
        return getResourceId(context, res.name(), "string");
    }

    public static int getResourceId(@NonNull Context context, @NonNull Res.ids res) {
        return getResourceId(context, res.name(), "id");
    }

    public static int getResourceId(@NonNull Context context, @NonNull Res.layouts res) {
        return getResourceId(context, res.name(), "layout");
    }

    public static int getResourceId(@NonNull Context context, @NonNull String name, @NonNull String type) {
        Resources res = context.getResources();
        String pkgName = context.getPackageName();
        return res.getIdentifier(name, type, pkgName);
    }

    public static int getResourceId(@NonNull Res.strings res) {
        return getResourceId(res.name(), "string");
    }

    public static int getResourceId(@NonNull Res.layouts res) {
        return getResourceId(res.name(), "layout");
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

    public static int getColorValue(@NonNull String colorName) {
        int id = getResourceId(colorName, "color");
        return Data.ctx.getResources().getColor(id);
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

    public static boolean getBooleanFromSettings(@NonNull Settings settings) {
        if (!(settings.entry instanceof UserPreferences.Entry.BoolEntry)) {
            Log.e("LBTTVUtil", "getBooleanFromSettings: not a BoolEntry: " + settings.entry);
            return false;
        }
        return ((UserPreferences.Entry.BoolEntry) settings.entry).get(Data.ctx).get();
    }

    public static Set<String> getStringSetFromSettings(@NonNull Settings settings) {
        if (!(settings.entry instanceof UserPreferences.Entry.StringSetEntry)) {
            Log.e("LBTTVUtil", "getStringSetFromSettings: not a StringSetEntry: " + settings.entry);
            return null;
        }
        return ((UserPreferences.Entry.StringSetEntry) settings.entry).get(Data.ctx).get();
    }

    public static Res.strings getSelectedDropdownFromSettings(@NonNull Settings settings) {
        if (!(settings.entry instanceof UserPreferences.Entry.DropDownEntry)) {
            Log.e("LBTTVUtil", "getSelectedDropdownFromSettings: not a DropDownEntry: " + settings.entry);
            return null;
        }
        return ((UserPreferences.Entry.DropDownEntry) settings.entry).get(Data.ctx).getSelected();
    }

    public static boolean selectedDropdownFromSettingsIs(@NonNull Settings settings, Res.strings cmp) {
        return getSelectedDropdownFromSettings(settings) == cmp;
    }
}
