package android.content;

import java.io.File;

public abstract class Context {
    public abstract SharedPreferences getSharedPreferences(String name, int mode);

    public abstract File getCacheDir();
}
