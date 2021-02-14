package bttv;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class Emote {
    public static final int BTTV = 1;
    public static final int FFZ = 2;
    String id;
    String code;
    String url;

    public Emote(String id, String code, String url) {
        this.id = id;
        this.code = code;
        this.url = url;
    }

    public static Emote fromJson(JSONObject jsonObject, int source) {
        String id = jsonObject.getString("id");
        String code = jsonObject.getString("code");
        String url;
        if (source == BTTV) {
            url = "https://cdn.betterttv.net/emote/" + id + "/1x";
        } else if (source == FFZ) {
            JSONObject images = jsonObject.getJSONObject("images");
            url = images.getString("1x");
        } else {
            Log.w("BTTVEmoteFromJson", "source unknown: " + source);
            url = "";
        }
        return new Emote(id, code, url);
    }

    public static List<Emote> fromJSONArray(String json, int source) {
        JSONArray arr = new JSONArray(json);
        return fromJSONArray(arr, source);
    }

    public static List<Emote> fromJSONArray(JSONArray arr, int source) {
        ArrayList<Emote> ret = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            ret.add(Emote.fromJson(obj, source));
        }
        return ret;
    }

    @Override
    public String toString() {
        return "Emote(" + id + ", " + code + ", " + url + ")";
    }
}
