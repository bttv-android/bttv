package bttv.api;

import android.util.Log;
import android.view.Menu;

public class DevTools {

    private static final String TAG = "LBTTVDevTools";

    // called in ToolbarPresenter.onMenuCreated()
    public static void maybeShowDevTools(Menu menu) {
        try {
            Log.d(TAG, "maybeShowDevTools: " + menu);
            bttv.DevTools.maybeShowDevTools(menu);
        } catch (Throwable t) {
            Log.e(TAG, "maybeShowDevTools: ", t);
        }
    }
}
