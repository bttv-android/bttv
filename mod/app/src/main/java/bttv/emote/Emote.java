package bttv.emote;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Emote {
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

    public static Emote fromJson(JSONObject jsonObject, Emotes.Source source) throws JSONException {
        String id = jsonObject.getString("id");
        String code;
        String url;
        String imageType = "";

        switch (source) {
            case BTTV:
                code = jsonObject.getString("code");
                url = "https://cdn.betterttv.net/emote/" + id + "/1x";
                imageType = jsonObject.getString("imageType");
                break;
            case FFZ:
                code = jsonObject.getString("code");
                JSONObject images = jsonObject.getJSONObject("images");
                url = images.getString("1x");
                imageType = "png";
                break;
            case STV:
                code = jsonObject.getString("name");
                JSONArray urls = jsonObject.getJSONArray("urls");
                JSONArray oneX = urls.getJSONArray(0);
                url = oneX.getString(1);
                String mime = jsonObject.getString("mime");
                if (mime.equals("image/gif")) {
                    imageType = "gif";
                } else {
                    imageType = "png";
                }
                break;
            default:
                Log.w("LBTTVEmoteFromJson", "source unknown: " + source);
                url = "";
                code = "";
                imageType = "";
        }

        return new Emote(id, code, url, imageType);
    }

    public static List<Emote> fromJSONArray(String json, Emotes.Source source) throws JSONException {
        JSONArray arr = new JSONArray(json);
        return fromJSONArray(arr, source);
    }

    public static List<Emote> fromJSONArray(JSONArray arr, Emotes.Source source) throws JSONException {
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
