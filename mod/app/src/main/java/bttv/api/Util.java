package bttv.api;


import io.reactivex.Single;

public class Util {
    final static String TAG = "LBTTVUtil";

    public static Single<Boolean> getTrueSingle() {
        return Single.just(true);
    }
}
