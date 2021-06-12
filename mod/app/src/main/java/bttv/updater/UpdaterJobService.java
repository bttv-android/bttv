package bttv.updater;

import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import bttv.Data;


public class UpdaterJobService extends JobService implements Updater.UpdateCallbackListener {

    final static int JOB_ID = 1873912;
    private JobParameters params = null;

    public static void schedule(Context context) {
        if (Build.VERSION.SDK_INT < 24) {
            Log.w("LBTTVUpdaterJobSvc", "schedule: API Level is " + Build.VERSION.SDK_INT + " < 24, wont schedule job");
            return;
        }
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);

        ComponentName serviceComponent = new ComponentName(context, UpdaterJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(UpdaterJobService.JOB_ID, serviceComponent);
        builder.setPeriodic(24 * 60 * 60 * 1000); // 1 day in ms

        // battery saving features
        if (Build.VERSION.SDK_INT >= 26) {
            builder.setRequiresBatteryNotLow(true);
        }
        if (Build.VERSION.SDK_INT >= 28) {
            builder.setEstimatedNetworkBytes(2500, 550);
        }
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NOT_ROAMING);

        JobInfo job = builder.build();
        scheduler.schedule(job);
        Log.d("LBTTVUpdateScheduler", "scheduled: " + job.toString());
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d("LBTTVUpdaterJob", "onStartJob: " + jobParameters.toString());
        this.params = jobParameters;
        Updater.checkForUpdates(this, this);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    @Override
    public void onError(Throwable error) {
        Log.d("LBTTVUpdaterJob", "onError");
        jobFinished(params, true);
    }

    @Override
    public void onNoUpdate() {
        Log.d("LBTTVUpdaterJob", "onNoUpdate");
        jobFinished(params, false);
    }

    @Override
    public void onUpdate(String newVersion, String body, String apkUrl, String htmlUrl) {
        Log.d("LBTTVUpdaterJob", "onUpdate");
        try {
            int icon = getResources().getIdentifier("ic_twitch_glitch_uv_alpha_only", "drawable", getPackageName());

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            Notifications.createChannels(this);

            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this, "bttv_android_updates")
                    .setSmallIcon(icon)
                    .setContentTitle("bttv-android update detected")
                    .setContentText(Data.getBttvVersion(this) + " -> " + newVersion)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(
                            PendingIntent.getActivity(
                                    this,
                                    0,
                                    Updater.updateActivityIndent(this, newVersion, body, apkUrl)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK),
                                    0
                            )
                    )
                    .setAutoCancel(true);

            notificationManager.notify((int) (Math.random() * 10000), notifBuilder.build());
        } catch (Throwable e) {
            Log.e("LBTTVUpdaterJob", "err creating notif ", e);
        }
    }
}
