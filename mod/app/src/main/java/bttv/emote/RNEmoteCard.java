package bttv.emote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

import bttv.RNNetwork;

/**
 * Answers the "EmoteCard" GraphQL request of the react native chat for our emotes,
 * Twitch does not know them and would show an error.
 */
public class RNEmoteCard {
    private static final String TAG = "LBTTVRNEmoteCard";
    private static final String OPERATION = "EmoteCard";
    private static final long ACTIVE_MS = 15000;
    private static volatile Emote activeEmote = null;
    private static volatile long activeUntil = 0;
    private static final Map<String, String> HEADERS = Collections.singletonMap("content-type", "application/json");

    /**
     * @param module react native NetworkingModule
     * @param body request body
     * @return true if we answer this request, it must not be sent then
     */
    public static boolean intercept(final Object module, final String url, final int requestId, String body) throws Exception {
        if (body == null || !body.contains("\"" + OPERATION + "\"")) {
            return false;
        }
        if (!body.contains("BTTV-")) {
            // a card for a Twitch emote
            activeEmote = null;
            return false;
        }

        final boolean isBatch = body.trim().startsWith("[");
        JSONObject operation;
        if (isBatch) {
            JSONArray batch = new JSONArray(body);
            if (batch.length() != 1) {
                // other operations need a real answer
                return false;
            }
            operation = batch.getJSONObject(0);
        } else {
            operation = new JSONObject(body);
        }

        if (!OPERATION.equals(operation.optString("operationName"))) {
            return false;
        }
        JSONObject variables = operation.optJSONObject("variables");
        String emoteId = variables == null ? null : variables.optString("emoteID", null);
        String realId = emoteId == null ? null : EmoteUrlUtil.extractBTTVId(emoteId);
        if (realId == null) {
            return false;
        }
        Emote emote = Emotes.getEmoteById(realId);
        if (emote == null) { // triggers error dialog
            Log.w(TAG, "intercept: unknown emote " + emoteId);
            return false;
        }

        JSONObject response = buildResponse(emoteId, emote);
        final String responseBody = isBatch ? new JSONArray().put(response).toString() : response.toString();

        // answer after sendRequest returned
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    RNNetwork.emitResponse(module, requestId, 200, HEADERS, url, responseBody);
                } catch (Throwable t) {
                    Log.e(TAG, "respond: ", t);
                }
            }
        });
        Log.d(TAG, "intercept: answering request " + requestId + " for " + emoteId);
        activeUntil = System.currentTimeMillis() + ACTIVE_MS;
        activeEmote = emote;
        return true;
    }

    /**
     * The card describes our emotes as global Twitch emotes, the wording is part of the chat's code.
     * While one of our cards is shown we replace that wording as it is laid out.
     * @param text a piece of text react native is about to lay out
     */
    public static String swapText(String text) {
        Emote emote = activeEmote;
        if (emote == null || text == null) {
            return text;
        }
        if (System.currentTimeMillis() > activeUntil) {
            activeEmote = null;
            return text;
        }
        String lower = text.trim().toLowerCase();
        if (!lower.equals("twitch global emote") && !lower.equals("global twitch emote")) {
            return text;
        }
        return providerName(emote) + " Emote";
    }

    private static String providerName(Emote emote) {
        switch (emote.source) {
            case FFZ:
                return "FrankerFaceZ";
            case STV:
                return "7TV";
            default:
                return "BetterTTV";
        }
    }

    private static JSONObject buildResponse(String emoteId, Emote emote) throws Exception {
        JSONObject emoteJson = new JSONObject();
        emoteJson.put("id", emoteId);
        emoteJson.put("token", emote.code);
        emoteJson.put("type", "GLOBALS");
        emoteJson.put("subscriptionTier", JSONObject.NULL);
        emoteJson.put("setID", "0");
        emoteJson.put("owner", JSONObject.NULL);
        emoteJson.put("subscriptionSummaries", new JSONArray());
        emoteJson.put("bitsBadgeTierSummary", JSONObject.NULL);
        emoteJson.put("__typename", "Emote");

        JSONObject extensions = new JSONObject();
        extensions.put("durationMilliseconds", 1);
        extensions.put("operationName", OPERATION);
        extensions.put("requestID", "bttv-android");

        JSONObject response = new JSONObject();
        response.put("data", new JSONObject().put("emote", emoteJson));
        response.put("extensions", extensions);
        return response;
    }
}
