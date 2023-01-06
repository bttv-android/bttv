package bttv.updater;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import bttv.Data;
import bttv.Network;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import tv.twitch.android.feature.viewer.main.navigation.PersistentBannerPresenter;
import tv.twitch.android.feature.viewer.main.navigation.PersistentBannerPresenter.PersistentBannerPresenterListener;

public class Updater {
    private static final String GH_API_HOST = "https://api.github.com";

    public static void manuallyCheckForUpdates(final Activity activity) {
        ManualUpdateListener listener = new ManualUpdateListener(activity);
        listener.onStart();
        checkForUpdates(activity, listener, true);
    }

    public static void checkForUpdates(final Activity activity, final PersistentBannerPresenter presenter) {
        checkForUpdates(activity, new UIListener(activity, presenter), false);
    }

    public static void checkForUpdates(@NonNull final Context context, @NonNull final UpdateCallbackListener listener, boolean manual) {
        if (!ResUtil.getBooleanFromSettings(Settings.AutoUpdateChecksEnabled) && !manual) {
            listener.onNoUpdate();
            return;
        }
        Log.d("LBTTVUpdated", "Checking for updates...");
        Network.get(GH_API_HOST + "/repos/bttv-android/bttv/releases/latest", new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("LBTTVNetworkFail", "Update Call failed", e);
                Log.e("LBTTVNetworkFail", call.toString());
                listener.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);

                    String str = responseBody.string();
                    JSONObject json = new JSONObject(str);
                    String tagName = json.getString("tag_name");
                    if (tagName.equals(Data.getBttvVersion(context))) {
                        Log.d("LBTTVUpdate",
                                "app up-to-date (version: , " + Data.getBttvVersion(context) + " gh: " + tagName + ")");
                        listener.onNoUpdate();
                        return;
                    }
                    String body = json.getString("body");
                    String apkUrl = extractApkUrl(json);
                    if (apkUrl == null) {
                        Log.w("LBTTVUpdate", "Update found, but no apk file attached, won't ask user");
                        listener.onNoUpdate();
                        return;
                    }

                    Log.d("LBTTVUpdater", "Update available " + Data.getBttvVersion(context) + " -> " + tagName);

                    listener.onUpdate(tagName, body, apkUrl, json.getString("html_url"));

                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.onError(e);
                }
            }

        });
    }

    private static String extractApkUrl(JSONObject json) throws JSONException {
        JSONArray assets = json.getJSONArray("assets");
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String mime = asset.getString("content_type");
            if (mime.equals("application/vnd.android.package-archive")) {
                return asset.getString("browser_download_url");
            }
        }
        return null;
    }

    public static Intent updateActivityIndent(@NonNull Context context, @NonNull String newVersion, @NonNull String body, @NonNull String apkUrl) {
        Intent intent = new Intent(context, UpdaterActivity.class);
        intent.putExtra("new_version", newVersion);
        intent.putExtra("body", body);
        intent.putExtra("url", apkUrl);
        return intent;
    }

    public interface UpdateCallbackListener {
        void onNoUpdate();

        void onUpdate(final String newVersion, final String body, final String apkUrl, final String htmlUrl);

        void onError(final Throwable error);
    }

    /** Shows update bar when update found, triggered automatically when the app is started */
    private static class UIListener implements UpdateCallbackListener {
        final Activity activity;
        final PersistentBannerPresenter presenter;

        public UIListener(final Activity activity, final PersistentBannerPresenter presenter) {
            this.activity = activity;
            this.presenter = presenter;
        }

        @Override
        public void onNoUpdate() {
        }

        @Override
        public void onUpdate(String newVersion, String body, String apkUrl, String htmlUrl) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    presenter.setListener(new PersistentBannerPresenterListener() {

                        @Override
                        public void installBannerShown() {
                            Log.d("LBTTVUpdater", "Shown prompt");
                        }

                        @Override
                        public void installUpdate() {
                            Log.d("LBTTVUpdater", "user requested install");
                            presenter.setShouldShowUpdateBanner(false);
                            activity.startActivity(updateActivityIndent(activity, newVersion, body, apkUrl));
                        }

                        @Override
                        public void updateDismissed() {
                            Log.d("LBTTVUpdater", "user dismissed prompt");
                        }

                    });
                    presenter.setShouldShowUpdateBanner(true);
                }
            });
        }

        @Override
        public void onError(Throwable error) {
            Log.e("LBTTVUpdaterUIListener", "an error has occurred while checking for updates", error);
        }
    }

    private static class ManualUpdateListener implements UpdateCallbackListener {
        private final Activity activity;
        private final Context appContext;

        private ManualUpdateListener(Activity activity) {
            this.activity = activity;
            this.appContext = activity.getApplicationContext();
        }

        private void showToast(@NonNull final Res.strings stringResource, final int duration) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CharSequence text = ResUtil.getLocaleString(ManualUpdateListener.this.activity, stringResource);
                    Toast.makeText(ManualUpdateListener.this.appContext, text, duration).show();
                }
            });
        }

        private void onStart() {
            showToast(Res.strings.bttv_updater_manual_update_start_toast, Toast.LENGTH_SHORT);
        }

        @Override
        public void onNoUpdate() {
            showToast(Res.strings.bttv_updater_manual_update_no_update_toast, Toast.LENGTH_LONG);
        }

        @Override
        public void onUpdate(String newVersion, String body, String apkUrl, String htmlUrl) {
            showToast(Res.strings.bttv_updater_manual_update_update_toast, Toast.LENGTH_SHORT);
            activity.startActivity(updateActivityIndent(activity, newVersion, body, apkUrl));
        }

        @Override
        public void onError(Throwable error) {
            showToast(Res.strings.bttv_updater_manual_update_error_toast, Toast.LENGTH_SHORT);
        }
    }
}
