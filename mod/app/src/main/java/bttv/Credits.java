package bttv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.Annotation;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Credits {
    private static final String TAG = "LBTTVCredits";

    public static void openDialog(Activity activity) {
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        LinearLayout v = (LinearLayout) activity.getLayoutInflater().inflate(ResUtil.getResourceId(Res.layouts.bttv_credits_dialog), null);

        TextView intoTv = v.findViewById(ResUtil.getResourceId(activity, Res.ids.bttv_credits_into_tv));
        intoTv.setText(linkify(activity, Res.strings.bttv_credits_intro));

        try {
            addContributors(v, activity);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        b.setView(v);
        b.setCancelable(true);
        b.setPositiveButton(ResUtil.getStringId("close"), null);
        AlertDialog dialog = b.create();
        dialog.show();
    }

    private static void addContributors(LinearLayout v, Activity activity) throws JSONException, IOException {
        Contributor[] contributors = loadJson(activity);
        HashMap<String, ArrayList<Contributor>> map = new HashMap<>();

        for (Contributor contributor : contributors) {
            for (String contribution : contributor.contributions) {
                ArrayList<Contributor> list = map.get(contribution);
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(contributor);
                map.put(contribution, list);
            }
        }

        for (String kind : map.keySet()) {
            if (kind.equals("maintenance"))
                continue;
            TextView kindTitle = new TextView(activity);
            kindTitle.setTextSize(20.0f);
            kindTitle.setText(kindToString(activity, kind));
            v.addView(kindTitle);
            for (Contributor contributor : map.get(kind)) {
                Log.d(TAG, contributor.toString());
                SpannableString spannableString = new SpannableString(contributor.login);
                spannableString.setSpan(
                        new LinkSpan(contributor.url, activity),
                        0,
                        contributor.login.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                TextView tv = new TextView(activity);
                tv.setText(spannableString);
                v.addView(tv);
            }
        }
    }

    private static Contributor[] loadJson(Activity activity) throws IOException, JSONException {
        InputStream is = activity.getAssets().open("bttv_contributors.json");
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        JSONObject json = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
        JSONArray arr = json.getJSONArray("contributors");

        Contributor[] ret = new Contributor[arr.length()];

        for (int i = 0; i < arr.length(); i++) {
            JSONObject jsonContr = arr.getJSONObject(i);
            Contributor contributor = new Contributor();
            contributor.login = jsonContr.getString("login");
            contributor.name = jsonContr.getString("name");
            contributor.url = jsonContr.getString("profile");
            JSONArray contributions = jsonContr.getJSONArray("contributions");
            contributor.contributions = new String[contributions.length()];
            for (int j = 0; j < contributions.length(); j++) {
                contributor.contributions[j] = contributions.getString(j);
            }
            ret[i] = contributor;
        }
        return ret;
    }

    private static SpannableString linkify(Activity activity, Res.strings res) {
        SpannedString sStr = (SpannedString) activity.getText(ResUtil.getResourceId(res));
        Annotation[] annotations = sStr.getSpans(0, sStr.length(), Annotation.class);
        SpannableString spannableString = new SpannableString(sStr);

        for (Annotation annotation: annotations) {
            if (annotation.getKey().equals("link_to")) {
                String link_to = annotation.getValue();
                spannableString.setSpan(
                    new LinkSpan(link_to, activity),
                    spannableString.getSpanStart(annotation),
                    spannableString.getSpanEnd(annotation),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
        return spannableString;
    }

    private static String kindToString(Context context, String kind) {
        switch (kind) {
            case "bug":
                return ResUtil.getLocaleString(context, Res.strings.bttv_credits_bugs);
            case "translation":
                return ResUtil.getLocaleString(context, Res.strings.bttv_credits_translations);
            case "ideas":
                return ResUtil.getLocaleString(context, Res.strings.bttv_credits_ideas);
        }
        return kind;
    }

    private static class LinkSpan extends ClickableSpan {
        final String link_to;
        final Activity activity;

        public LinkSpan(String link_to, Activity activity) {
            this.link_to = link_to;
            this.activity = activity;
        }

        @Override
        public void onClick(@NonNull View widget) {
            Log.d(TAG, "CLICKED " + link_to);

            Uri uri = Uri.parse(link_to);
            String packageName = "com.android.browser";
            String className = "com.android.browser.BrowserActivity";
            Intent internetIntent = new Intent(Intent.ACTION_VIEW, uri);
            internetIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            internetIntent.setClassName(packageName, className);
            activity.startActivity(internetIntent);
        }
    }

    private static class Contributor {
        String login;
        String name;
        String url;
        String[] contributions;

        @Override
        public String toString() {
            return "Contributor{" +
                    "login='" + login + '\'' +
                    ", name='" + name + '\'' +
                    ", url='" + url + '\'' +
                    ", contributions=" + Arrays.toString(contributions) +
                    '}';
        }
    }
}
