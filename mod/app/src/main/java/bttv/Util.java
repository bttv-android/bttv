package bttv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Util {
    public static void showError(Context ctx, Exception e) {
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("An Error has occurred");
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        b.setMessage(e.getMessage() + "\n\n" + errors);
        b.setCancelable(true);
        b.setPositiveButton(ResUtil.getLocaleString(ctx, "ok_confirmation"), null);
        b.show();
    }

    public static void launchBrowser(Activity activity, String url) {
        Intent viewIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(url));
        activity.startActivity(viewIntent);
    }
}
