"""Generate isolated fixtures for REAL production algorithms; no HTTP or device actions.

Compile the generated sources with the production classes listed by the Q-D host
suite, plus EmoteAutocompleteLogic/EmoteAutocompleteCatalog/LiveEmoteChannels.
Only platform, settings and catalog-publication boundaries are substituted.
"""
from pathlib import Path
import runpy
import sys

ROOT = Path(__file__).resolve().parents[1]
SOURCES = dict(runpy.run_path(str(ROOT / 'tests/stage5q_emote_details_host.py'))['SOURCES'])
SOURCES['bttv/emote/EmoteConsistencyHostTest.java'] = r'''package bttv.emote;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;
import java.lang.reflect.*;
import java.util.*;
import org.json.*;

/** Synthetic collisions are algorithmic cases, not reported device collisions. */
public final class EmoteConsistencyHostTest {
    static int checks, pairs;
    static final String CODE="ExampleEmote";
    static final String[] LEVEL={"FFZ-G","FFZ-C","BTTV-G","BTTV-C","STV-G","STV-C"};
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static Map<String,Emote> globals(String p){return p.equals("FFZ")?Emotes.globalEmotesFFZ:p.equals("BTTV")?Emotes.globalEmotesBTTV:Emotes.globalEmotes7TV;}
    static Emote emote(String p,String id,String code){
        String host=p.equals("FFZ")?"cdn.frankerfacez.com":p.equals("BTTV")?"cdn.betterttv.net":"cdn.7tv.app";
        return new Emote(id,Emotes.Source.valueOf(p),code,"https://"+host+"/emote/"+id+"/2x.webp","png",null);
    }
    @SuppressWarnings("unchecked") static Map<String,Object> channels() throws Exception{
        Field f=LiveEmoteChannels.class.getDeclaredField("CATALOGS");f.setAccessible(true);return (Map<String,Object>)f.get(null);
    }
    static void channel(String channel,Emote emote) throws Exception{
        Object cat=channels().get(channel);
        if(cat==null){Constructor<?> c=Class.forName("bttv.emote.LiveEmoteChannels$Catalog").getDeclaredConstructor();c.setAccessible(true);cat=c.newInstance();channels().put(channel,cat);}
        Field f=cat.getClass().getDeclaredField("providers");f.setAccessible(true);
        Object req=((Object[])f.get(cat))[emote.source.ordinal()];
        Field snap=req.getClass().getDeclaredField("snapshot");snap.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String,Emote> old=(Map<String,Emote>)snap.get(req);
        Map<String,Emote> fresh=new HashMap<>(old);fresh.put(emote.code,emote);snap.set(req,Collections.unmodifiableMap(fresh));
    }
    static void reset() throws Exception{
        channels().clear();ResUtil.enabled.clear();ResUtil.enabled.addAll(EnumSet.allOf(Settings.class));ResUtil.disabled=false;
        ResUtil.mode=Res.strings.bttv_settings_gif_render_mode_animate;
        for(String p:new String[]{"FFZ","BTTV","STV"}){globals(p).clear();Emote seed=emote(p,"seed", "Seed"+p);globals(p).put(seed.code,seed);}
    }
    static Emote add(int level,String code) throws Exception{
        String provider=LEVEL[level].split("-")[0];Emote e=emote(provider,"id"+level,code);
        if(level%2==0)globals(provider).put(code,e);else channel("100",e);return e;
    }
    static EmoteDetails chat(String text,String channel){
        List<LiveEmotes.Part> parts=LiveEmotes.split(text,channel);
        if(parts==null)return null;
        for(LiveEmotes.Part part:parts)if(part.marker!=null)return EmoteDetails.create(part.marker,part.text);
        return null;
    }
    static EmoteAutocompleteLogic.Suggestion autocomplete(String code,String channel){
        List<EmoteAutocompleteLogic.Suggestion> items=EmoteAutocompleteLogic.merge(code,Collections.emptyList(),EmoteAutocompleteCatalog.find(channel,code));
        for(EmoteAutocompleteLogic.Suggestion item:items)if(item.code.equals(code))return item;
        return null;
    }
    static LiveEmotePicker.Entry picker(String code,String channel){return LiveEmotePicker.winner(channel,Collections.emptySet(),code);}
    static Object lookup(String channel) throws Exception{
        Constructor<?> c=Class.forName("bttv.emote.LiveEmotes$Lookup").getDeclaredConstructor(String.class);
        c.setAccessible(true);return c.newInstance(channel);
    }
    static Emote find(Object lookup,String code) throws Exception{
        Method m=lookup.getClass().getDeclaredMethod("find",String.class);m.setAccessible(true);return (Emote)m.invoke(lookup,code);
    }
    static String display(String provider){return provider.equals("STV")?"7TV":provider;}
    static void agree(Emote winner,String code,String channel){
        EmoteDetails rendered=chat(code,channel);
        EmoteAutocompleteLogic.Suggestion ac=autocomplete(code,channel);
        LiveEmotePicker.Entry pk=picker(code,channel);
        check(rendered!=null&&ac!=null&&pk!=null,"all three surfaces populated");
        check(rendered.id.equals(winner.id)&&ac.id.equals(winner.id)&&pk.id.equals(winner.id),"same raw ID");
        check(rendered.provider.equals(display(winner.source.name()))&&ac.provider.equals(rendered.provider)&&pk.provider.equals(winner.source.name()),"same provider");
        check(rendered.url.equals(winner.url)&&ac.url.equals(winner.url)&&pk.url.equals(winner.url),"same URL");
        check(rendered.code.equals(code)&&ac.code.equals(code)&&pk.code.equals(code),"exact code/alias");
    }
    static EmoteAutocompleteLogic.Suggestion nat(String code){return EmoteAutocompleteLogic.Suggestion.nativeItem(code,new Object());}
    static List<String> codes(List<EmoteAutocompleteLogic.Suggestion> input){List<String> out=new ArrayList<>();for(EmoteAutocompleteLogic.Suggestion s:input)out.add(s.code);return out;}
    public static void main(String[] args) throws Exception{
        System.out.println("PAIR\tCHAT/AUTOCOMPLETE/PICKER/CARD\tRESULT");
        for(int i=0;i<6;i++)for(int j=i+1;j<6;j++){
            reset();Emote first=add(i,CODE);add(j,CODE);agree(first,CODE,"100");
            EmoteAutocompleteLogic.Suggestion ac=autocomplete(CODE,"100");
            EmoteAutocompleteLogic.Token token=EmoteAutocompleteLogic.token(":"+CODE,CODE.length()+1,CODE.length()+1);
            EmoteAutocompleteLogic.Insertion inserted=EmoteAutocompleteLogic.insertion(":"+CODE,token,ac.code);
            check(inserted.replacement.equals(CODE+" "),"plain code plus space, no outgoing metadata");
            EmoteDetails received=chat(inserted.replacement,"100");
            check(received.id.equals(first.id)&&received.url.equals(first.url)&&received.code.equals(CODE),"received/card bound identity");
            check(EmoteAutocompleteCatalog.find("100",CODE).size()==1,"no duplicate suggestion");
            pairs++;System.out.println(LEVEL[i]+" vs "+LEVEL[j]+"\t"+LEVEL[i]+":"+first.id+"\tPASS");
        }
        check(pairs==15,"all fifteen catalog pairs, including provider interleaving");
        reset();Emote first=add(0,CODE);for(int i=1;i<6;i++)add(i,CODE);agree(first,CODE,"100");
        System.out.println("ALL SIX\tFFZ-G:id0\tPASS");
        reset();Emote g=add(2,CODE);agree(g,CODE,"100");
        Emote c=add(3,CODE);agree(g,CODE,"100"); // Late channel cannot replace earlier global.
        Map<String,Emote> before=LiveEmoteChannels.snapshot("100",Emotes.Source.BTTV);
        Emote b=emote("BTTV","channelB",CODE);channel("200",b);globals("BTTV").remove(CODE);
        agree(c,CODE,"100");agree(b,CODE,"200");agree(c,CODE,"100");
        EmoteDetails bound=chat(CODE,"100");
        Emote later=emote("FFZ","laterGlobal",CODE);globals("FFZ").put(CODE,later);agree(later,CODE,"100");
        check(bound.id.equals(c.id)&&bound.url.equals(c.url),"late global does not rewrite existing card/row");
        channel("100",emote("BTTV","second","OtherCode"));
        check(before.size()==1&&!before.containsKey("OtherCode"),"published channel map is immutable snapshot");
        try{before.put("bad",b);throw new AssertionError("mutable channel snapshot");}catch(UnsupportedOperationException expected){checks++;}
        reset();Emote animated=add(0,CODE);animated.imageType="gif";Emote still=add(3,CODE);ResUtil.disabled=true;
        agree(still,CODE,"100");
        ResUtil.disabled=false;
        for(Res.strings mode:new Res.strings[]{Res.strings.bttv_settings_gif_render_mode_static,Res.strings.bttv_settings_gif_render_mode_animate,Res.strings.bttv_settings_gif_render_mode_animate_forever}){
            ResUtil.mode=mode;agree(animated,CODE,"100");
        }
        reset();for(int i=0;i<6;i++)add(i,CODE);
        ResUtil.enabled.remove(Settings.FFZEmotesEnabled);agree(globals("BTTV").get(CODE),CODE,"100");
        ResUtil.enabled.remove(Settings.BTTVEmotesEnabled);agree(globals("STV").get(CODE),CODE,"100");
        ResUtil.enabled.remove(Settings.SevenTVEmotesEnabled);
        check(chat(CODE,"100")==null&&autocomplete(CODE,"100")==null&&picker(CODE,"100")==null,"all providers disabled");
        reset();channel("100",emote("STV","sameSharedId","AliasOne"));channel("100",emote("STV","sameSharedId","AliasTwo"));
        agree(LiveEmoteChannels.snapshot("100",Emotes.Source.STV).get("AliasOne"),"AliasOne","100");
        agree(LiveEmoteChannels.snapshot("100",Emotes.Source.STV).get("AliasTwo"),"AliasTwo","100");
        check(EmoteAutocompleteCatalog.find("100","ALIAS").size()==2,"same-ID aliases remain separate and query ignores case");
        reset();Emote same=emote("BTTV","sameRawId",CODE);globals("BTTV").put(CODE,same);channel("100",emote("STV","sameRawId",CODE));agree(same,CODE,"100");
        reset();Emote upper=add(0,CODE),lower=emote("FFZ","lowerCase","exampleemote");channel("100",lower);
        agree(upper,CODE,"100");agree(lower,"exampleemote","100");
        check(EmoteAutocompleteCatalog.find("100","EXAMPLE").size()==2,"case-sensitive keys with insensitive prefix");
        EmoteAutocompleteLogic.Suggestion nativeItem=nat(CODE);
        List<EmoteAutocompleteLogic.Suggestion> merged=EmoteAutocompleteLogic.merge(CODE,Collections.singletonList(nativeItem),EmoteAutocompleteCatalog.find("100",CODE));
        check(merged.stream().filter(e->e.code.equals(CODE)).count()==1&&merged.contains(nativeItem),"native exact collision retains exact native object");
        check(LiveEmotePicker.snapshot("100",Collections.singleton(CODE)).stream().noneMatch(e->e.code.equals(CODE)),"native picker code reservation");
        List<EmoteAutocompleteLogic.Suggestion> nativeMatches=Arrays.asList(nat("OM"),nat("ome"),nat("someom"),nat("xxom"));
        List<EmoteAutocompleteLogic.Suggestion> candidates=Arrays.asList(new EmoteAutocompleteLogic.Suggestion("om","FFZ",false),new EmoteAutocompleteLogic.Suggestion("oma","FFZ",false));
        check(codes(EmoteAutocompleteLogic.merge("om",nativeMatches,candidates)).equals(Arrays.asList("OM","om","oma","ome","someom","xxom")),"native/provider exact, prefixes, preserved native substring order");
        candidates=new ArrayList<>();for(int i=0;i<20;i++)candidates.add(new EmoteAutocompleteLogic.Suggestion("om"+i,"BTTV",false));
        check(EmoteAutocompleteLogic.merge("om",Collections.emptyList(),candidates).size()==10,"no premature eight-row cap; maximum ten");
        check(EmoteAutocompleteLogic.match("om",candidates).size()==8,"inactive overload unchanged");
        check(EmoteAutocompleteLogic.match("om",Arrays.asList(null,new EmoteAutocompleteLogic.Suggestion("om bad","FFZ",false))).isEmpty(),"malformed code ignored");
        reset();add(0,CODE).url="http://invalid.example/emote/id0/x";add(3,CODE);
        check(chat(CODE,"100")==null&&picker(CODE,"100")==null&&autocomplete(CODE,"100")!=null,"known malformed-winner eligibility edge unchanged");
        reset();check(chat(CODE,"100")==null&&autocomplete(CODE,"100")==null&&picker(CODE,"100")==null,"no matching catalogs");
        Emote onlyChannel=add(1,CODE);
        for(String unknown:new String[]{null,"-1","not-a-channel","999"})check(chat(CODE,unknown)==null&&autocomplete(CODE,unknown)==null&&picker(CODE,unknown)==null,"unknown channel cannot borrow entries");
        agree(onlyChannel,CODE,"100");
        JSONObject response=new JSONObject("{\"channelEmotes\":[{\"id\":\"own\",\"code\":\"Same\",\"imageType\":\"png\"}],\"sharedEmotes\":[{\"id\":\"shared\",\"code\":\"Same\",\"imageType\":\"png\"}]}");
        check(LiveEmoteChannels.parseBTTV(response).get("Same").id.equals("shared"),"unchanged BTTV shared/owned parse");
        Object input=new Object(),owner=new Object();
        check(LiveEmotePicker.sameBinding(input,input,owner,owner,"100","100",2,2,true),"current binding accepted");
        check(!LiveEmotePicker.sameBinding(input,input,owner,owner,"100","200",2,2,true),"cross-channel binding rejected");
        check(!LiveEmotePicker.sameBinding(input,input,owner,owner,"100","100",2,3,true),"stale generation rejected");
        check(!LiveEmotePicker.sameBinding(input,input,owner,owner,"100","100",2,2,false),"inactive/detached binding rejected");
        reset();Emote originalGlobal=add(0,CODE);Object oldLookup=lookup("100");
        Map<String,Emote> originalMap=Emotes.globalEmotesFFZ;
        Emote replacement=emote("FFZ","publishedGlobal",CODE);
        Emotes.globalEmotesFFZ=new HashMap<>(originalMap);Emotes.globalEmotesFFZ.put(CODE,replacement);
        check(find(oldLookup,CODE)==originalGlobal,"old token lookup retains captured global map");
        agree(replacement,CODE,"100");
        check(originalMap.get(CODE)==originalGlobal,"global publication does not mutate prior snapshot");
        reset();for(String p:new String[]{"FFZ","BTTV","STV"})globals(p).clear();
        check(find(lookup("100"),CODE)==null&&autocomplete(CODE,"100")==null&&picker(CODE,"100")==null,"completely empty catalogs; read-only lookup starts no load");
        check(LiveEmoteGlobals.calls==0&&LiveEmoteGlobals.loads==0,"no HTTP or catalog scheduling");
        System.out.println("PASS "+checks+" real-algorithm synthetic checks; all 15 pairs and all-six agree. Device behavior remains unverified.");
    }
}
'''

if __name__ == '__main__':
    destination = Path(sys.argv[1])
    for name, code in SOURCES.items():
        target = destination / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(code, encoding='utf-8')
    print('wrote', len(SOURCES), 'isolated host fixtures')
