package bttv.updater;

import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.Manifest;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import bttv.Data;


public class UpdaterJobService extends JobService implements Updater.UpdateCallbackListener {

    final static int JOB_ID = 1873913;
    private JobParameters params = null;

    public static void schedule(Context context) {
        if (Build.VERSION.SDK_INT < 24) {
            Log.w("LBTTVUpdateScheduler", "schedule: API Level is " + Build.VERSION.SDK_INT + " < 24, wont schedule job");
            return;
        }
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        cancelOld(scheduler);

        if (isJobScheduled(scheduler)) {
            Log.d("LBTTVUpdateScheduler", "already scheduled");
            return;
        }

        ComponentName serviceComponent = new ComponentName(context, UpdaterJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(UpdaterJobService.JOB_ID, serviceComponent);
        builder.setPeriodic(2 * 24 * 60 * 60 * 1000); // 2 days in ms

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

    public static void cancelOld(JobScheduler jobScheduler) {
        jobScheduler.cancel(1873912);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean isJobScheduled(JobScheduler jobScheduler) {
        JobInfo info = jobScheduler.getPendingJob(JOB_ID);
        return info != null;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d("LBTTVUpdaterJob", "onStartJob: " + jobParameters.toString());
        this.params = jobParameters;
        Updater.checkForUpdates(this, this, false);
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, proceed with notification
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
                                        PendingIntent.FLAG_IMMUTABLE
                                )
                        )
                        .setAutoCancel(true);

                notificationManager.notify((int) (Math.random() * 10000), notifBuilder.build());
            } else {
                Log.w("NotificationsPermissionsReceiver", "Permissions are not allowed, not showing notification");
                //Twitch already asks on startup, we do not need to take care of this
            }
            jobFinished(params, false);
        } catch (Throwable e) {
            Log.e("LBTTVUpdaterJob", "err creating notif ", e);
            jobFinished(params, true);
        }
    }
}
