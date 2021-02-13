package bttv;

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

    @Override
    public String toString() {
        return "Emote(" + id + ", " + code + ", " + imageType + ")";
    }
}
