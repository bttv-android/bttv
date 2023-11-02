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
    public String imageType;
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
                url = "https://cdn.betterttv.net/emote/" + id + "/2x";
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
                JSONObject data = jsonObject.getJSONObject("data");
                code = data.getString("name");

                JSONObject hostObj = data.getJSONObject("host");
                StringBuilder urlBuilder = new StringBuilder("https:"); // for some godforsaken reason this part is missing
                urlBuilder.append(hostObj.getString("url"));
                urlBuilder.append('/');

                JSONArray files = hostObj.getJSONArray("files");

                // the following is semantically equivalent to this:
                // - filter out non-webp files
                // - if length > 1 choose the second option (the 2x size, which has a height of 64)
                //   else choose the first item (low quality)

                String smallerThan64 = null;
                String geThan64 = null;

                for (int i = 0; i < files.length(); i++) {
                    JSONObject emote = files.getJSONObject(i);
                    if (!emote.getString("format").equals("WEBP")) {
                        continue;
                    }
                    String name = emote.getString("name");
                    if (emote.getInt("height") >= 64 && geThan64 == null) {
                        geThan64 = name;
                    } else if (smallerThan64 == null) {
                        smallerThan64 = name;
                    }
                }
                String selected = null;
                if (geThan64 != null) {
                    selected = geThan64;
                } else {
                    selected = smallerThan64; // if missing, we are f'ed anyway
                }

                urlBuilder.append(selected);

                url = urlBuilder.toString();

                if (data.getBoolean("animated")) {
                    imageType = "gif";
                } else {
                    imageType = "png";
                }
                owner = data.getJSONObject("owner").getString("display_name");
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
        JSONArray arr;
        if (source == Emotes.Source.STV) {
            JSONObject response = new JSONObject(json);
            JSONObject emoteSet;
            // global emotes already is only the emote set,
            // for user-related requests get it
            if (response.has("emote_set")) {
                emoteSet = response.getJSONObject("emote_set");
            } else {
                emoteSet = response;
            }
            arr = emoteSet.getJSONArray("emotes");
        } else {
            arr = new JSONArray(json);
        }
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
