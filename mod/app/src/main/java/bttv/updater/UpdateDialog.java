package bttv.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.FileProvider;

/** An existing-Settings dialog, never a service or a separately exported Activity. */
public final class UpdateDialog implements UpdateController.Observer {
    private final Activity activity;
    private final UpdateController controller;
    private final Runnable dismissed;
    private final AlertDialog dialog;
    private final TextView information, result;
    private final ProgressBar progress;
    private AlertDialog permissionDialog;
    private boolean disposed;
    private long installRequest=-1;

    public UpdateDialog(Activity activity,Runnable dismissed) {
        this.activity=activity; this.dismissed=dismissed; controller=UpdateController.get(activity);
        LinearLayout layout=new LinearLayout(activity); layout.setOrientation(LinearLayout.VERTICAL);
        int padding=Math.round(20*activity.getResources().getDisplayMetrics().density);
        layout.setPadding(padding,padding,padding,padding);
        information=new TextView(activity); result=new TextView(activity);
        information.setTextColor(0xffefeff1); result.setTextColor(0xffefeff1);
        try {
            UpdateRelease.Installed installed=UpdateVerifier.installed(activity);
            information.setText("Installed BTTV "+installed.name+" ("+installed.code+")\nOfficial releases only · Downloads and installation require your approval\n\n");
        } catch(Exception e) { information.setText("Installed identity unavailable. Updates will remain blocked.\n\n"); }
        progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);
        layout.addView(information); layout.addView(progress); layout.addView(result);
        ScrollView scroll=new ScrollView(activity); scroll.addView(layout);
        dialog=new AlertDialog.Builder(activity,android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Check for Updates").setView(scroll)
                .setPositiveButton("Check",null).setNeutralButton("Cancel operation",null).setNegativeButton("Close",null).create();
        dialog.setOnDismissListener(ignored-> {
            controller.remove(this); installRequest=-1;
            if(!disposed) { disposed=true; dismissed.run(); }
        });
    }
    public void show() {
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ignored->action());
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(ignored->{installRequest=-1;controller.cancel();});
        controller.observe(this);
    }
    public void dispose() {
        if(permissionDialog!=null)permissionDialog.dismiss();
        disposed=true; installRequest=-1; controller.remove(this); dialog.dismiss();
    }
    @Override public void changed(UpdateSession.Snapshot state) {
        if(disposed || activity.isFinishing() || activity.isDestroyed())return;
        information.setContentDescription("Installed BTTV version and manual update policy");
        boolean busy=controller.session.running();
        progress.setVisibility(busy?View.VISIBLE:View.GONE);
        progress.setIndeterminate(state.total<=0 || state.state!=UpdateSession.State.DOWNLOADING);
        if(state.total>0) { progress.setMax(100); progress.setProgress((int)(100*state.bytes/state.total)); }
        String text=state.message;
        if(state.release!=null) {
            UpdateRelease r=state.release;
            text+="\n\nBTTV "+r.name+" ("+r.code+")\nTwitch "+r.twitchVersion+" · Android API "+r.minSdk+"+\n"+
                    r.abis+"\nDownload: "+r.size+" bytes\n\n"+r.notes;
        }
        result.setText(text); // Plain TextView only, no Markdown/HTML/link execution.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!busy);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(busy?View.VISIBLE:View.GONE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(state.state==UpdateSession.State.AVAILABLE?"Download":
                state.state==UpdateSession.State.READY?(activity.getPackageManager().canRequestPackageInstalls()?"Install":"Allow installation"):"Check");
        if(installRequest==state.generation && state.state==UpdateSession.State.READY && !busy) {
            installRequest=-1;
            handoff(state);
        }
    }
    private void action() {
        if(!foreground())return;
        UpdateSession.Snapshot state=controller.session.snapshot();
        if(state.state==UpdateSession.State.AVAILABLE)controller.download();
        else if(state.state==UpdateSession.State.READY) {
            if(!activity.getPackageManager().canRequestPackageInstalls()) {
                permissionDialog=new AlertDialog.Builder(activity,android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setMessage("Android must allow BTTV to request app installation. You can decline. Returning here never installs automatically.")
                        .setNegativeButton("Cancel",null).setPositiveButton("Open Android settings",(d,w)-> {
                            try { activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName()))); }
                            catch(Exception e) { result.setText("Android installation settings are unavailable. Nothing was installed."); }
                        }).create();
                permissionDialog.show();
            } else installRequest=controller.prepareInstall();
        } else controller.check();
    }
    private void handoff(UpdateSession.Snapshot state) {
        // Only an explicit Install click on this still-live foreground dialog can arm this path.
        if(!foreground())return;
        if(!activity.getPackageManager().canRequestPackageInstalls()) { result.setText("Installation permission was not granted. Nothing was installed."); return; }
        try {
            Uri uri=FileProvider.getUriForFile(activity,UpdateRelease.PACKAGE+".provider",state.file);
            Intent intent=installerIntent(uri);
            activity.startActivity(intent);
            result.setText("Android installer launched. Installation is NOT confirmed. Android and you decide whether to proceed.");
        } catch(Exception e) { result.setText("Android installer could not be opened. Nothing was installed by BTTV."); }
    }
    static Intent installerIntent(Uri uri) {
        if(uri==null || !"content".equals(uri.getScheme()) || !(UpdateRelease.PACKAGE+".provider").equals(uri.getAuthority()))
            throw new IllegalArgumentException("Unexpected installer URI");
        return new Intent(Intent.ACTION_VIEW).setDataAndType(uri,UpdateRelease.MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
    private boolean foreground() {
        return !disposed && dialog.isShowing() && !activity.isFinishing() && !activity.isDestroyed()
                && dialog.getWindow()!=null && dialog.getWindow().getDecorView().hasWindowFocus();
    }
}
