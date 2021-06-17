package bttv.api;

import bttv.Res;
import bttv.Util;
import tv.twitch.android.shared.chat.emotecard.EmoteCardViewDelegate;

public class Strings {
    public static String getEmoteAddedByBttvString(EmoteCardViewDelegate delegate) {
        return Util.getLocaleString(delegate.getContext(), Res.strings.bttv_emote_added_by_bttv);
    }
}
