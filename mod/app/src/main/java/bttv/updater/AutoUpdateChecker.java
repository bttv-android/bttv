package bttv.updater;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;

/** Foreground metadata checks only; the existing dialog owns all explicit downloads/installations. */
public final class AutoUpdateChecker implements Application.ActivityLifecycleCallbacks,
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static AutoUpdateChecker instance;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final SharedPreferences preferences;
    private final AutoUpdateState state;
    private final UpdateController controller;
    private WeakReference<Activity> resumed=new WeakReference<>(null);
    private View banner;
    private UpdateDialog viewed;
    private final Runnable foreground=()-> {
        try { checkDue(); showNotice(); }
        catch(RuntimeException ignored) { clearBanner(); }
    };

    public static synchronized void initialize(Context context) {
        if(instance!=null || context==null)return;
        try {
            Context app=context.getApplicationContext();
            if(!(app instanceof Application))return;
            AutoUpdateChecker checker=new AutoUpdateChecker((Application)app);
            ((Application)app).registerActivityLifecycleCallbacks(checker);
            try { checker.preferences.registerOnSharedPreferenceChangeListener(checker); }
            catch(RuntimeException failure) {
                ((Application)app).unregisterActivityLifecycleCallbacks(checker);
                checker.preferences.unregisterOnSharedPreferenceChangeListener(checker);
                throw failure;
            }
            instance=checker;
        } catch(RuntimeException | LinkageError ignored) {
            // An optional update check must never interrupt Twitch startup.
        }
    }

    AutoUpdateChecker(Application app) {
        preferences=app.getSharedPreferences("BTTV",0);
        controller=UpdateController.get(app);
        state=new AutoUpdateState(new AutoUpdateState.Store() {
            public boolean enabled() { return isEnabled(); }
            public long get(String key) {
                try { return preferences.getLong(key,0); }
                catch(RuntimeException invalid) { return 0; }
            }
            public void checked(long success,long attempt,long retry) {
                preferences.edit().putLong(AutoUpdateState.SUCCESS,success)
                        .putLong(AutoUpdateState.ATTEMPT,attempt).putLong(AutoUpdateState.RETRY,retry).apply();
            }
            public void notified(long code) {
                preferences.edit().putLong(AutoUpdateState.NOTIFIED,code).apply();
            }
        });
    }
    private boolean isEnabled() {
        try { return preferences.getBoolean(AutoUpdateState.ENABLED,true); }
        catch(RuntimeException invalid) { return false; }
    }
    private void checkDue() {
        if(!alive(resumed.get()))return;
        final long ticket=state.begin(System.currentTimeMillis());
        if(ticket<0)return;
        try {
            long request=controller.checkAutomatically(new UpdateController.AutomaticCheck() {
                public boolean enabled() { return isEnabled(); }
                public void finished(long id,UpdateRelease.Outcome outcome,UpdateRelease release,long retryAt) {
                    try {
                        state.complete(ticket,System.currentTimeMillis(),outcome,release,retryAt);
                        showNotice();
                    } catch(RuntimeException ignored) { clearBanner(); }
                }
            });
            if(request<0)state.abort(ticket);
        } catch(RuntimeException ignored) { state.abort(ticket); }
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences prefs,String key) {
        if(!AutoUpdateState.ENABLED.equals(key))return;
        main.post(()-> {
            try {
                main.removeCallbacks(foreground);
                if(!isEnabled()) {
                    state.disabled(); controller.cancelAutomatic(); clearBanner();
                } else schedule();
            } catch(RuntimeException ignored) { clearBanner(); }
        });
    }
    private void schedule() {
        main.removeCallbacks(foreground);
        if(isEnabled() && alive(resumed.get()))main.postDelayed(foreground,1500);
    }
    private static boolean alive(Activity activity) {
        return activity!=null && !activity.isFinishing() && !activity.isDestroyed();
    }
    private static boolean noticeHost(Activity activity) {
        String name=activity.getClass().getName();
        return "tv.twitch.android.feature.viewer.landing.ViewerLandingActivity".equals(name)
                || "tv.twitch.android.settings.SettingsActivity".equals(name)
                || "bttv.settings.SettingsActivity".equals(name);
    }
    private void showNotice() {
        Activity activity=resumed.get();
        UpdateRelease release=state.notice();
        if(banner!=null || release==null || !alive(activity) || !noticeHost(activity)
                || !activity.hasWindowFocus() || !controller.canNotify(release)
                || (Build.VERSION.SDK_INT>=26 && activity.isInPictureInPictureMode()))return;
        ViewGroup root=activity.findViewById(android.R.id.content);
        if(root==null)return;
        LinearLayout row=new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        int pad=Math.round(8*activity.getResources().getDisplayMetrics().density);
        row.setPadding(pad,pad,pad,pad); row.setBackgroundColor(0xff18181b); row.setElevation(pad);
        TextView text=new TextView(activity); text.setTextColor(0xffefeff1);
        text.setText("BTTV "+release.name+" update available");
        row.addView(text,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        Button view=new Button(activity); view.setText("View update"); row.addView(view);
        Button later=new Button(activity); later.setText("Later"); row.addView(later);
        // Both actions are user gestures. Showing this strip cannot start a transfer or installer.
        later.setOnClickListener(ignored->clearBanner());
        view.setOnClickListener(ignored-> {
            if(!isEnabled() || !alive(activity) || resumed.get()!=activity)return;
            clearBanner();
            try {
                if(viewed==null) {
                    viewed=new UpdateDialog(activity,()->viewed=null);
                    viewed.show();
                }
            } catch(RuntimeException unavailable) { closeViewed(); }
        });
        ViewGroup.LayoutParams params=root instanceof FrameLayout
                ?new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.TOP)
                :new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(row,params); banner=row;
        try { if(!state.notified(release))clearBanner(); }
        catch(RuntimeException unavailable) { clearBanner(); }
    }
    private void clearBanner() {
        View old=banner; banner=null;
        try { if(old!=null && old.getParent() instanceof ViewGroup)((ViewGroup)old.getParent()).removeView(old); }
        catch(RuntimeException ignored) { /* Detached owners cannot retain an updater view. */ }
    }
    private void closeViewed() {
        UpdateDialog old=viewed; viewed=null;
        try { if(old!=null)old.dispose(); }
        catch(RuntimeException ignored) { /* Never let optional UI interrupt Activity teardown. */ }
    }
    private void release(Activity activity) {
        if(resumed.get()!=activity)return;
        main.removeCallbacks(foreground); clearBanner();
        closeViewed();
        resumed.clear();
    }
    @Override public void onActivityResumed(Activity activity) {
        try {
            Activity previous=resumed.get();
            if(previous!=null && previous!=activity)release(previous);
            resumed=new WeakReference<>(activity); schedule();
        } catch(RuntimeException ignored) { clearBanner(); }
    }
    @Override public void onActivityPaused(Activity activity) { release(activity); }
    @Override public void onActivityDestroyed(Activity activity) { release(activity); }
    @Override public void onActivityStopped(Activity activity) { release(activity); }
    @Override public void onActivityCreated(Activity activity,Bundle state) {}
    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity,Bundle state) {}
}
