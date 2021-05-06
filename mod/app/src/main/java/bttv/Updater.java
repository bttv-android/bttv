package bttv;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import tv.twitch.android.app.core.navigation.PersistentBannerPresenter;
import tv.twitch.android.app.core.navigation.PersistentBannerPresenter.PersistentBannerPresenterListener;

public class Updater {
    private static final String GH_API_HOST = "https://api.github.com";

    public static void checkForUpdates(final Activity activity, final PersistentBannerPresenter presenter) {
        Log.d("LBTTVUpdated", "Checking for updates...");
        Network.get(GH_API_HOST + "/repos/bttv-android/bttv/releases/latest", new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("LBTTVNetworkFail", "Update Call failed", e);
                Log.e("LBTTVNetworkFail", call.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);

                    String str = responseBody.string();
                    JSONObject json = new JSONObject(str);
                    String tagName = json.getString("tag_name");
                    if (tagName.equals(Data.getBttvVersion())) {
                        Log.d("LBTTVUpdate",
                                "app up-to-date (version: , " + Data.getBttvVersion() + " gh: " + tagName + ")");
                        return;
                    }
                    String body = json.getString("body");
                    if (body == null) {
                        body = "No changelog found.";
                    }
                    String apkUrl = extractApkUrl(json);
                    if (apkUrl == null) {
                        Log.w("LBTTVUpdate", "Update found, but no apk file attached, won't ask user");
                        return;
                    }
                    Log.d("LBTTVUpdater", "Update available " + Data.getBttvVersion() + " -> " + tagName);
                    askUser(activity, presenter, tagName, body, apkUrl, json.getString("html_url"));
                } catch (JSONException e) {
                    e.printStackTrace();
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

    private static void askUser(final Activity activity, final PersistentBannerPresenter presenter,
            final String newVersion, final String body, final String apkUrl, final String htmlUrl) {

        presenter.setListener(new PersistentBannerPresenterListener() {

            @Override
            public void installBannerShown() {
                Log.d("LBTTVUpdater", "Shown prompt");
            }

            @Override
            public void installUpdate() {
                Log.d("LBTTVUpdater", "user requested install");
                presenter.setShouldShowUpdateBanner(false);

                Intent intent = new Intent(activity, UpdaterActivity.class);
                intent.putExtra("new_version", newVersion);
                intent.putExtra("body", body);
                intent.putExtra("url", apkUrl);
                activity.startActivity(intent);

            }

            @Override
            public void updateDismissed() {
                Log.d("LBTTVUpdater", "user dismissed prompt");
            }

        });

        presenter.setShouldShowUpdateBanner(true);

    }

}
