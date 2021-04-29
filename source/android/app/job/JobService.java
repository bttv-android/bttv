package android.app.job;

import android.app.Service;

public abstract class JobService extends Service {
    public abstract boolean onStartJob(JobParameters params);

    public abstract boolean onStopJob(JobParameters params);

    public final void jobFinished(JobParameters params, boolean needsRescheduled) {
    };
}
