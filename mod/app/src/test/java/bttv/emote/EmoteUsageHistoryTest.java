package bttv.emote;

import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

/** Tests the production codec/model/writer, not an independent history implementation. */
public class EmoteUsageHistoryTest {
    private EmoteUsageHistory.Binding binding(String code) {
        return new EmoteUsageHistory.Binding("BTTV","id-"+code,"0",code);
    }
    private Map<EmoteUsageHistory.Binding,Integer> delta(String code,int count) {
        return Collections.singletonMap(binding(code),count);
    }
    @Test public void qualifiedIdentityAndScopes() {
        Set<EmoteUsageHistory.Binding> keys=new HashSet<>();
        for(String p:Arrays.asList("BTTV","FFZ","STV"))for(String c:Arrays.asList("0","100"))
            keys.add(new EmoteUsageHistory.Binding(p,"same",c,"Code"));
        assertEquals(6,keys.size());
        assertEquals(new EmoteUsageHistory.Binding("STV","id","0","X"),new EmoteUsageHistory.Binding("7TV","id","0","X"));
        assertNotEquals(binding("Alias"),binding("alias"));
        assertNotEquals(new EmoteUsageHistory.Binding("STV","id","0","Alias"),new EmoteUsageHistory.Binding("STV","id","0","Other"));
    }
    @Test public void invalidBindings() {
        for(String p:Arrays.asList("",null,"other"))try{new EmoteUsageHistory.Binding(p,"x","0","X");fail();}catch(IllegalArgumentException expected){}
        for(String v:Arrays.asList("",null,"two words","x\n","x\u0000",String.join("",Collections.nCopies(513,"x")))) {
            try{new EmoteUsageHistory.Binding("BTTV",v,"0","X");fail();}catch(IllegalArgumentException expected){}
            try{new EmoteUsageHistory.Binding("BTTV","x","0",v);fail();}catch(IllegalArgumentException expected){}
        }
        for(String c:Arrays.asList(null,"-1","01","channel",""))try{new EmoteUsageHistory.Binding("BTTV","x",c,"X");fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void occurrencesSeparateSendsAndOrdering() {
        EmoteUsageHistory.State s=EmoteUsageHistory.empty(false);
        s=EmoteUsageHistory.add(s,"123",delta("A",3));
        s=EmoteUsageHistory.add(s,"123",delta("B",1));
        s=EmoteUsageHistory.add(s,"123",delta("B",1));
        assertEquals(3,s.list("123").get(0).count);
        s=EmoteUsageHistory.add(s,"123",delta("B",1));
        assertEquals("B",s.list("123").get(0).binding.code);
        assertTrue(s.list("999").isEmpty());assertTrue(s.list(null).isEmpty());
    }
    @Test public void persistenceRoundTripAndPrivacy() throws Exception {
        EmoteUsageHistory.State s=EmoteUsageHistory.add(EmoteUsageHistory.empty(false),"123456789",delta("Code",3));
        String encoded=EmoteUsageHistory.encode(s);
        assertFalse(encoded.contains("123456789"));assertFalse(encoded.contains("https:"));assertFalse(encoded.contains("bttv31pk:"));
        EmoteUsageHistory.State copy=EmoteUsageHistory.decode(encoded);
        assertEquals(encoded,EmoteUsageHistory.encode(copy));assertEquals(3,copy.list("123456789").get(0).count);
        assertEquals(s.partition("123456789"),copy.partition("123456789"));
        assertTrue(copy.list("987654321").isEmpty());
        try{copy.accounts.clear();fail();}catch(UnsupportedOperationException expected){}
        try{copy.list("123456789").clear();fail();}catch(UnsupportedOperationException expected){}
    }
    @Test public void countersSaturateAndSequenceRenormalizes() throws Exception {
        EmoteUsageHistory.State s=EmoteUsageHistory.add(EmoteUsageHistory.empty(false),"123",delta("A",Integer.MAX_VALUE));
        s=EmoteUsageHistory.add(s,"123",delta("A",3));assertEquals(Integer.MAX_VALUE,s.list("123").get(0).count);
        JSONObject j=new JSONObject(EmoteUsageHistory.encode(s));j.put("sequence",Long.MAX_VALUE);
        s=EmoteUsageHistory.add(EmoteUsageHistory.decode(j.toString()),"123",delta("B",1));
        assertTrue(s.sequence>0 && s.sequence<10);assertEquals(Integer.MAX_VALUE,s.list("123").get(0).count);
    }
    @Test public void boundedAccountsAndEviction() throws Exception {
        EmoteUsageHistory.State s=EmoteUsageHistory.empty(false);
        for(int account=1;account<=5;account++)for(int i=0;i<60;i++)
            s=EmoteUsageHistory.add(s,""+account,delta("Code"+i,i+1));
        assertEquals(4,s.accounts.size());assertTrue(s.list("1").isEmpty());
        int total=0;for(List<EmoteUsageHistory.Use> u:s.accounts.values()){assertEquals(50,u.size());total+=u.size();}
        assertEquals(200,total);assertTrue(EmoteUsageHistory.encode(s).getBytes("UTF-8").length<=EmoteUsageHistory.BYTES);
        assertEquals("Code59",s.list("5").get(0).binding.code);
    }
    @Test public void rejectsCorruptRowsAndPreservesFutureSchema() throws Exception {
        EmoteUsageHistory.State good=EmoteUsageHistory.add(EmoteUsageHistory.empty(false),"123",delta("X",1));
        String original=EmoteUsageHistory.encode(good);
        for(String key:Arrays.asList("count","sequence")) {
            JSONObject j=new JSONObject(original);j.getJSONArray("accounts").getJSONObject(0).getJSONArray("uses").getJSONObject(0).put(key,-1);
            assertTrue(EmoteUsageHistory.decode(j.toString()).accounts.isEmpty());
        }
        JSONObject j=new JSONObject(original);JSONArray a=j.getJSONArray("accounts").getJSONObject(0).getJSONArray("uses");a.put(a.getJSONObject(0));
        assertTrue(EmoteUsageHistory.decode(j.toString()).accounts.isEmpty());
        EmoteUsageHistory.State future=EmoteUsageHistory.decode("{\"schema\":2}");assertTrue(future.readOnly);
        assertSame(future,EmoteUsageHistory.add(future,"123",delta("X",1)));
        assertTrue(EmoteUsageHistory.decode(String.join("",Collections.nCopies(EmoteUsageHistory.BYTES+1,"x"))).readOnly);
        assertTrue(EmoteUsageHistory.decode("[[[[[[[[[0]]]]]]]]]").readOnly);
        assertTrue(EmoteUsageHistory.decode("garbage").accounts.isEmpty());
        assertTrue(EmoteUsageHistory.decode(null).accounts.isEmpty());
    }
    @Test public void actualUtf8ByteCapAndOversizedArray() throws Exception {
        EmoteUsageHistory.State s=EmoteUsageHistory.empty(false);
        String wide=String.join("",Collections.nCopies(500,"界"));
        for(int account=1;account<=4;account++) {
            Map<EmoteUsageHistory.Binding,Integer> rows=new LinkedHashMap<>();
            for(int i=0;i<50;i++)rows.put(new EmoteUsageHistory.Binding("STV",wide+i,"0",wide+i),i+1);
            s=EmoteUsageHistory.add(s,""+account,rows);
        }
        String encoded=EmoteUsageHistory.encode(s);
        assertTrue(encoded.getBytes("UTF-8").length<=EmoteUsageHistory.BYTES);
        int total=0;for(List<EmoteUsageHistory.Use> uses:s.accounts.values())total+=uses.size();
        assertTrue(total>0 && total<200);assertEquals(encoded,EmoteUsageHistory.encode(EmoteUsageHistory.decode(encoded)));
        JSONObject j=new JSONObject(encoded);JSONArray accounts=j.getJSONArray("accounts");
        while(accounts.length()<=4)accounts.put(accounts.getJSONObject(0));
        assertTrue(EmoteUsageHistory.decode(j.toString()).accounts.isEmpty());
    }
    private static final class Memory implements EmoteUsageStore.Backend {
        String value;boolean fail;int writes;
        public String read(){return value;}
        public boolean write(String s){writes++;if(fail)return false;value=s;return true;}
    }
    @Test public void actualWriterFailureRestartAndCapturedAccount() {
        Memory memory=new Memory();EmoteUsageStore.Worker w=new EmoteUsageStore.Worker(memory,()->{});w.load();
        memory.fail=true;w.record("111",delta("A",3));assertNull(memory.value);
        assertEquals(3,w.state.list("111").get(0).count);
        memory.fail=false;w.record("222",delta("B",1));
        EmoteUsageStore.Worker restart=new EmoteUsageStore.Worker(memory,()->{});restart.load();
        assertEquals(3,restart.state.list("111").get(0).count);assertEquals("B",restart.state.list("222").get(0).binding.code);
        assertFalse(restart.state.partition("111").equals(restart.state.partition("222")));
        memory.value="{\"schema\":3}";w=new EmoteUsageStore.Worker(memory,()->{});w.load();int before=memory.writes;
        w.record("111",delta("A",1));assertEquals(before,memory.writes);assertEquals("{\"schema\":3}",memory.value);
    }
    @Test public void visibleUsesExactCurrentBindingAndCap() {
        EmoteUsageHistory.State s=EmoteUsageHistory.empty(false);List<LiveEmotePicker.Entry> winners=new ArrayList<>();
        for(int i=0;i<40;i++) {
            String code="C"+i;s=EmoteUsageHistory.add(s,"123",delta(code,1));
            winners.add(new LiveEmotePicker.Entry("BTTV",false,"id-"+code,code,"https://example.test/x",false));
        }
        List<LiveEmotePicker.Entry> out=EmoteUsageHistory.visible(s,"123","100",winners);assertEquals(24,out.size());
        assertTrue(EmoteUsageHistory.visible(s,"999","100",winners).isEmpty());
        LiveEmotePicker.Entry changed=new LiveEmotePicker.Entry("FFZ",false,"id-C39","C39","https://example.test/x",false);
        assertTrue(EmoteUsageHistory.visible(s,"123","100",Collections.singletonList(changed)).isEmpty());
        EmoteUsageHistory.Binding channel=new EmoteUsageHistory.Binding("STV","id","100","Alias");
        s=EmoteUsageHistory.add(s,"123",Collections.singletonMap(channel,5));
        LiveEmotePicker.Entry e=new LiveEmotePicker.Entry("STV",true,"id","Alias","https://example.test/x",false);
        assertEquals(1,EmoteUsageHistory.visible(s,"123","100",Collections.singletonList(e)).size());
        assertTrue(EmoteUsageHistory.visible(s,"123","200",Collections.singletonList(e)).isEmpty());
    }
}
