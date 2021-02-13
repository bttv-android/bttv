package bttv;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Emote {
    String id;
    String code;
    String imageType;

    public Emote(String id, String code, String imageType) {
        this.id = id;
        this.code = code;
        this.imageType = imageType;
    }

    public static Emote fromJson(JSONObject jsonObject) {
        return new Emote(jsonObject.getString("id"), jsonObject.getString("code"), jsonObject.getString("imageType"));
    }

    public static List<Emote> fromJSONArray(String json) {
        JSONArray arr = new JSONArray(json);
        return fromJSONArray(arr);
    }

    public static List<Emote> fromJSONArray(JSONArray arr) {
        ArrayList<Emote> ret = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            ret.add(Emote.fromJson(obj));
        }
        return ret;
    }

    @Override
    public String toString() {
        return "Emote(" + id + ", " + code + ", " + imageType + ")";
    }
}
