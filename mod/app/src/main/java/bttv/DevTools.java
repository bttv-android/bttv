package bttv;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import bttv.settings.Settings;

public class DevTools {
    private static final String TAG = "LBTTVDevTools";

    public static void maybeShowDevTools(Menu menu) {
        if (!ResUtil.getBooleanFromSettings(Settings.DevToolsEnabled)) {
            return;
        }

        Res.ids[] ids = new Res.ids[]{
                Res.ids.debug_experiment_dialog,
                Res.ids.ad_debug_settings,
                Res.ids.commerce_settings,
                Res.ids.creator_settings,
                Res.ids.community_settings,
                Res.ids.latency_injection_settings,
        };
        for (Res.ids id : ids) {
            int intid = ResUtil.getResourceId(Data.ctx, id);
            if (intid == -1) {
                Log.w(TAG, "maybeShowDevTools: intid is -1 for " + id);
                continue;
            }
            MenuItem menuItem = menu.findItem(intid);
            if (menuItem == null) {
                Log.w(TAG, "maybeShowDevTools: menuItem is null for " + id + " intid: " + intid);
                continue;
            }
            menuItem.setVisible(true);
        }
    }
}
