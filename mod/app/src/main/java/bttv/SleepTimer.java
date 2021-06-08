package bttv;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import bttv.settings.UserPreferences;

public class SleepTimer {

    public static void onInit(Context context, View view) {
        try {
            if (context == null) {
                Log.e("LBTTVSleepTimer", "onInit(): context is null");
                return;
            }
            if (view == null) {
                Log.e("LBTTVSleepTimer", "onInit(): view is null");
                return;
            }
            int timerButtonId = Util.getResourceId(context, "bttv_sleep_timer_button", "id");
            ImageView timerButtonView = view.findViewById(timerButtonId);

            if (!UserPreferences.getShouldShowSleepTimer(context)) {
                // User does not want to see button, make sure it's gone
                timerButtonView.setVisibility(View.GONE);
                return;
            }

            timerButtonView.setVisibility(View.VISIBLE);
            timerButtonView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("LBTTVDEBUG", "WORKS!!");
                }
            });
        } catch(Throwable t) {
            Log.e("LBTTVSleepTimer", "onInit error: ", t);
        }
    }
}
