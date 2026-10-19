package bttv.emote;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

/** Bounded local identity/count model. Never stores messages, image URLs or native session IDs. */
public final class EmoteUsageHistory {
    static final int PER_ACCOUNT = 50, ACCOUNTS = 4, BYTES = 256 * 1024, DISPLAY = 24;
    static final class Binding {
        final String provider, id, channel, code;
        Binding(String provider, String id, String channel, String code) {
            if ("7TV".equals(provider)) provider = "STV";
            if (!("FFZ".equals(provider) || "BTTV".equals(provider) || "STV".equals(provider))
                    || !PickerSyntheticId.token(id) || !PickerSyntheticId.token(code)
                    || !("0".equals(channel) || PickerSyntheticId.positive(channel)))
                throw new IllegalArgumentException("Invalid emote binding");
            this.provider = provider; this.id = id; this.channel = channel; this.code = code;
        }
        static Binding of(LiveEmotePicker.Entry e, String channel) {
            return new Binding(e.provider, e.id, e.channel ? channel : "0", e.code);
        }
        String key() { return provider + ":" + id.length() + ":" + id + ":" + channel + ":" + code; }
        boolean matches(LiveEmotePicker.Entry e, String current) {
            return provider.equals(e.provider) && id.equals(e.id) && code.equals(e.code)
                    && channel.equals(e.channel ? current : "0");
        }
        @Override public boolean equals(Object o) { return o instanceof Binding && key().equals(((Binding)o).key()); }
        @Override public int hashCode() { return key().hashCode(); }
    }
    static final class Use {
        final Binding binding; final int count; final long sequence;
        Use(Binding binding, int count, long sequence) { this.binding=binding; this.count=count; this.sequence=sequence; }
    }
    static final Comparator<Use> ORDER = Comparator.comparingInt((Use u)->u.count).reversed()
            .thenComparing(Comparator.comparingLong((Use u)->u.sequence).reversed())
            .thenComparing(u->u.binding.key());
    static final class State {
        final String salt; final long sequence; final boolean readOnly;
        final Map<String,List<Use>> accounts;
        State(String salt, long sequence, boolean readOnly, Map<String,List<Use>> accounts) {
            this.salt=salt; this.sequence=sequence; this.readOnly=readOnly;
            Map<String,List<Use>> copy=new TreeMap<>();
            for (Map.Entry<String,List<Use>> e:accounts.entrySet())
                copy.put(e.getKey(),Collections.unmodifiableList(new ArrayList<>(e.getValue())));
            this.accounts=Collections.unmodifiableMap(copy);
        }
        String partition(String viewer) {
            if (!PickerSyntheticId.positive(viewer) || !hex(salt)) return null;
            try { return digest(salt + ":" + viewer); } catch (Exception ignored) { return null; }
        }
        List<Use> list(String viewer) {
            String key=partition(viewer);
            List<Use> value=key==null ? null : accounts.get(key);
            return value==null ? Collections.emptyList() : value;
        }
    }
    private static String digest(String s) throws Exception {
        byte[] bytes=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder out=new StringBuilder(64);
        for (byte b:bytes) out.append(String.format(Locale.ROOT,"%02x", b & 255));
        return out.toString();
    }
    static State empty(boolean readOnly) {
        byte[] salt=new byte[32]; new SecureRandom().nextBytes(salt);
        StringBuilder s=new StringBuilder(64);
        for (byte b:salt) s.append(String.format(Locale.ROOT,"%02x",b & 255));
        return new State(s.toString(),0,readOnly,Collections.emptyMap());
    }
    private static boolean hex(String s) { return s!=null && s.matches("[0-9a-f]{64}"); }
    private static long number(JSONObject j,String key,long maximum) throws Exception {
        Object n=j.get(key);
        if (!(n instanceof Integer || n instanceof Long)) throw new IllegalArgumentException("Invalid number");
        long v=((Number)n).longValue();
        if (v<0 || v>maximum) throw new IllegalArgumentException("Invalid number");
        return v;
    }
    private static String string(JSONObject j,String key) throws Exception {
        Object s=j.get(key); if (!(s instanceof String)) throw new IllegalArgumentException("Invalid string");
        return (String)s;
    }
    /** Bound before JSONObject recursion/allocation, including unknown future schemas. */
    private static void bounded(String s) {
        if (s.length()>BYTES || s.getBytes(StandardCharsets.UTF_8).length>BYTES)
            throw new IllegalArgumentException("History too large");
        int depth=0; boolean quoted=false,escaped=false;
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if (quoted) {
                if (escaped) escaped=false; else if (c=='\\') escaped=true; else if(c=='"') quoted=false;
            } else if(c=='"') quoted=true;
            else if(c=='{' || c=='[') { if(++depth>8) throw new IllegalArgumentException("History too deep"); }
            else if(c=='}' || c==']') { if(--depth<0) throw new IllegalArgumentException("History malformed"); }
        }
        if (quoted || depth!=0) throw new IllegalArgumentException("History malformed");
    }
    static State decode(String text) {
        if (text==null || text.isEmpty()) return empty(false);
        // A value which cannot safely be parsed is not overwritten by an older reader.
        try { bounded(text); } catch (Throwable invalid) { return empty(true); }
        try {
            JSONObject j=new JSONObject(text);
            if(number(j,"schema",Long.MAX_VALUE)!=1) return empty(true);
            String salt=string(j,"salt"); if(!hex(salt)) throw new IllegalArgumentException("Invalid salt");
            long next=number(j,"sequence",Long.MAX_VALUE);
            JSONArray a=j.getJSONArray("accounts"); if(a.length()>ACCOUNTS) throw new IllegalArgumentException("Accounts cap");
            Map<String,List<Use>> accounts=new TreeMap<>();
            for(int i=0;i<a.length();i++) {
                JSONObject p=a.getJSONObject(i); String key=string(p,"key");
                if(!hex(key) || accounts.containsKey(key)) throw new IllegalArgumentException("Account key");
                JSONArray rows=p.getJSONArray("uses"); if(rows.length()>PER_ACCOUNT) throw new IllegalArgumentException("Rows cap");
                List<Use> uses=new ArrayList<>();Set<Binding> seen=new HashSet<>();
                for(int k=0;k<rows.length();k++) {
                    JSONObject u=rows.getJSONObject(k);
                    Binding b=new Binding(string(u,"provider"),string(u,"id"),string(u,"channel"),string(u,"code"));
                    int count=(int)number(u,"count",Integer.MAX_VALUE); long seq=number(u,"sequence",next);
                    if(count==0 || seq==0 || !seen.add(b)) throw new IllegalArgumentException("Invalid use");
                    uses.add(new Use(b,count,seq));
                }
                uses.sort(ORDER); accounts.put(key,uses);
            }
            return new State(salt,next,false,accounts);
        } catch(Throwable invalid) { return empty(false); }
    }
    static String encode(State state) throws Exception {
        JSONObject j=new JSONObject(); j.put("schema",1);j.put("salt",state.salt);j.put("sequence",state.sequence);
        JSONArray accounts=new JSONArray();
        for(Map.Entry<String,List<Use>> p:state.accounts.entrySet()) {
            JSONArray uses=new JSONArray();
            for(Use u:p.getValue()) {
                JSONObject r=new JSONObject();Binding b=u.binding;
                r.put("provider",b.provider);r.put("id",b.id);r.put("channel",b.channel);r.put("code",b.code);
                r.put("count",u.count);r.put("sequence",u.sequence);uses.put(r);
            }
            JSONObject a=new JSONObject();a.put("key",p.getKey());a.put("uses",uses);accounts.put(a);
        }
        j.put("accounts",accounts);return j.toString();
    }
    static State add(State state,String viewer,Map<Binding,Integer> occurrences) {
        if(state.readOnly || occurrences.isEmpty()) return state;
        String key=state.partition(viewer);if(key==null) return state;
        Map<String,List<Use>> accounts=new TreeMap<>(state.accounts);long next=state.sequence;
        if(next>=Long.MAX_VALUE-1) {
            List<Long> seqs=new ArrayList<>();
            for(List<Use> uses:accounts.values())for(Use u:uses)if(!seqs.contains(u.sequence))seqs.add(u.sequence);
            Collections.sort(seqs);
            for(String account:new ArrayList<>(accounts.keySet())) {
                List<Use> uses=new ArrayList<>();
                for(Use u:accounts.get(account))uses.add(new Use(u.binding,u.count,seqs.indexOf(u.sequence)+1L));
                accounts.put(account,uses);
            }
            next=seqs.size();
        }
        long sequence=next+1;
        Map<Binding,Use> rows=new HashMap<>();
        for(Use u:accounts.getOrDefault(key,Collections.emptyList()))rows.put(u.binding,u);
        for(Map.Entry<Binding,Integer> e:occurrences.entrySet()) {
            if(e.getKey()==null || e.getValue()==null || e.getValue()<=0)continue;
            Use old=rows.get(e.getKey());long count=(old==null?0L:old.count)+(long)e.getValue();
            rows.put(e.getKey(),new Use(e.getKey(),(int)Math.min(Integer.MAX_VALUE,count),sequence));
        }
        List<Use> uses=new ArrayList<>(rows.values());uses.sort(ORDER);
        if(uses.size()>PER_ACCOUNT)uses=new ArrayList<>(uses.subList(0,PER_ACCOUNT));
        accounts.put(key,uses);
        while(accounts.size()>ACCOUNTS) {
            String oldest=null;long oldestSeq=Long.MAX_VALUE;
            for(Map.Entry<String,List<Use>> e:accounts.entrySet()) {
                long last=0;for(Use u:e.getValue())last=Math.max(last,u.sequence);
                if(last<oldestSeq){oldestSeq=last;oldest=e.getKey();}
            }
            accounts.remove(oldest);
        }
        State result=new State(state.salt,sequence,false,accounts);
        try {
            while(encode(result).getBytes(StandardCharsets.UTF_8).length>BYTES) {
                String victim=null;Use lowest=null;
                for(Map.Entry<String,List<Use>> e:accounts.entrySet())for(Use u:e.getValue())
                    if(lowest==null || ORDER.compare(u,lowest)>0){victim=e.getKey();lowest=u;}
                if(victim==null)return state;
                List<Use> smaller=new ArrayList<>(accounts.get(victim));smaller.remove(lowest);accounts.put(victim,smaller);
                result=new State(state.salt,sequence,false,accounts);
            }
            return result;
        } catch(Throwable unavailable) { return state; }
    }
    static List<LiveEmotePicker.Entry> visible(State state,String viewer,String channel,List<LiveEmotePicker.Entry> winners) {
        Map<Binding,LiveEmotePicker.Entry> current=new HashMap<>();
        for(LiveEmotePicker.Entry e:winners)current.put(Binding.of(e,channel),e);
        List<LiveEmotePicker.Entry> result=new ArrayList<>();
        for(Use u:state.list(viewer)) {
            LiveEmotePicker.Entry e=current.get(u.binding);
            if(e!=null)result.add(e);
            if(result.size()==DISPLAY)break;
        }
        return result;
    }
    private EmoteUsageHistory() {}
}
