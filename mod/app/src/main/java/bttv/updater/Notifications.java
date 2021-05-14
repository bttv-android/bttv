package bttv.updater;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

public class Notifications {

    public static void createChannels(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel = new NotificationChannel("bttv_android_updates", "bttv-android updates", importance);
            mChannel.setDescription("bttv-android will look for new updates everyday.");
            notificationManager.createNotificationChannel(mChannel);
        }
    }
}
