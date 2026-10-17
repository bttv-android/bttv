package bttv.emote;

import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import bttv.Data;
import bttv.ResUtil;
import bttv.settings.Settings;

/** Channel-scoped catalogs for ordinary live chat. No mutable current-channel state. */
public final class LiveEmoteChannels {
    private static final String TAG = "BTTV31TRACE";
    private static final long RETRY_AFTER_MS = 60_000L;
    private static final ConcurrentHashMap<String, Catalog> CATALOGS = new ConcurrentHashMap<>();
    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(
            3, 3, 30L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(64), task -> {
                Thread thread = new Thread(task, "bttv31-channel");
                thread.setDaemon(true);
                return thread;
            });
    private static volatile boolean schedulingFailureLogged;

    private LiveEmoteChannels() {}

    private static final class Request {
        volatile Map<String, Emote> snapshot = Collections.emptyMap();
        boolean loading;
        boolean loaded;
        long retryAt;
    }

    private static final class Catalog {
        final Request[] providers = {new Request(), new Request(), new Request()};
        boolean readyLogged;
    }

    /** Called on selection and once per bridge invocation, never for individual tokens. */
    public static void ensureChannel(String channelId) {
        try {
            if (!validChannelId(channelId) || Data.ctx == null) {
                return;
            }
            Catalog catalog = CATALOGS.get(channelId);
            if (catalog == null) {
                Catalog created = new Catalog();
                Catalog previous = CATALOGS.putIfAbsent(channelId, created);
                catalog = previous == null ? created : previous;
                if (previous == null && LiveEmoteAutocomplete.DEBUG_LOGGING) {
                    log("channel selected id=" + channelId);
                }
            }
            for (Emotes.Source source : Emotes.Source.values()) {
                if (enabled(source)) {
                    schedule(channelId, source, catalog);
                }
            }
        } catch (Throwable failure) {
            // Even settings access, executor rejection or a dependency failure must fail open.
            if (!schedulingFailureLogged) {
                schedulingFailureLogged = true;
                log("channel scheduling unavailable (" + failure.getClass().getSimpleName() + ")");
            }
        }
    }

