package bttv.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class UserPreferences {

    public static abstract class Entry {
        public interface Value {
            Object get();
        }
        public static class BoolValue implements Value {
            Boolean b;
            public BoolValue(Boolean b) {
                this.b = b;
            }
            public Boolean get() {
                return b;
            }
        }

        public final String key;
        public final String primaryTextResourceName;
        public final String secondaryTextResourceName;
        public final String auxiliaryTextResourceName;
        public final Value defaultValue;

        Entry(String key, Value defaultValue, String primaryTextRes, String secondaryTextRes, String auxiliaryTextRes) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.primaryTextResourceName = primaryTextRes;
            this.secondaryTextResourceName = secondaryTextRes;
            this.auxiliaryTextResourceName = auxiliaryTextRes;
        }

        public abstract Value get(Context ctx);
        public abstract void set(Context ctx, Value value);

        @Override
        public String toString() {
            return "Entry(key: " + key + ", defaultValue: " + defaultValue + ")";
        }

        public static class BoolEntry extends Entry {
            BoolEntry(String key, BoolValue defaultValue, String primaryTextRes, String secondaryTextRes, String auxiliaryTextRes) {
                super(key, defaultValue, primaryTextRes, secondaryTextRes, auxiliaryTextRes);
            }

            @Override
            public BoolValue get(Context ctx) {
                boolean b = ((BoolValue) this.defaultValue).get();
                return new BoolValue(UserPreferences.getBoolean(ctx, this.key, b));
            }

            @Override
            public void set(Context ctx, Value value) {
                if (!(value instanceof BoolValue)) {
                    IllegalStateException e = new IllegalStateException("Value is not a BoolValue " + this.toString());
                    Log.e("LBTTVBoolEntry", "Value is not a BoolValue", e);
                    throw e;
                }
                boolean b = ((BoolValue) value).get();
                UserPreferences.setBoolean(ctx, this.key, b);
            }

            @Override
            public String toString() {
                return "BoolEntry(key: " + key + ", defaultValue: " + defaultValue + ")";
            }
        }
    }

    private static SharedPreferences prefs = null;
    private static SharedPreferences.Editor editor = null;

    public static void ensureLoaded(Context ctx) {
        if(ctx == null && (prefs == null || editor == null)) {
            Log.e("LBTTVUserPReferences", "ensureLoaded: ctx is null, can't set editor field, this will cause problems!", new Exception());
            return;
        }
        if (prefs == null) {
            prefs = ctx.getSharedPreferences("BTTV", 0);
        }
        if (editor == null) {
            editor = prefs.edit();
        }
    }

    public static boolean getBoolean(Context ctx, String key, boolean defaultValue) {
        ensureLoaded(ctx);
        return prefs.getBoolean(key, defaultValue);
    }

    public static void setBoolean(Context ctx, String key, boolean value) {
        ensureLoaded(ctx);
        editor.putBoolean(key, value);
        editor.apply();
    }

}
