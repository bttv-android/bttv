package bttv.api;

import android.util.Log;

import java.util.List;

import tv.twitch.android.shared.emotes.emotepicker.models.EmoteUiSet;

public class EmotePicker {
    public static void extendList(List<EmoteUiSet> list) {
        try {
            bttv.emote.EmotePicker.extendList(list);
        } catch (Throwable e) {
            Log.e("LBTTVEmotePicker", "extendlist failed: ", e);
        }
    }
}
