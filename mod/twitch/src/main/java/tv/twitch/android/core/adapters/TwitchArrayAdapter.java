package tv.twitch.android.core.adapters;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.List;

public class TwitchArrayAdapter<T> extends ArrayAdapter<T> {
    public TwitchArrayAdapter(Context context, List<T> list, int i) {
        super(context, i, list);
    }
}
