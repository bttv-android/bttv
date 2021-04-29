package bttv;

import android.app.job.JobService;
import android.app.job.JobParameters;

public class UpdaterJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Updater.checkForUpdates(null, null);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
