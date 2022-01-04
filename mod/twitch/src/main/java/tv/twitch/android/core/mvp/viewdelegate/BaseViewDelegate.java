package tv.twitch.android.core.mvp.viewdelegate;

import android.content.Context;
import android.view.View;

public abstract class BaseViewDelegate {
    public final Context getContext() {
        throw new IllegalStateException("BaseViewDelegate.getContext() was called");
    }
    public final View getContentView() {
        throw new IllegalStateException("BaseViewDelegate.getContentView() was called");
    }
}
