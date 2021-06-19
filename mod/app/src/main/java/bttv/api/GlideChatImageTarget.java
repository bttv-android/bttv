package bttv.api;

import android.util.Log;

import tv.twitch.android.shared.ui.elements.span.UrlDrawable;

public class GlideChatImageTarget {

    public static boolean getIsBttvEmote(tv.twitch.android.shared.ui.elements.span.GlideChatImageTarget target) {
        UrlDrawable urlDrawable = target.mUrlDrawable;
        if (urlDrawable == null) {
            Log.i("LBTTVDEBUG", "getIsBttvEmote: urlDrawable) is null");
            return false;
        }
        String url = urlDrawable.getUrl();
        Log.i("LBTTVDEBUG", "getIsBttvEmote: " + url);
        return url.contains("betterttv.net") || url.contains("7tv.app"); // TODO: find better way of determining this
    }
}