    static boolean validChannelId(String id) {
        if (id == null || id.isEmpty() || id.length() > 32) {
            return false;
        }
        boolean positive = false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            positive |= c != '0';
        }
        return positive;
    }

    private static boolean enabled(Emotes.Source source) {
        switch (source) {
            case BTTV: return ResUtil.getBooleanFromSettings(Settings.BTTVEmotesEnabled);
            case FFZ: return ResUtil.getBooleanFromSettings(Settings.FFZEmotesEnabled);
            case STV: return ResUtil.getBooleanFromSettings(Settings.SevenTVEmotesEnabled);
            default: return false;
        }
    }

    static Map<String, Emote> snapshot(String channelId, Emotes.Source source) {
        Catalog catalog = channelId == null ? null : CATALOGS.get(channelId);
        return catalog == null ? Collections.emptyMap() : catalog.providers[source.ordinal()].snapshot;
    }

    private static void schedule(String channelId, Emotes.Source source, Catalog catalog) {
        Request request = catalog.providers[source.ordinal()];
        synchronized (request) {
            if (request.loading || request.loaded || SystemClock.elapsedRealtime() < request.retryAt) {
                return;
            }
            request.loading = true;
        }
        try {
            WORKERS.execute(() -> {
                try {
                    Map<String, Emote> parsed = load(channelId, source);
                    synchronized (request) {
                        request.snapshot = Collections.unmodifiableMap(new HashMap<>(parsed));
                        request.loaded = true; // Includes successful empty catalogs.
                        request.loading = false;
                    }
                    EmoteCatalogEvents.published(source.name(), channelId);
                    if (LiveEmoteAutocomplete.DEBUG_LOGGING) {
                        log(providerName(source) + " channel " + channelId + " n=" + parsed.size());
                    }
                    reportReady(channelId, catalog);
                } catch (Throwable failure) {
                    failed(channelId, source, request, failure);
                }
            });
        } catch (Throwable failure) {
            failed(channelId, source, request, failure);
        }
    }

    private static void failed(String id, Emotes.Source source, Request request, Throwable failure) {
        synchronized (request) {
            request.retryAt = SystemClock.elapsedRealtime() + RETRY_AFTER_MS;
            request.loading = false;
        }
        String detail = failure instanceof LiveEmoteGlobals.HttpFailure
                ? "http=" + ((LiveEmoteGlobals.HttpFailure) failure).status
                : "error=" + failure.getClass().getSimpleName();
        log(providerName(source) + " channel " + detail);
    }

    private static void reportReady(String id, Catalog catalog) {
        synchronized (catalog) {
            if (catalog.readyLogged) {
                return;
            }
            for (Request request : catalog.providers) {
                synchronized (request) {
                    if (!request.loaded) {
                        return;
                    }
                }
            }
            catalog.readyLogged = true;
            if (LiveEmoteAutocomplete.DEBUG_LOGGING) {
                log("channel ready " + id);
            }
        }
    }

    static String providerName(Emotes.Source source) {
        return source == Emotes.Source.STV ? "7TV" : source.name();
    }

    private static void log(String message) {
        try {
            Log.i(TAG, message);
        } catch (Throwable ignored) {
            // Diagnostics are never a prerequisite for displaying chat.
        }
    }

    private static Map<String, Emote> load(String id, Emotes.Source source)
            throws IOException, JSONException {
        if (source == Emotes.Source.STV) {
            return load7TV(id);
        }
        try {
            String url = source == Emotes.Source.BTTV
                    ? "https://api.betterttv.net/3/cached/users/twitch/" + id
                    : "https://api.frankerfacez.com/v1/room/id/" + id;
            JSONObject response = new JSONObject(LiveEmoteGlobals.fetch(url));
            return source == Emotes.Source.BTTV ? parseBTTV(response) : parseFFZ(response);
        } catch (LiveEmoteGlobals.HttpFailure failure) {
            if (failure.status != 404) {
                throw failure;
            }
            log(providerName(source) + " channel http=404 (empty)");
            return Collections.emptyMap();
        }
    }

    static Map<String, Emote> parseBTTV(JSONObject response) throws JSONException {
        Map<String, Emote> result = parseArray(response.getJSONArray("channelEmotes"), Emotes.Source.BTTV);
        result.putAll(parseArray(response.getJSONArray("sharedEmotes"), Emotes.Source.BTTV));
        return result;
    }

    static Map<String, Emote> parseFFZ(JSONObject response) throws JSONException {
        String setId = response.getJSONObject("room").getString("set");
        JSONArray entries = response.getJSONObject("sets").getJSONObject(setId).getJSONArray("emoticons");
        JSONArray normalized = new JSONArray();
        for (int i = 0; i < entries.length(); i++) {
            try {
                JSONObject emote = entries.getJSONObject(i);
                JSONObject urls = emote.getJSONObject("urls");
                JSONObject animated = emote.optJSONObject("animated");
                boolean isAnimated = animated != null
                        && (!animated.optString("2").isEmpty() || !animated.optString("1").isEmpty());
                if (isAnimated) {
                    urls = animated;
                }
                String url = urls.optString("2");
                if (url.isEmpty()) {
                    url = urls.getString("1");
                }
                JSONObject value = new JSONObject()
                        .put("id", emote.getString("id"))
                        .put("code", emote.getString("name"))
                        .put("images", new JSONObject().put("2x", https(url)))
                        .put("imageType", isAnimated ? "gif" : "png");
                JSONObject owner = emote.optJSONObject("owner");
                if (owner != null) {
                    value.put("user", new JSONObject().put("displayName", owner.optString("display_name")));
                }
                normalized.put(value);
            } catch (JSONException ignored) {
                // One malformed entry must not hide the rest of the channel's emotes.
            }
        }
        if (entries.length() != 0 && normalized.length() == 0) {
            throw new JSONException("No usable FFZ channel entries");
        }
        return parseArray(normalized, Emotes.Source.FFZ);
    }

    private static Map<String, Emote> load7TV(String id) throws IOException, JSONException {
        String[] hosts = {"https://7tv.io/v3", "https://api.7tv.app/v3"};
        Exception failure = null;
        int notFound = 0;
        for (String host : hosts) {
            try {
                return parse7TVUser(new JSONObject(LiveEmoteGlobals.fetch(host + "/users/twitch/" + id)));
            } catch (LiveEmoteGlobals.HttpFailure error) {
                log("7TV channel " + id + " http=" + error.status + " host=" + host);
                if (error.status == 404) {
                    notFound++;
                } else {
                    failure = error;
                }
            } catch (IOException | JSONException error) {
                failure = error;
            }
        }
        if (notFound == hosts.length) {
            return Collections.emptyMap();
        }
        if (failure instanceof JSONException) {
            throw (JSONException) failure;
        }
        throw (IOException) failure;
    }

    private static Map<String, Emote> parse7TVUser(JSONObject user) throws IOException, JSONException {
        if (!user.has("emote_set") && !user.has("emote_set_id")) {
            throw new JSONException("Missing 7TV set metadata");
        }
        JSONObject set = user.optJSONObject("emote_set");
        String setId = set == null ? user.optString("emote_set_id", "") : set.optString("id", "");
        if (user.opt("emote_set") instanceof String) {
            setId = user.getString("emote_set");
        }
        JSONArray embedded = set == null ? null : set.optJSONArray("emotes");
        if (embedded != null && (embedded.length() != 0 || setId.isEmpty())) {
            try {
                return parseArray(embedded, Emotes.Source.STV);
            } catch (JSONException error) {
                if (setId.isEmpty()) {
                    throw error;
                }
            }
        }
        if (!setId.isEmpty()) {
            // Some user responses omit the contents or embed an empty/incomplete set.
            if (!setId.matches("[A-Za-z0-9_-]{1,64}")) {
                throw new JSONException("Invalid 7TV set ID");
            }
            JSONObject fetched = new JSONObject(LiveEmoteGlobals.fetch("https://api.7tv.app/v3/emote-sets/" + setId));
            return parseArray(fetched.getJSONArray("emotes"), Emotes.Source.STV);
        }
        if (set != null) {
            throw new JSONException("Incomplete 7TV set");
        }
        return Collections.emptyMap();
    }

    private static Map<String, Emote> parseArray(JSONArray entries, Emotes.Source source) throws JSONException {
        Map<String, Emote> result = new HashMap<>();
        for (int i = 0; i < entries.length(); i++) {
            try {
                JSONObject value = entries.getJSONObject(i);
                if (source == Emotes.Source.STV) {
                    // Normalize only channel data; retain the proven global parser and WEBP selection.
                    value = new JSONObject(value.toString());
                    JSONObject data = value.getJSONObject("data");
                    JSONObject host = data.getJSONObject("host");
                    String hostUrl = https(host.getString("url"));
                    host.put("url", hostUrl.substring("https:".length()));
                    if (data.isNull("owner")) {
                        data.remove("owner");
                    }
                }
                Emote emote = Emote.fromJson(value, source);
                emote.url = https(emote.url);
                if (emote.id.isEmpty() || emote.code.isEmpty() || emote.url.endsWith("/null")) {
                    throw new JSONException("Incomplete channel emote");
                }
                result.put(emote.code, emote);
            } catch (JSONException ignored) {
                // Ignore isolated broken entries, but retry a wholly unusable nonempty response.
            }
        }
        if (entries.length() != 0 && result.isEmpty()) {
            throw new JSONException("No usable channel entries");
        }
        return result;
    }

    private static String https(String url) throws JSONException {
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        if (url.startsWith("https://")) {
            return url;
        }
        throw new JSONException("Non-HTTPS emote URL");
    }
}
