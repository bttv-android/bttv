"""Generate isolated JVM fixtures for the production SleepTimer implementation.

Usage: python tests/stage5i_sleep_timer_host.py <ignored-output-directory>
Compile the generated stubs and runner with SleepTimer.java and SleepTimerButton.java.
"""

from pathlib import Path
import sys


SOURCES = {
    "android/content/Context.java": """package android.content;
public class Context {}
""",
    "android/content/DialogInterface.java": """package android.content;
public interface DialogInterface {
    interface OnClickListener { void onClick(DialogInterface dialog, int which); }
}
""",
    "android/util/AttributeSet.java": """package android.util;
public interface AttributeSet {}
""",
    "android/util/Log.java": """package android.util;
public final class Log {
    public static int e(String tag, String message) { return 0; }
}
""",
    "android/view/View.java": """package android.view;
import android.content.Context;
public class View {
    public static final int VISIBLE = 0;
    public static final int GONE = 8;
    public interface OnClickListener { void onClick(View view); }
    private final Context context;
    private int visibility = VISIBLE;
    private OnClickListener listener;
    public View(Context context) { this.context = context; }
    public Context getContext() { return context; }
    @SuppressWarnings("unchecked")
    public <T extends View> T findViewById(int id) { return (T) this; }
    public void setVisibility(int value) { visibility = value; }
    public int getVisibility() { return visibility; }
    public void setOnClickListener(OnClickListener value) { listener = value; }
    public boolean performClick() { if (listener == null) return false; listener.onClick(this); return true; }
}
""",
    "android/widget/ImageView.java": """package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
public class ImageView extends View {
    public ImageView(Context context) { super(context); }
    public ImageView(Context context, AttributeSet attrs) { super(context); }
    public ImageView(Context context, AttributeSet attrs, int style) { super(context); }
}
""",
    "androidx/appcompat/widget/AppCompatImageView.java": """package androidx.appcompat.widget;
import android.content.Context;
import android.util.AttributeSet;
public class AppCompatImageView extends android.widget.ImageView {
    public AppCompatImageView(Context context) { super(context); }
    public AppCompatImageView(Context context, AttributeSet attrs) { super(context, attrs); }
    public AppCompatImageView(Context context, AttributeSet attrs, int style) { super(context, attrs, style); }
}
""",
    "androidx/annotation/NonNull.java": """package androidx.annotation;
public @interface NonNull {}
""",
    "android/app/AlertDialog.java": """package android.app;
import android.content.Context;
import android.content.DialogInterface;
public class AlertDialog {
    public void show() {}
    public static final class Builder {
        public static Builder last;
        public CharSequence[] items;
        public DialogInterface.OnClickListener itemListener;
        public DialogInterface.OnClickListener positiveListener;
        public Builder(Context context) { last = this; }
        public Builder setTitle(int value) { return this; }
        public Builder setMessage(int value) { return this; }
        public Builder setItems(CharSequence[] values, DialogInterface.OnClickListener listener) { items = values; itemListener = listener; return this; }
        public Builder setCancelable(boolean value) { return this; }
        public Builder setNegativeButton(int value, DialogInterface.OnClickListener listener) { return this; }
        public Builder setPositiveButton(int value, DialogInterface.OnClickListener listener) { positiveListener = listener; return this; }
        public AlertDialog create() { return new AlertDialog(); }
    }
}
""",
    "android/os/Process.java": """package android.os;
public final class Process {
    public static volatile int calls;
    public static volatile int lastPid = -1;
    public static int myPid() { return 4242; }
    public static void killProcess(int pid) { calls++; lastPid = pid; }
}
""",
    "bttv/settings/Settings.java": """package bttv.settings;
public enum Settings { ShouldShowSleepTimer }
""",
    "bttv/Res.java": """package bttv;
public final class Res {
    public enum strings {
        bttv_sleep_timer_select_dialog_title,
        bttv_sleep_timer_cancel_dialog_title,
        bttv_sleep_timer_cancel_dialog_message
    }
}
""",
    "bttv/ResUtil.java": """package bttv;
public final class ResUtil {
    public static boolean enabled;
    public static int getResourceId(android.content.Context context, String name, String type) { return 1; }
    public static int getResourceId(android.content.Context context, Res.strings name) { return 1; }
    public static boolean getBooleanFromSettings(bttv.settings.Settings setting) { return enabled; }
}
""",
    "bttv/SleepTimerHostTest.java": """package bttv;
import android.app.AlertDialog;
import android.content.Context;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Timer;
public final class SleepTimerHostTest {
    private static final Method SCHEDULE;
    private static final Method ABORT;
    private static final Field TIMER;
    static {
        try {
            SCHEDULE = SleepTimer.class.getDeclaredMethod("scheduleStop", int.class);
            SCHEDULE.setAccessible(true);
            ABORT = SleepTimer.class.getDeclaredMethod("abortSchedule");
            ABORT.setAccessible(true);
            TIMER = SleepTimer.class.getDeclaredField("timer");
            TIMER.setAccessible(true);
        } catch (Exception error) { throw new RuntimeException(error); }
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void schedule(int minutes) throws Exception { SCHEDULE.invoke(null, minutes); }
    private static void abort() throws Exception { ABORT.invoke(null); }
    private static Timer timer() throws Exception { return (Timer) TIMER.get(null); }
    public static void main(String[] args) throws Exception {
        Context context = new Context();
        ResUtil.enabled = false;
        SleepTimerButton hidden = new SleepTimerButton(context);
        check(hidden.getVisibility() == android.view.View.GONE, "disabled button was visible");

        ResUtil.enabled = true;
        SleepTimerButton visible = new SleepTimerButton(context);
        check(visible.getVisibility() == android.view.View.VISIBLE, "enabled button was hidden");
        check(visible.performClick(), "button listener was not attached");
        check(timer() == null, "opening selection dialog started a timer");
        check(AlertDialog.Builder.last.items.length == 6, "duration count changed");
        String[] expected = {"5 minutes", "10 minutes", "15 minutes", "30 minutes", "45 minutes", "1 hour"};
        for (int index = 0; index < expected.length; index++) {
            check(expected[index].contentEquals(AlertDialog.Builder.last.items[index]), "duration label mismatch");
        }

        AlertDialog.Builder.last.itemListener.onClick(null, 0);
        Timer first = timer();
        check(first != null, "selection did not create a timer");
        schedule(10);
        Timer replacement = timer();
        check(replacement != null && replacement != first, "replacement reused the old timer");
        abort();
        check(timer() == null, "cancellation did not clear timer state");

        android.os.Process.calls = 0;
        schedule(1);
        Thread.sleep(50L);
        check(android.os.Process.calls == 0, "process terminated before expiration");
        abort();
        Thread.sleep(50L);
        check(android.os.Process.calls == 0, "cancelled timer terminated process");

        schedule(0);
        long deadline = System.currentTimeMillis() + 2000L;
        while (android.os.Process.calls == 0 && System.currentTimeMillis() < deadline) Thread.sleep(10L);
        check(android.os.Process.calls == 1, "expiration did not terminate exactly once");
        check(android.os.Process.lastPid == 4242, "expiration used the wrong process ID");
        Thread.sleep(100L);
        check(android.os.Process.calls == 1, "single timer expired more than once");
        abort();
        System.out.println("PASS SleepTimer visibility/durations/create/replace/cancel/single-expiration/PID behavior");
    }
}
""",
}


if __name__ == "__main__":
    output = Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        target = output / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding="utf-8")
    print(f"Generated {len(SOURCES)} isolated SleepTimer JVM fixtures in {output}")
