package bttv.emote;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Emote {
    public static final int BTTV = 1;
    public static final int FFZ = 2;
    public String id;
    public String code;
    public String url;
    public String imageType;

    public Emote(String id, String code, String url, String imageType) {
        this.id = id;
        this.code = code;
        this.url = url;
        this.imageType = imageType;
    }

    public static Emote fromJson(JSONObject jsonObject, int source) throws JSONException {
        String id = jsonObject.getString("id");
        String code = jsonObject.getString("code");
        String url;
        String imageType = "";

        if (source == BTTV) {
            url = "https://cdn.betterttv.net/emote/" + id + "/1x";
            imageType = jsonObject.getString("imageType");
        } else if (source == FFZ) {
            JSONObject images = jsonObject.getJSONObject("images");
            url = images.getString("1x");
            imageType = "png";
        } else {
            Log.w("LBTTVEmoteFromJson", "source unknown: " + source);
            url = "";
            imageType = "";
        }
        return new Emote(id, code, url, imageType);
    }

    public static List<Emote> fromJSONArray(String json, int source) throws JSONException {
        JSONArray arr = new JSONArray(json);
        return fromJSONArray(arr, source);
    }

    public static List<Emote> fromJSONArray(JSONArray arr, int source) throws JSONException {
        ArrayList<Emote> ret = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            ret.add(Emote.fromJson(obj, source));
        }
        return ret;
    }

    @Override
    public String toString() {
        return "Emote(" + id + ", " + code + ", " + url + " imageType: " + imageType + ")";
    }
}
