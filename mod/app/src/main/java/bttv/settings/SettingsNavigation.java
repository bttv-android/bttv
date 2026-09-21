package bttv.settings;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

/** Installs the BTTV entry point in Twitch's current settings toolbar. */
public final class SettingsNavigation {
    private SettingsNavigation() {
    }

    public static void install(final Activity activity, Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            CharSequence title = menu.getItem(i).getTitle();
            if (title != null && "BTTV".contentEquals(title)) {
                return;
            }
        }
        MenuItem item = menu.add("BTTV");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem ignored) {
                activity.startActivity(new Intent(activity, SettingsActivity.class));
                return true;
            }
        });
    }
}
