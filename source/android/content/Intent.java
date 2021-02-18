package android.content;

import android.net.Uri;

public class Intent {
    public Intent(Context packageContext, Class<?> cls) {

    }

    public Intent(String s) {

    }

    public Intent putExtra(String name, String value) {
        return this;
    }

    public String getStringExtra(String name) {
        return "";
    }

    public Intent setDataAndType(Uri data, String type) {
        return this;
    }

    public Intent setFlags(int flags) {
        return this;
    }
}
