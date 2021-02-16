package android.content;

public interface SharedPreferences {
    public interface Editor {
        Editor putBoolean(String key, boolean value);

        void apply();
    }

    public Editor edit();

    public boolean getBoolean(String key, boolean defValue);
}
