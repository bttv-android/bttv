package bttv.badge;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Global assignments only. No Twitch models, viewer identity or emote catalogs. */
public final class BadgeCatalog {
    public static final int MAX_METADATA = 2 * 1024 * 1024;
    public static final int MAX_ASSIGNMENTS = 100_000;
    public static final int MAX_DEFINITIONS = 64;
    public static final int MAX_PER_AUTHOR = 8;

    public enum Provider {
        BTTV("https://api.betterttv.net/3/cached/badges/twitch"),
        FFZ("https://api.frankerfacez.com/v1/badges/ids");

        public final String endpoint;
        Provider(String endpoint) { this.endpoint = endpoint; }
    }

    public static final class Badge {
        public final Provider provider;
        public final String id;
        public final String title;
        public final String url;
        public final Integer background;

        private Badge(Provider provider, String id, String title, String url, Integer background) {
            this.provider = provider;
            this.id = id;
            this.title = title;
            this.url = url;
            this.background = background;
        }

        public String key() { return provider.name() + ":" + id; }
    }

    public static final class Snapshot {
        public final Provider provider;
        public final Map<String, Badge> definitions;
        private final Map<String, List<Badge>> authors;

        private Snapshot(Provider provider, Map<String, Badge> definitions,
                         Map<String, List<Badge>> authors) {
            this.provider = provider;
            this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
            this.authors = Collections.unmodifiableMap(authors);
        }

        public List<Badge> forAuthor(String authorId) {
            String id = authorId(authorId);
            List<Badge> found = id == null ? null : authors.get(id);
            return found == null ? Collections.<Badge>emptyList() : found;
        }
    }

    private BadgeCatalog() {}

