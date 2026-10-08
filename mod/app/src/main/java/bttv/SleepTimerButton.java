package bttv;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/** Player-overlay button that attaches the existing BTTV sleep-timer UI once. */
public final class SleepTimerButton extends AppCompatImageView {
    public SleepTimerButton(Context context) {
        super(context);
        SleepTimer.onInit(context, this);
    }

    public SleepTimerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        SleepTimer.onInit(context, this);
    }

    public SleepTimerButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        SleepTimer.onInit(context, this);
    }
}
