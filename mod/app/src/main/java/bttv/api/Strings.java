package bttv.api;

import bttv.Res;
import bttv.ResUtil;
import tv.twitch.android.shared.chat.emotecard.EmoteCardViewDelegate;

public class Strings {
    public static String getEmoteAddedByBttvString(EmoteCardViewDelegate delegate) {
        return ResUtil.getLocaleString(delegate.getContext(), Res.strings.bttv_emote_added_by_bttv);
    }

    public static int getStringId(String strName) {
        return ResUtil.getStringId(strName);
    }
}
