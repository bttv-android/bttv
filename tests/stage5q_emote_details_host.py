"""Isolated JVM tests of real card identity/metadata and accepted GIF policy; no HTTP."""
from pathlib import Path
import runpy
import sys

ROOT = Path(__file__).resolve().parents[1]
SOURCES = dict(runpy.run_path(str(ROOT / 'tests/stage5q_gif_modes_host.py'))['SOURCES'])
SOURCES['bttv/emote/EmoteDetailsHostTest.java'] = '''package bttv.emote;
import bttv.Res;import bttv.ResUtil;import java.util.*;
public final class EmoteDetailsHostTest {
 static int checks;
 static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 static String url(String source,String id){return "https://"+(source.equals("FFZ")?"cdn.frankerfacez.com":source.equals("BTTV")?"cdn.betterttv.net":"cdn.7tv.app")+"/emote/"+id+"/2x.webp";}
 static String marker(String source,String id){return "bttv31://"+source+":"+id+":"+url(source,id);}
 static Emote entry(String source,String id,String code,String url,String owner){return new Emote(id,Emotes.Source.valueOf(source),code,url,"gif",owner);}
 static Map<String,Emote> map(String s){return s.equals("FFZ")?Emotes.globalEmotesFFZ:s.equals("BTTV")?Emotes.globalEmotesBTTV:Emotes.globalEmotes7TV;}
 public static void main(String[] args){
  for(String nativeId:new String[]{null,"", "25","emotesv2_a0b0d83986b2434a87f5dfb28a32bb02","BTTV-old","unrelated","bttv31pk:v1:1:G:BTTV:0:abc:def","bttv31ac://BTTV/id"}){
   check(!EmoteDetails.isSynthetic(nativeId),"native/unrelated passthrough");
   check(EmoteDetails.create(nativeId,"Kappa")==null,"no third-party data for other schemes");
  }
  for(String source:new String[]{"BTTV","FFZ","STV"}){
   String id="same_id", code="ExactAlias";
   EmoteDetails d=EmoteDetails.create(marker(source,id),code);
   check(d!=null&&d.code.equals(code),"bound alias retained");
   check(d.id.equals(id)&&d.url.equals(url(source,id)),"bound identity retained");
   check(d.provider.equals(source.equals("STV")?"7TV":source),"provider display");
   check(d.owner==null&&d.animated==null,"missing catalog metadata optional");
   Emote e=entry(source,id,code,url(source,id),"ProviderOwner");
   map(source).put(code,e);
   d=EmoteDetails.create(marker(source,id),code);
   check(d.url.equals(url(source,id)),"cached URL not substituted");
   check(Boolean.TRUE.equals(d.animated),"actual cached animation flag");
   check(source.equals("BTTV")?d.owner==null:"ProviderOwner".equals(d.owner),"neutral verified owner, no BTTV invention");
   check(!d.description().contains("Artist")&&!d.description().contains("Subscription")&&!d.description().contains("channel"),"no invented artist/subscription/channel");
   check(d.description().startsWith(d.provider),"provider always visible");
   e.owner="mutated";e.imageType="png";
   check(source.equals("BTTV")?d.owner==null:"ProviderOwner".equals(d.owner),"immutable attribution snapshot");
   check(Boolean.TRUE.equals(d.animated),"immutable animation snapshot");
   check(Boolean.FALSE.equals(EmoteDetails.create(marker(source,id),code).animated),"known static metadata");
   e.imageType="webp";e.owner="  ";
   check(EmoteDetails.create(marker(source,id),code).animated==null,"unknown animated state omitted");
   check(EmoteDetails.create(marker(source,id),code).owner==null,"empty owner omitted");
   EmoteDetails alias=EmoteDetails.create(marker(source,id),"AnotherAlias");
   check(alias.code.equals("AnotherAlias")&&alias.owner==null,"ID-only alias lookup forbidden");
   EmoteDetails wrong=EmoteDetails.withCached(EmoteDetails.parse(marker(source,id),code),entry(source,"other",code,url(source,id),"wrong"));
   check(wrong.owner==null,"ID mismatch cannot borrow metadata");
   wrong=EmoteDetails.withCached(EmoteDetails.parse(marker(source,id),code),entry(source,id,code,url(source,"other"),"wrong"));
   check(wrong.owner==null,"URL mismatch cannot borrow metadata");
   String other=source.equals("FFZ")?"BTTV":"FFZ";
   wrong=EmoteDetails.withCached(EmoteDetails.parse(marker(source,id),code),entry(other,id,code,url(source,id),"wrong"));
   check(wrong.owner==null,"provider/raw-ID collision isolated");
   map(source).put(code,entry(source,"new_channel_id",code,url(source,"new_channel_id"),"wrong"));
   EmoteDetails old=EmoteDetails.create(marker(source,id),code);
   check(old.id.equals(id)&&old.url.equals(url(source,id))&&old.owner==null,"channel switch cannot rewrite old row");
   map(source).clear();
   check(EmoteDetails.create(marker(source,id),code).url.equals(url(source,id)),"removed catalog retains minimum card");
  }
  EmoteDetails a=EmoteDetails.create(marker("STV","same_id"),"AliasA"), b=EmoteDetails.create(marker("STV","same_id"),"AliasB");
  check(a.code.equals("AliasA")&&b.code.equals("AliasB"),"same-ID aliases independent");
  check(!EmoteDetails.create(marker("BTTV","same_id"),"SameCode").provider.equals(EmoteDetails.create(marker("STV","same_id"),"SameCode").provider),"same-name provider collision independent");
  for(String bad:new String[]{"bttv31://","bttv31://BTTV","bttv31://UNKNOWN:id:https://cdn.7tv.app/emote/id/2x.webp","bttv31://BTTV::https://cdn.betterttv.net/emote/id/2x.webp","bttv31://BTTV:bad id:https://cdn.betterttv.net/emote/id/2x.webp","bttv31://BTTV:id"}){
   check(EmoteDetails.isSynthetic(bad),"malformed reserved scheme intercepted");
   EmoteDetails d=EmoteDetails.create(bad,"BoundCode");
   check(d!=null&&d.code.equals("BoundCode")&&d.url==null,"safe local unavailable, not native routing");
   check(d.description().contains("Preview unavailable"),"missing preview textual fallback");
  }
  for(String bad:new String[]{"file:///emote/id/x","content://x/emote/id/x","javascript:bad","data:image/gif,x","http://cdn.betterttv.net/emote/id/x","https://evil.example/emote/id/x","https://cdn.betterttv.net.evil.example/emote/id/x","https://cdn.betterttv.net@evil.example/emote/id/x","https://evil@cdn.betterttv.net/emote/id/x","https://cdn.7tv.app/emote/id/x","https://cdn.betterttv.net:443/emote/id/x","https://cdn.betterttv.net/emote/other/x","https://cdn.betterttv.net/emote/id/../x","https://cdn.betterttv.net/emote/id/%2e%2e/x","https://cdn.betterttv.net/emote/id/x?next=evil","https://cdn.betterttv.net/emote/id/x#fragment","https://cdn.betterttv.net/emote/id/"}){
   EmoteDetails d=EmoteDetails.create("bttv31://BTTV:id:"+bad,"Name");
   check(d.url==null&&d.provider.equals("BTTV")&&d.code.equals("Name"),"untrusted image omitted, text works");
  }
  check(EmoteDetails.create(marker("BTTV","id"),null).code.equals("Emote unavailable"),"null code fallback");
  check(EmoteDetails.create(marker("BTTV","id"),"").code.equals("Emote unavailable"),"empty code fallback");
  String longCode=String.join("",Collections.nCopies(100,"LongAlias"));
  check(EmoteDetails.create(marker("STV","id"),longCode).code.equals(longCode),"long exact code not truncated");
  String longMarker="bttv31://BTTV:id:"+String.join("",Collections.nCopies(8200,"x"));
  check(EmoteDetails.create(longMarker,"Name").url==null,"oversized input safe");
  Res.strings[] modes={Res.strings.bttv_settings_gif_render_mode_animate_forever,Res.strings.bttv_settings_gif_render_mode_animate,Res.strings.bttv_settings_gif_render_mode_static,Res.strings.bttv_settings_gif_render_mode_disabled};
  boolean[][] policy={{true,true},{true,false},{false,false},{false,false}};
  for(int i=0;i<modes.length;i++){ResUtil.mode=modes[i];ResUtil.disabled=false;for(int n=0;n<2;n++)check(GifPolicy.shouldAnimate(n==1)==policy[i][n],"accepted preview decode policy");}
  check(LiveEmoteGlobals.calls==0&&LiveEmoteGlobals.loads==0,"no catalog HTTP/refresh");
  check(android.util.Log.lines.isEmpty(),"no diagnostic logging");
  System.out.println("PASS Stage 5Q-D "+checks+" JVM identity/security/metadata/policy checks; Android UI remains device-unverified");
 }
}'''

if __name__ == '__main__':
    destination = Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        path = destination / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(source + '\n', encoding='utf-8', newline='\n')
    print('Generated Stage 5Q-D JVM fixtures:', len(SOURCES))
