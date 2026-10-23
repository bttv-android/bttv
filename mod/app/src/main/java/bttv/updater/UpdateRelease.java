package bttv.updater;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Conservative official-release contract. Unknown compatibility is never an install offer. */
public final class UpdateRelease {
    enum Outcome { AVAILABLE, NOT_NEWER, UNSUPPORTED, MISSING_ASSET, INVALID_METADATA,
        NETWORK, RATE_LIMIT, SERVER, HTTP_ERROR, CANCELLED, DISABLED, INTERNAL_ERROR }
    static final class Problem extends IOException {
        final Outcome outcome;
        Problem(String message) { this(message,Outcome.INVALID_METADATA); }
        Problem(String message,Outcome outcome) { super(message); this.outcome=outcome; }
    }
    public static final String PACKAGE = "tv.twitch.bttvandroid.app";
    public static final String API = "https://api.github.com/repos/bttv-android/bttv/releases/latest";
    public static final String MIME = "application/vnd.android.package-archive";
    public static final int MAX_METADATA = 262144, MAX_NOTES = 24576;
    public static final long MAX_APK = 536870912L;
    private static final String MARKER = "```bttv-update\n";

    public static final class Installed {
        public final String name, signer;
        public final long code;
        public final int sdk;
        public final List<String> abis;
        public Installed(String name, long code, String signer, int sdk, List<String> abis) {
            this.name = name; this.code = code; this.signer = signer;
            this.sdk = sdk; this.abis = Collections.unmodifiableList(new ArrayList<>(abis));
        }
    }

    public static final class Inspection {
        public final UpdateRelease candidate;
        public final String message;
        final Outcome outcome;
        Inspection(UpdateRelease candidate, String message, Outcome outcome) {
            this.candidate = candidate; this.message = message; this.outcome=outcome;
        }
    }

    public final String tag, name, notes, url, assetName, sha256, signer, twitchVersion;
    public final long code, size, assetId;
    public final int minSdk;
    public final List<String> abis;

    private UpdateRelease(String tag, String name, String notes, String url, String assetName,
                          String sha256, String signer, String twitchVersion, long code,
                          long size, long assetId, int minSdk, List<String> abis) {
        this.tag=tag; this.name=name; this.notes=notes; this.url=url; this.assetName=assetName;
        this.sha256=sha256; this.signer=signer; this.twitchVersion=twitchVersion;
        this.code=code; this.size=size; this.assetId=assetId; this.minSdk=minSdk;
        this.abis=Collections.unmodifiableList(new ArrayList<>(abis));
    }

