package bttv.emote;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class LiveEmotePickerTest {
    private Emote emote(String provider, String code) {
        return new Emote("id-" + code, Emotes.Source.valueOf(provider), code,
                "https://cdn.example.test/" + code, "png", null);
    }
    private LiveEmotePicker.Catalog catalog(String provider, boolean channel, boolean enabled, Emote... emotes) {
        Map<String, Emote> map = new HashMap<>();
        for (Emote emote : emotes) map.put(emote.code, emote);
        return new LiveEmotePicker.Catalog(provider, channel, enabled, map);
    }
    private List<LiveEmotePicker.Entry> enumerate(LiveEmotePicker.Catalog... catalogs) {
        return LiveEmotePicker.enumerate(Arrays.asList(catalogs), true, Collections.emptySet());
    }
    @Test public void empty() { assertTrue(enumerate().isEmpty()); }
    @Test public void allProvidersAndScopes() {
        for (String p : Arrays.asList("FFZ", "BTTV", "STV")) for (boolean channel : new boolean[]{false,true}) {
            List<LiveEmotePicker.Entry> out = enumerate(catalog(p, channel, true, emote(p,"X")));
            assertEquals(1, out.size()); assertEquals(p, out.get(0).provider); assertEquals(channel, out.get(0).channel);
        }
    }
    @Test public void everyPrecedenceLevel() {
        String[] providers = {"FFZ","FFZ","BTTV","BTTV","STV","STV"};
        for (int first = 0; first < 6; first++) {
            List<LiveEmotePicker.Catalog> catalogs = new ArrayList<>();
            for (int i = first; i < 6; i++) catalogs.add(catalog(providers[i], i%2==1, true, emote(providers[i],"Same")));
            List<LiveEmotePicker.Entry> out = LiveEmotePicker.enumerate(catalogs,true,Collections.emptySet());
            assertEquals(1,out.size()); assertEquals(providers[first],out.get(0).provider);
            assertEquals(first%2==1,out.get(0).channel);
        }
    }
    @Test public void disabledProviders() {
        assertEquals("BTTV", enumerate(catalog("FFZ",false,false,emote("FFZ","X")),
                catalog("BTTV",false,true,emote("BTTV","X"))).get(0).provider);
    }
    @Test public void disabledAnimationMatchesRendererEligibility() {
        Emote animated = emote("FFZ","X"); animated.imageType="gif";
        List<LiveEmotePicker.Catalog> catalogs = Arrays.asList(catalog("FFZ",false,true,animated),
                catalog("BTTV",false,true,emote("BTTV","X")));
        assertEquals("BTTV", LiveEmotePicker.enumerate(catalogs,false,Collections.emptySet()).get(0).provider);
        assertTrue(LiveEmotePicker.enumerate(catalogs,true,Collections.emptySet()).get(0).animated);
    }
    @Test public void malformedWinnerDoesNotFallThrough() {
        Emote bad=emote("FFZ","X"); bad.url="http://insecure.test/x";
        assertTrue(enumerate(catalog("FFZ",false,true,bad),catalog("BTTV",true,true,emote("BTTV","X"))).isEmpty());
        bad.url="https://example.test/x"; bad.id="";
        assertTrue(enumerate(catalog("FFZ",false,true,bad),catalog("BTTV",true,true,emote("BTTV","X"))).isEmpty());
    }
    @Test public void unlockedNativeCollisions() {
        assertTrue(LiveEmotePicker.enumerate(Collections.singletonList(catalog("STV",true,true,emote("STV","X"))),
                true,Collections.singleton("X")).isEmpty());
    }
    @Test public void aliasesCaseSortingAndNoPrefixCap() {
        Map<String,Emote> values=new HashMap<>();
        for(int i=0;i<100;i++) values.put("Code"+i,emote("STV","Code"+i));
        Emote alias=emote("STV","Alias"); alias.id="same-id"; values.put(alias.code,alias);
        Emote lower=emote("STV","alias"); lower.id="same-id"; values.put(lower.code,lower);
        List<LiveEmotePicker.Entry> out=enumerate(new LiveEmotePicker.Catalog("STV",true,true,values));
        assertEquals(102,out.size()); assertEquals("Alias",out.get(0).code); assertEquals("alias",out.get(1).code);
        assertEquals(102,enumerate(new LiveEmotePicker.Catalog("STV",true,true,values)).size());
        assertEquals(102,values.size());
    }
    @Test public void displayOrderIndependentOfPrecedence() {
        List<LiveEmotePicker.Catalog> catalogs=new ArrayList<>();
        for(String p:Arrays.asList("FFZ","BTTV","STV")) for(boolean c:new boolean[]{false,true})
            catalogs.add(catalog(p,c,true,emote(p,p+c)));
        List<LiveEmotePicker.Entry> out=LiveEmotePicker.enumerate(catalogs,true,Collections.emptySet());
        for(int i=0;i<6;i++) assertEquals(i,out.get(i).group());
    }
    @Test public void canonicalSyntheticRoundTrip() {
        String value=PickerSyntheticId.encode(5,true,"STV","207813352","abc","OMEGALUL");
        PickerSyntheticId id=PickerSyntheticId.decode(value);
        assertNotNull(id); assertEquals(5,id.session); assertEquals("OMEGALUL",id.code);
        assertNull(PickerSyntheticId.decode(value.replace(":5:",":05:")));
        assertNull(PickerSyntheticId.decode(value+"="));
        assertNull(PickerSyntheticId.decode(value.replace(":C:",":G:")));
        assertNull(PickerSyntheticId.decode("bttv31ac://STV/abc"));
        assertNull(PickerSyntheticId.encode(1,true,"STV","01","id","X"));
        assertNull(PickerSyntheticId.encode(1,false,"STV","0","id","two words"));
        assertNotNull(PickerSyntheticId.decode(PickerSyntheticId.encode(6,false,"FFZ","0","123","X")));
    }
    private void edit(String text,int a,int b,String expected,int cursor) {
        PickerInsertion.Plan p=PickerInsertion.plan(text,a,b,"Kappa"); assertNotNull(p);
        assertEquals(expected,text.substring(0,p.start)+p.replacement+text.substring(p.end));
        assertEquals(cursor,p.cursor);
    }
    @Test public void insertionEmptyAndEnd() { edit("",0,0,"Kappa ",6); edit("hi",2,2,"hi Kappa ",9); }
    @Test public void insertionMiddleAndFollowingSeparator() { edit("a b",1,1,"a Kappa b",8); }
    @Test public void insertionRangesBothDirections() { edit("a xyz b",2,5,"a Kappa b",8); edit("a xyz b",5,2,"a Kappa b",8); }
    @Test public void insertionUnicodeTabsNewlines() {
        edit("\uD83D\uDE00",2,2,"\uD83D\uDE00 Kappa ",9);
        edit("a\t\nb",2,2,"a\tKappa \nb",8);
    }
    @Test public void insertionInvalidSelection() {
        assertNull(PickerInsertion.plan("",-1,0,"Kappa")); assertNull(PickerInsertion.plan("",0,1,"Kappa"));
    }
    @Test public void identityNotEqualityAndDuplicateTombstones() {
        PickerInsertion.Registry<Object> registry=new PickerInsertion.Registry<>(); Object owner=new Object();
        String identity=registry.register("Kappa",owner,"synthetic");
        assertNull(registry.find("Kappa")); assertNull(registry.find(new String(identity)));
        PickerInsertion.Pending<Object> pending=registry.find(identity);
        assertSame(owner,pending.owner.get()); pending.consumed=true;
        assertSame(pending,registry.find(identity)); assertTrue(registry.find(identity).consumed);
        pending.owner.clear(); assertNotNull(registry.find(identity));
    }
    @Test public void publicationBeforeAndDuringRegistrationAndDisposal() {
        EmoteCatalogEvents.published("STV","1"); // snapshot seed must not wait for an event
        int[] count={0};
        EmoteCatalogEvents.Subscription bad=EmoteCatalogEvents.subscribe((p,c,r)-> {throw new RuntimeException();});
        EmoteCatalogEvents.Subscription good=EmoteCatalogEvents.subscribe((p,c,r)->count[0]++);
        EmoteCatalogEvents.published("FFZ",null); assertEquals(1,count[0]);
        good.dispose(); bad.dispose(); EmoteCatalogEvents.published("FFZ",null); assertEquals(1,count[0]);
    }
    @Test public void bindingRejectsSwitchReturnDetachAndRecreation() {
        Object input=new Object(), owner=new Object();
        assertTrue(LiveEmotePicker.sameBinding(input,input,owner,owner,"A","A",1,1,true));
        assertFalse(LiveEmotePicker.sameBinding(input,input,owner,owner,"A","B",1,2,true));
        assertFalse(LiveEmotePicker.sameBinding(input,input,owner,owner,"A","A",1,3,true));
        assertFalse(LiveEmotePicker.sameBinding(input,input,owner,owner,"A","A",1,1,false));
        assertFalse(LiveEmotePicker.sameBinding(input,new Object(),owner,new Object(),"A","A",1,1,true));
        assertFalse(LiveEmotePicker.sameBinding(null,input,owner,owner,"A","A",1,1,true));
    }
}
