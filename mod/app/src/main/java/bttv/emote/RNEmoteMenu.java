package bttv.emote;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bttv.Data;
import bttv.Network;
import bttv.RNNetwork;

/**
 * Adds our emotes to the emote menu of the react native chat.
 * The menu is filled from the "ChatEmoteSets" GraphQL operation, we send that request ourselves
 * and add an emote set per provider to the answer.
 */
public class RNEmoteMenu {
    private static final String TAG = "LBTTVRNEmoteMenu";
    private static final String OPERATION = "ChatEmoteSets";
    private static final String CHANNEL_OPERATION = "ChatChannelLockedEmotes";
    private static final int WAIT_FOR_EMOTES_MS = 4000;
    private static final String EMOTE_TYPE = "MEGA_COMMERCE";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private static final String BTTV_ICON = "https://cdn.betterttv.net/assets/logos/mascot.png";
    private static final String FFZ_ICON = "https://www.frankerfacez.com/static/images/cover/zreknarf.png";
    // the official logo is black on transparent, this is a white version on a dark tile
    private static final String STV_ICON = "data:image/png;base64,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";

    /**
     * @param fallback sends the request the regular way, used when we fail
     * @return true if we answer this request, it must not be sent then
     */
    public static boolean intercept(final Object module, String method, final String url, final int requestId,
                                    Object headers, final String body, final Runnable fallback) throws Exception {
        if (body == null || !"POST".equals(method) || !body.contains("\"" + OPERATION + "\"")) {
            return false;
        }
        final List<String[]> requestHeaders = RNNetwork.readHeaders(headers);

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    proxy(module, url, requestId, requestHeaders, body);
                } catch (Throwable t) {
                    Log.e(TAG, "proxy failed, using fallback: ", t);
                    fallback.run();
                }
            }
        });
        return true;
    }

    private static void proxy(Object module, String url, int requestId, List<String[]> requestHeaders, String body) throws Exception {
        boolean isBatch = body.trim().startsWith("[");
        JSONArray operations = isBatch ? new JSONArray(body) : new JSONArray().put(new JSONObject(body));

        int emoteSetsIndex = -1;
        int channel = Data.currentBroadcasterId;
        boolean channelFromRequest = false;
        for (int i = 0; i < operations.length(); i++) {
            JSONObject operation = operations.getJSONObject(i);
            String name = operation.optString("operationName");
            if (OPERATION.equals(name)) {
                emoteSetsIndex = i;
            } else if (CHANNEL_OPERATION.equals(name)) {
                JSONObject variables = operation.optJSONObject("variables");
                String id = variables == null ? "" : variables.optString("channelOwnerID", "");
                if (!id.isEmpty()) {
                    channel = Integer.parseInt(id);
                    channelFromRequest = true;
                }
            }
        }
        if (emoteSetsIndex == -1) {
            throw new IllegalStateException("operation not found");
        }
        if (channel > 0 && channel != Data.currentBroadcasterId) {
            Data.setCurrentBroadcasterId(channel);
        } else {
            Emotes.ensureChannelEmotes(Data.ctx, channel);
        }

        // the real request
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        int status;
        Map<String, String> responseHeaders = new LinkedHashMap<>();
        String responseBody;
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            for (String[] header : requestHeaders) {
                String name = header[0].toLowerCase();
                if (name.equals("content-length") || name.equals("accept-encoding") || name.equals("host")) {
                    continue;
                }
                connection.setRequestProperty(header[0], header[1]);
            }
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.getBytes("UTF-8"));
            }
            status = connection.getResponseCode();
            if (status != 200) {
                throw new IllegalStateException("unexpected status " + status);
            }
            for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                if (header.getKey() == null || header.getValue().isEmpty()) {
                    continue;
                }
                String name = header.getKey().toLowerCase();
                if (name.equals("content-encoding") || name.equals("content-length") || name.equals("transfer-encoding")) {
                    continue;
                }
                responseHeaders.put(header.getKey(), header.getValue().get(header.getValue().size() - 1));
            }
            responseBody = readAll(connection.getInputStream());
        } finally {
            connection.disconnect();
        }

        waitForEmotes(channel);

        String newBody = responseBody;
        try {
            JSONArray responses = isBatch ? new JSONArray(responseBody) : new JSONArray().put(new JSONObject(responseBody));
            JSONObject currentUser = responses.getJSONObject(emoteSetsIndex).getJSONObject("data").getJSONObject("currentUser");
            JSONArray emoteSets = currentUser.getJSONArray("emoteSets");
            int added = addEmoteSets(emoteSets, channel);
            newBody = isBatch ? responses.toString() : responses.getJSONObject(0).toString();
            Log.d(TAG, "proxy: added " + added + " emotes for channel " + channel + " (current " + Data.currentBroadcasterId
                    + ", from request " + channelFromRequest + ") to request " + requestId);
        } catch (Throwable t) {
            // still a valid answer
            Log.e(TAG, "proxy: could not add emote sets: ", t);
        }

        RNNetwork.emitResponse(module, requestId, status, responseHeaders, url, newBody);
    }

    // the answer is built from the emotes we have, so the lists of this channel should be loaded
    private static void waitForEmotes(int channel) throws InterruptedException {
        long end = System.currentTimeMillis() + WAIT_FOR_EMOTES_MS;
        while (System.currentTimeMillis() < end) {
            boolean globals = !Emotes.globalEmotesBTTV.isEmpty();
            boolean channelLoaded = channel <= 0
                    || (isLoaded(Emotes.Source.BTTV, Emotes.channelEmotesBTTV, channel)
                    && isLoaded(Emotes.Source.FFZ, Emotes.channelEmotesFFZ, channel)
                    && isLoaded(Emotes.Source.STV, Emotes.channelEmotes7TV, channel));
            if (globals && channelLoaded) {
                return;
            }
            Thread.sleep(50);
        }
        Log.w(TAG, "waitForEmotes: not all emotes of channel " + channel + " are loaded");
    }

    private static boolean isLoaded(Emotes.Source source, Map<Integer, Set<String>> channelEmotes, int channel) {
        // a channel without an account at the provider never gets an entry
        return channelEmotes.containsKey(channel) || Network.isChannelRequestFinished(source, channel);
    }

    private static int addEmoteSets(JSONArray emoteSets, int channel) throws Exception {
        // use what Twitch sent as templates, so the shape is the one the chat expects
        JSONObject setTemplate = null;
        JSONObject emoteTemplate = null;
        JSONObject ownerTemplate = null;
        for (int i = 0; i < emoteSets.length(); i++) {
            JSONObject set = emoteSets.getJSONObject(i);
            JSONArray emotes = set.optJSONArray("emotes");
            if (setTemplate == null) {
                setTemplate = set;
            }
            if (emoteTemplate == null && emotes != null && emotes.length() > 0) {
                emoteTemplate = emotes.getJSONObject(0);
            }
            if (ownerTemplate == null && set.optJSONObject("owner") != null) {
                ownerTemplate = set.getJSONObject("owner");
                setTemplate = set;
            }
        }
        if (setTemplate == null || emoteTemplate == null) {
            throw new IllegalStateException("no templates");
        }
        if (ownerTemplate == null) {
            // without an owner the menu lists our sets under the global Twitch emotes
            Log.w(TAG, "addEmoteSets: no set with an owner in " + emoteSets.length() + " sets, building one");
            ownerTemplate = new JSONObject();
            ownerTemplate.put("id", "");
            ownerTemplate.put("login", "");
            ownerTemplate.put("displayName", "");
            ownerTemplate.put("profileImageURL", "");
            ownerTemplate.put("__typename", "User");
        }

        // ours go first, so the "Unlocked Emotes" tab of the menu jumps to them
        JSONArray original = new JSONArray(emoteSets.toString());
        while (emoteSets.length() > 0) {
            emoteSets.remove(0);
        }

        // emotes of the current channel first, then the global ones
        // the chat caches sets by their id, so the sets of a channel have the channel in their id
        int added = 0;
        added += addEmoteSet(emoteSets, setTemplate, emoteTemplate, ownerTemplate, "BetterTTV Channel", "betterttv-channel-" + channel, BTTV_ICON,
                channelEmotes(Emotes.Source.BTTV, Emotes.channelEmotesBTTV.get(channel), channel));
        added += addEmoteSet(emoteSets, setTemplate, emoteTemplate, ownerTemplate, "FrankerFaceZ Channel", "frankerfacez-channel-" + channel, FFZ_ICON,
                channelEmotes(Emotes.Source.FFZ, Emotes.channelEmotesFFZ.get(channel), channel));
        added += addEmoteSet(emoteSets, setTemplate, emoteTemplate, ownerTemplate, "7TV Channel", "seventv-channel-" + channel, STV_ICON,
                channelEmotes(Emotes.Source.STV, Emotes.channelEmotes7TV.get(channel), channel));
        added += addEmoteSet(emoteSets, setTemplate, emoteTemplate, ownerTemplate, "BetterTTV Global", "betterttv-global", BTTV_ICON,
                globalEmotes(Emotes.globalEmotesBTTV.values()));
        added += addEmoteSet(emoteSets, setTemplate, emoteTemplate, ownerTemplate, "FrankerFaceZ Global", "frankerfacez-global", FFZ_ICON,
                globalEmotes(Emotes.globalEmotesFFZ.values()));
        added += addEmoteSet(emoteSets, setTemplate, emoteTemplate, ownerTemplate, "7TV Global", "seventv-global", STV_ICON,
                globalEmotes(Emotes.globalEmotes7TV.values()));
        for (int i = 0; i < original.length(); i++) {
            emoteSets.put(original.get(i));
        }
        return added;
    }

    private static Map<String, Emote> channelEmotes(Emotes.Source source, Set<String> channelCodes, int channel) {
        Map<String, Emote> emotes = new LinkedHashMap<>();
        if (channelCodes == null) {
            return emotes;
        }
        for (String code : new ArrayList<>(channelCodes)) {
            Emote emote = Emotes.getEmote(channel, code);
            if (emote != null && emote.source == source) {
                emotes.put(emote.id, emote);
            }
        }
        return emotes;
    }

    private static Map<String, Emote> globalEmotes(Collection<Emote> globals) {
        Map<String, Emote> emotes = new LinkedHashMap<>();
        for (Emote emote : new ArrayList<>(globals)) {
            emotes.put(emote.id, emote);
        }
        return emotes;
    }

    private static int addEmoteSet(JSONArray emoteSets, JSONObject setTemplate, JSONObject emoteTemplate, JSONObject ownerTemplate,
                                   String displayName, String login, String icon, Map<String, Emote> emotes) throws Exception {
        if (emotes.isEmpty()) {
            return 0;
        }

        String setId = "BTTV-set-" + login;
        JSONArray emotesJson = new JSONArray();
        for (Emote emote : emotes.values()) {
            JSONObject emoteJson = new JSONObject(emoteTemplate.toString());
            emoteJson.put("id", "BTTV-" + emote.id);
            emoteJson.put("setID", setId);
            emoteJson.put("token", emote.code);
            emoteJson.put("assetType", "gif".equals(emote.imageType) ? "ANIMATED" : "STATIC");
            // the order of the sets Twitch sends changes with every request, the template may be any emote.
            // The menu groups emotes of the type GLOBALS (and some others) on its own, ignoring the owner of their set,
            // MEGA_COMMERCE emotes are listed under the owner of their set
            if (emoteJson.has("type")) {
                emoteJson.put("type", EMOTE_TYPE);
            }
            if (emoteJson.has("modifiers")) {
                emoteJson.put("modifiers", JSONObject.NULL);
            }
            emotesJson.put(emoteJson);
        }

        JSONObject set = new JSONObject(setTemplate.toString());
        set.put("id", setId);
        set.put("emotes", emotesJson);
        if (ownerTemplate != null) {
            JSONObject owner = new JSONObject(ownerTemplate.toString());
            owner.put("id", "BTTV-owner-" + login);
            owner.put("login", login);
            owner.put("displayName", displayName);
            if (owner.has("profileImageURL")) {
                owner.put("profileImageURL", icon);
            }
            set.put("owner", owner);
        }
        emoteSets.put(set);
        return emotes.size();
    }

    private static String readAll(InputStream in) throws Exception {
        try (InputStream stream = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        }
    }
}
