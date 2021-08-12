package bttv.updater;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import bttv.Res;
import bttv.ResUtil;
import bttv.Util;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
import ru.noties.markwon.Markwon;
import ru.noties.markwon.SpannableConfiguration;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

public class UpdaterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getResources();
        int bttv_updater_activity = res.getIdentifier("bttv_updater_activity", "layout", getPackageName());
        int bttv_updater_activity_title = res.getIdentifier("bttv_updater_activity_title", "id", getPackageName());
        int bttv_updater_activity_body = res.getIdentifier("bttv_updater_activity_body", "id", getPackageName());
        int bttv_updater_activity_pb = res.getIdentifier("bttv_updater_activity_pb", "id", getPackageName());

        setContentView(bttv_updater_activity);

        Intent intent = getIntent();
        String newVersion = intent.getStringExtra("new_version");
        String body = intent.getStringExtra("body");
        String url = intent.getStringExtra("url");

        TextView titleView = findViewById(bttv_updater_activity_title);

        String titleTemplate = ResUtil.getLocaleString(this, Res.strings.bttv_updater_downloading_version);
        titleView.setText(String.format(titleTemplate, newVersion));

        TextView bodyView = findViewById(bttv_updater_activity_body);
        bodyView.setText(Markwon.markdown(SpannableConfiguration.create(this), body));

        ProgressBar bar = findViewById(bttv_updater_activity_pb);

        startDownload(url, bar);

    }

    private interface ProgressListener {
        void update(long bytesRead, long contentLength, boolean done);
    }

    private void startDownload(final String url, final ProgressBar bar) {
        final String filename = "bttv.apk";
        final ProgressListener progListener = getProgressListener(bar);
        final OkHttpClient client = getClient(progListener);

        final Request request = new Request.Builder().url(url).build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response = client.newCall(request).execute();

                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);

                    Log.d("LBTTVUpdaterActivity", "sucessful");

                    File downloadedFile = new File(UpdaterActivity.this.getCacheDir(), filename);
                    FileOutputStream os = new FileOutputStream(downloadedFile);
                    BufferedSink sink = Okio.buffer(Okio.sink(os));
                    sink.writeAll(response.body().source());
                    sink.close();

                    Intent intent = new Intent("android.intent.action.INSTALL_PACKAGE");
                    Uri uri = FileProvider.getUriForFile(UpdaterActivity.this, "tv.twitch.bttvandroid.app.provider",
                            downloadedFile);
                    intent.setDataAndType(uri, "application/vnd.android.package-archive");
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);

                } catch (Exception e) {
                    Log.e("LBTTVUpdaterActivity", "Update failed", e);
                    UpdaterActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Util.showError(UpdaterActivity.this, e);
                        }
                    });
                }

            }
        }).start();

    }

    private ProgressListener getProgressListener(final ProgressBar bar) {
        return new ProgressListener() {
            boolean firstUpdate = true;

            @Override
            public void update(long bytesRead, long contentLength, boolean done) {
                if (done) {
                    Log.d("LBTTVUpdaterActivity", "Download done");
                    return;
                }

                if (firstUpdate) {
                    firstUpdate = false;
                }

                if (contentLength != -1) {
                    bar.setProgress((int) ((100 * bytesRead) / contentLength));
                }

            }

        };
    }

    private OkHttpClient getClient(final ProgressListener progressListener) {
        return new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
                .addNetworkInterceptor(new Interceptor() {
                    @NotNull
                    @Override
                    public Response intercept(@NotNull Chain chain) throws IOException {
                        Response originalResponse = chain.proceed(chain.request());
                        return originalResponse.newBuilder()
                                .body(new ProgressResponseBody(originalResponse.body(), progressListener)).build();
                    }
                }).build();
    }

    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        public MediaType contentType() {
            return responseBody.contentType();
        }

        public long contentLength() {
            return responseBody.contentLength();
        }

        @NotNull
        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(@NotNull Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }

}
