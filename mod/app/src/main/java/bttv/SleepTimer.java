package bttv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;

import java.util.Timer;
import java.util.TimerTask;

import bttv.settings.Settings;

@SuppressWarnings("unused")
public class SleepTimer {
    private static Timer timer = null;

    @SuppressWarnings("unused")
    public static void onInit(final Context context, final View view) {
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

            if (!Util.getBooleanFromSettings(Settings.ShouldShowSleepTimer)) {
                // User does not want to see button, make sure it's gone
                timerButtonView.setVisibility(View.GONE);
                return;
            }

            timerButtonView.setVisibility(View.VISIBLE);
            timerButtonView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { openDialog(context); }
            });
        } catch(Throwable t) {
            Log.e("LBTTVSleepTimer", "onInit error: ", t);
        }
    }

    private static void openDialog(@NonNull Context context) {
        if (timer == null) {
            openSelectDialog(context);
        } else {
            openCancelDialog(context);
        }
    }

    private static void openSelectDialog(@NonNull Context context) {
        final int[] selected = new int[]{ -1 }; // has to be final, but we need mutability

        CharSequence[] items = new CharSequence[]{
                "5 minutes",
                "10 minutes",
                "15 minutes",
                "30 minutes",
                "45 minutes",
                "1 hour",
        };

        int[] minutes = new int[]{ 5, 10, 15, 30, 45, 60 };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Util.getResourceId(context, Res.strings.bttv_sleep_timer_select_dialog_title));

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
                selected[0] = index;
                if (selected[0] != -1) {
                    scheduleStop(minutes[selected[0]]);
                }
            }
        });

        builder
                .setCancelable(true)
                .setNegativeButton(Util.getResourceId(context, "cancel", "string"), null);

        builder.create().show();
    }

    private static void openCancelDialog(@NonNull Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setTitle(Util.getResourceId(context, Res.strings.bttv_sleep_timer_cancel_dialog_title))
                .setMessage(Util.getResourceId(context, Res.strings.bttv_sleep_timer_cancel_dialog_message));

        builder.setCancelable(true).setNegativeButton(Util.getResourceId(context, "no_prompt", "string"), null);
        builder.setPositiveButton(Util.getResourceId(context, "yes_prompt", "string"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                abortSchedule();
            }
        });
        builder.create().show();
    }

    private static void scheduleStop(int minutes) {
        abortSchedule();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }, minutes * 60 * 1000L);
    }

    private static void abortSchedule() {
        if (timer == null) {
            return;
        }
        timer.cancel();
        timer = null;
    }

}