    /** Canonical decimal identity, without a potentially overflowing numeric conversion. */
    public static String authorId(String value) {
        if (value == null || value.isEmpty() || value.length() > 20) return null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') return null;
        }
        int start = 0;
        while (start < value.length() && value.charAt(start) == '0') start++;
        return start == value.length() ? null : value.substring(start);
    }

    public static String assetUrl(Provider provider, String value) throws IOException {
        if (value == null || value.length() > 256) throw invalid();
        if (value.startsWith("//")) value = "https:" + value;
        try {
            URI uri = new URI(value);
            String host = provider == Provider.BTTV ? "cdn.betterttv.net" : "cdn.frankerfacez.com";
            String path = uri.getRawPath();
            boolean allowed = provider == Provider.BTTV
                    ? path != null && path.matches("/badges/[a-z_]{1,48}\\.svg")
                    : path != null && path.matches("/badge/[1-9][0-9]{0,8}/[124]");
            if (!"https".equals(uri.getScheme()) || !host.equals(uri.getHost())
                    || uri.getRawUserInfo() != null || uri.getPort() != -1
                    || uri.getRawQuery() != null || uri.getRawFragment() != null || !allowed) throw invalid();
            return uri.toASCIIString();
        } catch (java.net.URISyntaxException failure) {
            throw invalid();
        }
    }

    private static IOException invalid() { return new IOException("Invalid badge metadata"); }

    private static String title(JSONObject object, String field) throws Exception {
        String value = object.getString(field);
        if (value.isEmpty() || value.length() > 80) throw invalid();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 32 || c == 127 || c == '<' || c == '>' || Character.isSurrogate(c)
                    || (c >= 0x202a && c <= 0x202e) || (c >= 0x2066 && c <= 0x2069)) throw invalid();
        }
        return value;
    }

    private static String badgeId(Object value) throws IOException {
        String id = value == null ? null : authorId(value.toString());
        if (id == null || id.length() > 9) throw invalid();
        return id;
    }

    private static void boundedJson(String json) throws IOException {
        if (json == null || json.length() > MAX_METADATA) throw invalid();
        int depth = 0;
        boolean quoted = false, escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
            } else if (c == '"') quoted = true;
            else if (c == '[' || c == '{') { if (++depth > 12) throw invalid(); }
            else if (c == ']' || c == '}') { if (--depth < 0) throw invalid(); }
        }
        if (depth != 0 || quoted) throw invalid();
    }

    /** Parse privately and return a complete snapshot; any invalid response leaves publication to caller. */
    public static Snapshot parse(Provider provider, String json) throws Exception {
        boundedJson(json);
        Map<String, Badge> definitions = new LinkedHashMap<>();
        Map<String, Map<String, Badge>> assignments = new HashMap<>();
        int count = 0;
        if (provider == Provider.BTTV) {
            JSONArray records = new JSONArray(json);
            if (records.length() > MAX_ASSIGNMENTS) throw invalid();
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.getJSONObject(i);
                String author = authorId(record.getString("providerId"));
                if (author == null) throw invalid();
                JSONObject item = record.getJSONObject("badge");
                String id = badgeId(item.get("type"));
                Badge badge = new Badge(provider, id, "BTTV " + title(item, "description"),
                        assetUrl(provider, item.getString("svg")), null);
                addDefinition(definitions, badge);
                addAssignment(assignments, author, badge);
            }
        } else {
            JSONObject root = new JSONObject(json);
            JSONArray items = root.getJSONArray("badges");
            if (items.length() > MAX_DEFINITIONS) throw invalid();
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String id = badgeId(item.get("id"));
                String color = item.isNull("color") ? null : item.getString("color");
                Integer background = null;
                if (color != null) {
                    if (!color.matches("#[0-9a-fA-F]{6}")) throw invalid();
                    background = (int) (0xff000000L | Long.parseLong(color.substring(1), 16));
                }
                JSONObject urls = item.getJSONObject("urls");
                String url = urls.has("4") && !urls.isNull("4") ? urls.getString("4")
                        : urls.has("2") && !urls.isNull("2") ? urls.getString("2") : urls.getString("1");
                Badge badge = new Badge(provider, id, "FFZ " + title(item, "title"),
                        assetUrl(provider, url), background);
                // Deliberately ignore replaces, slot, css and click/navigation metadata.
                addDefinition(definitions, badge);
            }
            JSONObject users = root.getJSONObject("users");
            if (users.length() > MAX_DEFINITIONS) throw invalid();
            java.util.Iterator<String> keys = users.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Badge badge = definitions.get(badgeId(key));
                if (badge == null) throw invalid();
                JSONArray authors = users.getJSONArray(key);
                if ((long) count + authors.length() > MAX_ASSIGNMENTS) throw invalid();
                count += authors.length();
                for (int i = 0; i < authors.length(); i++) {
                    String author = authorId(authors.get(i).toString());
                    if (author == null) throw invalid();
                    addAssignment(assignments, author, badge);
                }
            }
        }
        Map<String, List<Badge>> frozen = new HashMap<>();
        for (Map.Entry<String, Map<String, Badge>> item : assignments.entrySet()) {
            List<Badge> badges = new ArrayList<>(item.getValue().values());
            Collections.sort(badges, new Comparator<Badge>() {
                @Override public int compare(Badge left, Badge right) {
                    int lengths = Integer.compare(left.id.length(), right.id.length());
                    return lengths == 0 ? left.id.compareTo(right.id) : lengths;
                }
            });
            if (badges.size() > MAX_PER_AUTHOR) throw invalid();
            frozen.put(item.getKey(), Collections.unmodifiableList(badges));
        }
        return new Snapshot(provider, definitions, frozen);
    }

    private static void addDefinition(Map<String, Badge> definitions, Badge badge) throws IOException {
        Badge old = definitions.get(badge.id);
        if (old != null && (!old.url.equals(badge.url) || !old.title.equals(badge.title)
                || !java.util.Objects.equals(old.background, badge.background))) throw invalid();
        definitions.put(badge.id, badge);
        if (definitions.size() > MAX_DEFINITIONS) throw invalid();
    }

    private static void addAssignment(Map<String, Map<String, Badge>> assignments,
                                      String author, Badge badge) {
        Map<String, Badge> badges = assignments.get(author);
        if (badges == null) { badges = new TreeMap<>(); assignments.put(author, badges); }
        badges.put(badge.key(), badge);
    }
}
