package bttv.api;

import android.util.Log;

import java.util.List;

import tv.twitch.android.models.emotes.EmoteSet;

public class Autocomplete {
    final static String TAG = "LBTTVAutocomplete";

    public static void addOurEmotes(List<EmoteSet> list) {
        try {
            bttv.Autocomplete.addOurEmotes(list);
        } catch (Throwable e) {
            Log.e(TAG, "addOurEmotes: failed", e);
        }
    }
}