    public static Inspection inspect(String text, Installed installed) throws IOException {
        try {
            boundedJson(text);
            JSONObject json = new JSONObject(text);
            String tag = string(json, "tag_name", 100);
            String releaseName=json.isNull("name")?tag:json.optString("name",tag);
            require(releaseName.length()<=200,"Release name is too large");
            require(tag.matches("v[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.-]+)?"), "Invalid release tag");
            require(json.get("draft") instanceof Boolean && json.get("prerelease") instanceof Boolean,
                    "Missing release status");
            if (json.getBoolean("draft") || json.getBoolean("prerelease"))
                return new Inspection(null, "No compatible update: this channel excludes drafts and prereleases.",Outcome.UNSUPPORTED);
            String notes = json.isNull("body") ? "" : json.optString("body", "");
            require(notes.length() <= MAX_NOTES, "Release notes are too large");
            JSONArray assets = json.getJSONArray("assets");
            require(assets.length() <= 64, "Too many release assets");
            JSONObject apk = null;
            for (int i=0; i<assets.length(); i++) {
                JSONObject item=assets.getJSONObject(i);
                if (!MIME.equals(item.optString("content_type"))) continue;
                require(apk==null, "Ambiguous APK assets");
                require("uploaded".equals(item.optString("state")), "APK is not uploaded");
                apk=item;
            }
            if(apk==null)throw new Problem("No uploaded APK asset",Outcome.MISSING_ASSET);
            String assetName=string(apk,"name",180);
            require(assetName.matches("[A-Za-z0-9][A-Za-z0-9._-]*\\.apk"), "Invalid APK asset name");
            long id=number(apk,"id",Long.MAX_VALUE), size=number(apk,"size",MAX_APK);
            String url=string(apk,"browser_download_url",1024);
            require(assetUrl(url,tag,assetName), "Unexpected APK origin");
            String digest=apk.isNull("digest") ? "" : apk.optString("digest", "");
            if (!digest.isEmpty()) require(digest.matches("sha256:[0-9a-fA-F]{64}"), "Invalid asset digest");

            // Explicit, visible release-note metadata; no hidden test mode or arbitrary sidecar URL.
            int start=notes.indexOf(MARKER);
            if (start<0) return new Inspection(null,
                    "No compatible update: the official release does not provide sufficient compatibility metadata.",Outcome.UNSUPPORTED);
            require((start==0 || notes.charAt(start-1)=='\n') && notes.indexOf(MARKER,start+1)<0,
                    "Ambiguous compatibility metadata");
            int end=notes.indexOf("\n```",start+MARKER.length());
            require(end>=0, "Incomplete compatibility metadata");
            String compatibility=notes.substring(start+MARKER.length(),end);
            boundedJson(compatibility);
            JSONObject c=new JSONObject(compatibility);
            compatible(number(c,"schemaVersion",1)==1 && c.getBoolean("standalone"), "Unsupported compatibility schema");
            compatible(PACKAGE.equals(string(c,"packageName",100)), "Wrong package");
            String name=string(c,"versionName",96);
            require(tag.equals("v"+name) && tag.equals(string(c,"releaseTag",100)), "Version/tag mismatch");
            long code=number(c,"versionCode",Integer.MAX_VALUE);
            if (code<=installed.code) return new Inspection(null, "No compatible update: the release is not newer than this installation.",Outcome.NOT_NEWER);
            String signer=hash(string(c,"signerSha256",64));
            compatible(signer.equals(hash(installed.signer)), "The release signing identity is incompatible with this installation");
            int min=(int)number(c,"minSdk",1000);
            compatible(min<=installed.sdk, "Android version is unsupported");
            String twitch=string(c,"twitchVersion",32);
            compatible(twitch.matches("31\\.[0-9]+\\.[0-9]+"), "Unsupported Twitch base");
            JSONArray abiJson=c.getJSONArray("supportedAbis");
            compatible(abiJson.length()==2, "A complete dual-ARM release is required");
            List<String> abis=new ArrayList<>();
            for(int i=0;i<abiJson.length();i++) {
                String abi=abiJson.getString(i);
                require((abi.equals("arm64-v8a") || abi.equals("armeabi-v7a")) && !abis.contains(abi), "Invalid ABI metadata");
                abis.add(abi);
            }
            compatible(!Collections.disjoint(abis,installed.abis), "Device ABI is unsupported");
            require(id==number(c,"assetId",Long.MAX_VALUE) && assetName.equals(string(c,"assetName",180))
                    && size==number(c,"sizeBytes",MAX_APK), "Asset metadata mismatch");
            String sha=hash(string(c,"sha256",64));
            require(digest.isEmpty() || digest.substring(7).equalsIgnoreCase(sha), "Conflicting APK hashes");
            String display=notes.substring(0,start)+notes.substring(end+4);
            UpdateRelease candidate=new UpdateRelease(tag,name,display.trim(),url,assetName,sha,signer,twitch,
                    code,size,id,min,abis);
            return new Inspection(candidate,"Compatible update available.",Outcome.AVAILABLE);
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new Problem("Invalid official release metadata"); }
    }

    static void boundedJson(String text) throws IOException {
        require(text!=null && !text.trim().isEmpty() && text.length()<=MAX_METADATA, "Empty or oversized metadata");
        int depth=0; boolean quoted=false, escaped=false;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(quoted) { if(escaped) escaped=false; else if(c=='\\') escaped=true; else if(c=='"') quoted=false; }
            else if(c=='"') quoted=true;
            else if(c=='{' || c=='[') require(++depth<=32,"Metadata nesting is too deep");
            else if(c=='}' || c==']') require(--depth>=0,"Invalid metadata nesting");
        }
        require(depth==0 && !quoted,"Incomplete metadata");
    }

    static String string(JSONObject j,String key,int max) throws Exception {
        Object o=j.get(key);
        require(o instanceof String,"Invalid "+key);
        String s=(String)o;
        require(!s.isEmpty() && s.length()<=max && !s.matches("(?s).*[\\x00-\\x1f\\x7f].*"),"Invalid "+key);
        return s;
    }
    static long number(JSONObject j,String key,long max) throws Exception {
        Object o=j.get(key);
        require(o instanceof Integer || o instanceof Long,"Invalid "+key);
        long v=((Number)o).longValue(); require(v>0 && v<=max,"Invalid "+key); return v;
    }
    static String hash(String s) throws IOException {
        require(s!=null && s.matches("[0-9a-fA-F]{64}"),"Missing or invalid SHA-256 identity");
        return s.toUpperCase(java.util.Locale.ROOT);
    }
    static boolean assetUrl(String value,String tag,String file) {
        try {
            URI u=new URI(value);
            return "https".equals(u.getScheme()) && "github.com".equals(u.getHost()) && u.getPort()==-1
                    && u.getUserInfo()==null && u.getQuery()==null && u.getFragment()==null
                    && u.getRawPath().equals("/bttv-android/bttv/releases/download/"+tag+"/"+file);
        } catch(Exception e) { return false; }
    }
    static void require(boolean ok,String reason) throws IOException { if(!ok) throw new Problem(reason); }
    private static void compatible(boolean ok,String reason) throws IOException {
        if(!ok)throw new Problem(reason,Outcome.UNSUPPORTED);
    }
}
