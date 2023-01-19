package bttv.emote;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import tv.twitch.android.models.emotes.EmoteModelAssetType;

public class Emote {
    public String id;
    public Emotes.Source source;
    public String code;
    public String url;
    public String imageType; // this is really only used to start the Gif if it is a Gif
    public String owner;

    public Emote(String id, Emotes.Source source, String code, String url, String imageType, String owner) {
        this.id = id;
        this.source = source;
        this.code = code;
        this.url = url;
        this.imageType = imageType;
        this.owner = owner;
    }

    public static Emote fromJson(JSONObject jsonObject, Emotes.Source source) throws JSONException {
        String id = jsonObject.getString("id");
        String code;
        String url;
        String imageType = "";
        String owner = null;

        switch (source) {
            case BTTV:
                code = jsonObject.getString("code");
                url = "https://cdn.betterttv.net/emote/" + id + "/2x.webp";
                imageType = jsonObject.getString("imageType");
                owner = null; // we don't get the owner :/
                break;
            case FFZ:
                code = jsonObject.getString("code");
                JSONObject images = jsonObject.getJSONObject("images");
                if (images.has("2x") && !images.isNull("2x")) {
                    url = images.getString("2x");
                } else {
                    url = images.getString("1x");
                }
                imageType = jsonObject.getString("imageType");
                if (jsonObject.has("user"))
                    owner = jsonObject.getJSONObject("user").getString("displayName");
                break;
            case STV:
                code = jsonObject.getString("name");
                JSONArray urls = jsonObject.getJSONArray("urls");

                JSONArray urlArr; // urlArr = ["1", "https://.."]

                if (urls.length() >= 2) {
                    urlArr = urls.getJSONArray(1); // 2x
                } else {
                    urlArr = urls.getJSONArray(0); // 1x
                }

                url = urlArr.getString(1);

                String mime = jsonObject.getString("mime");
                if (mime.equals("image/gif")) {
                    imageType = "gif";
                } else {
                    imageType = "png";
                }
                owner = jsonObject.getJSONObject("owner").getString("display_name");
                break;
            default:
                Log.w("LBTTVEmoteFromJson", "source unknown: " + source);
                url = "";
                code = "";
                imageType = "";
                owner = null;
        }

        return new Emote(id, source, code, url, imageType, owner);
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

    public boolean isAnimated() {
        if (this.source != Emotes.Source.STV) {
            return this.imageType.equals("gif");
        } else {
            return true;
        }
    }

    public EmoteModelAssetType getAssetType() {
        return this.isAnimated() ? EmoteModelAssetType.ANIMATED : EmoteModelAssetType.STATIC;
    }
}
