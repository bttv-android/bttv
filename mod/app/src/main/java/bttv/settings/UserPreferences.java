package bttv.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import bttv.Res;
import tv.twitch.android.models.settings.SettingsDestination;

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
        public final Res.strings primaryTextResource;
        public final Res.strings secondaryTextResource;
        public final Res.strings auxiliaryTextResource;
        public final Value defaultValue;

        Entry(String key, Value defaultValue, Res.strings primaryTextRes, Res.strings secondaryTextRes, Res.strings auxiliaryTextRes) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.primaryTextResource = primaryTextRes;
            this.secondaryTextResource = secondaryTextRes;
            this.auxiliaryTextResource = auxiliaryTextRes;
        }

        public abstract Value get(Context ctx);
        public abstract void set(Context ctx, Value value);

        @NotNull
        @Override
        public String toString() {
            return "Entry(key: " + key + ", defaultValue: " + defaultValue + ")";
        }

        public static class BoolEntry extends Entry {
            BoolEntry(String key, BoolValue defaultValue, Res.strings primaryTextRes, Res.strings secondaryTextRes, Res.strings auxiliaryTextRes) {
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

            @NotNull
            @Override
            public String toString() {
                return "BoolEntry(key: " + key + ", defaultValue: " + defaultValue + ")";
            }
        }

        public static class LinkEntry extends Entry {
            public final SettingsDestination destination;
            LinkEntry(Res.strings primaryTextRes, Res.strings secondaryTextRes, Res.strings auxiliaryTextRes, SettingsDestination destination) {
                super(null, null, primaryTextRes, secondaryTextRes, auxiliaryTextRes);
                this.destination = destination;
            }

            @Override
            public Value get(Context ctx) {
                return null;
            }

            @Override
            public void set(Context ctx, Value value) {

            }

            @NotNull
            @Override
            public String toString() {
                return "LinkEntry()";
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

    public static void setStringSet(Context ctx, String key, Set<String> stringSet) {
        ensureLoaded(ctx);
        Set<String> s = new HashSet<>(stringSet); // copy cuz bug
        editor.putStringSet(key, s);
        editor.apply();
    }

    public static Set<String> getStringSet(Context ctx, String key) {
        ensureLoaded(ctx);
        return new HashSet<>(prefs.getStringSet(key, new HashSet<>()));  // copy cuz bug
    }

}
