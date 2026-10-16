package bttv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.text.Annotation;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class Credits {

    public static void openDialog(Activity activity) {
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        ScrollView scrollView = (ScrollView) activity.getLayoutInflater().inflate(ResUtil.getResourceId(Res.layouts.bttv_credits_dialog), null);

        LinearLayout linearLayout = (LinearLayout) scrollView.getChildAt(0);

        TextView intoTv = scrollView.findViewById(ResUtil.getResourceId(activity, Res.ids.bttv_credits_into_tv));
        intoTv.setText(linkify(activity, Res.strings.bttv_credits_intro));
        intoTv.setLinkTextColor(activity.getResources().getColor(ResUtil.getResourceId(activity, "text_link", "color")));
        intoTv.setMovementMethod(LinkMovementMethod.getInstance());

        try {
            addContributors(linearLayout, activity);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        b.setView(scrollView);
        b.setCancelable(true);
        b.setPositiveButton(ResUtil.getStringId("close"), null);
        AlertDialog dialog = b.create();
        dialog.show();
    }

    private static void addContributors(LinearLayout v, Activity activity) throws JSONException, IOException {
        Contributor[] contributors = loadJson(activity);
        LinkedHashMap<String, ArrayList<Contributor>> map = new LinkedHashMap<>();

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
            kindTitle.setTextColor(activity.getResources().getColor(ResUtil.getResourceId(activity, "text_base", "color")));
            kindTitle.setPadding(0, dp(activity, 16), 0, dp(activity, 8));
            kindTitle.setAccessibilityHeading(true);
            kindTitle.setText(kindToString(activity, kind));
            v.addView(kindTitle);
            for (Contributor contributor : map.get(kind)) {
                SpannableString spannableString = new SpannableString(contributor.login);
                String destination = CreditsLinks.destination(contributor.url);
                if (destination != null && !contributor.login.isEmpty()) {
                    spannableString.setSpan(new LinkSpan(destination, activity), 0,
                            contributor.login.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                TextView tv = new TextView(activity);
                tv.setTextSize(16);
                tv.setTextColor(activity.getResources().getColor(ResUtil.getResourceId(activity, "text_base", "color")));
                tv.setLinkTextColor(activity.getResources().getColor(ResUtil.getResourceId(activity, "text_link", "color")));
                tv.setMinHeight(dp(activity, 48));
                tv.setGravity(android.view.Gravity.CENTER_VERTICAL);
                tv.setText(spannableString);
                if (destination != null) {
                    tv.setMovementMethod(LinkMovementMethod.getInstance());
                    tv.setOnClickListener(ignored -> openLink(activity, destination));
                    tv.setFocusable(true);
                }
                v.addView(tv);
            }
        }
    }

    private static Contributor[] loadJson(Activity activity) throws IOException, JSONException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream is = activity.getAssets().open("bttv_contributors.json")) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = is.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
        }
        JSONObject json = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        JSONArray arr = json.getJSONArray("contributors");

        Contributor[] ret = new Contributor[arr.length()];

        for (int i = 0; i < arr.length(); i++) {
            JSONObject jsonContr = arr.getJSONObject(i);
            Contributor contributor = new Contributor();
            contributor.login = jsonContr.getString("login");
            contributor.name = jsonContr.getString("name");
            contributor.url = jsonContr.optString("profile", "");
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
        CharSequence sStr = activity.getText(ResUtil.getResourceId(res));
        Annotation[] annotations = sStr instanceof Spanned
                ? ((Spanned) sStr).getSpans(0, sStr.length(), Annotation.class) : new Annotation[0];
        SpannableString spannableString = new SpannableString(sStr);

        for (Annotation annotation: annotations) {
            if (annotation.getKey().equals("link_to")) {
                String link_to = CreditsLinks.destination(annotation.getValue());
                if (link_to != null) spannableString.setSpan(
                    new LinkSpan(link_to, activity),
                    spannableString.getSpanStart(annotation),
                    spannableString.getSpanEnd(annotation),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
        return spannableString;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static void openLink(Activity activity, String supplied) {
        String destination = CreditsLinks.destination(supplied);
        if (destination == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(destination)));
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            // Keep the Credits dialog usable even without a supported link handler.
        }
    }

    private static String kindToString(Context context, String kind) {
        switch (kind) {
            case "bug":
                return ResUtil.getLocaleString(context, Res.strings.bttv_credits_bugs);
            case "translation":
                return ResUtil.getLocaleString(context, Res.strings.bttv_credits_translations);
            case "ideas":
                return ResUtil.getLocaleString(context, Res.strings.bttv_credits_ideas);
            case "doc":
                return ResUtil.getLocaleString(context, Res.strings.bttv_credits_docs);
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
            openLink(activity, link_to);
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
