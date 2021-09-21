package tv.twitch.android.core.mvp.viewdelegate;

import android.content.Context;

public abstract class BaseViewDelegate {
    public final Context getContext() {
        throw new IllegalStateException("BaseViewDelegate.getContext() was called");
    }
}
