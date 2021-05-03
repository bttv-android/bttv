package bttv;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.app.job.JobParameters;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class UpdaterJobService extends JobService implements Updater.UpdateCallbackListener {

    final static int JOB_ID = 1873912;
    private JobParameters params = null;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        this.params = jobParameters;
        Updater.checkForUpdates(null, null, this);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    public static void schedule(Context context) {
        if (Build.VERSION.SDK_INT < 24) {
            Log.w("LBTTVUpdaterJobSvc", "schedule: API Level is " + Build.VERSION.SDK_INT + " < 24, wont schedule job");
            return;
        }
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);

        if (scheduler.getPendingJob(JOB_ID) != null) {
            return;
        }

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

        scheduler.schedule(builder.build());
    }

    @Override
    public void onDone(boolean ok) {
        if (ok) {
            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this, "bttv-android updates")
                    .setContentTitle("bttv-android update detected")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify((int) (Math.random() * 10000), notifBuilder.build());
        }
        jobFinished(params, !ok);
    }
}
