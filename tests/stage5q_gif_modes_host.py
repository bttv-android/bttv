"""Generate isolated JVM fixtures for real policy/catalog code; never uses HTTP."""
from pathlib import Path
import runpy
import sys

ROOT = Path(__file__).resolve().parents[1]
SOURCES = dict(runpy.run_path(str(ROOT / 'tests/stage5q_trace_cleanup_host.py'))['SOURCES'])
SOURCES['bttv/ResUtil.java'] = '''package bttv;
public final class ResUtil {
 public static final java.util.Set<bttv.settings.Settings> enabled=java.util.EnumSet.allOf(bttv.settings.Settings.class);
 public static boolean disabled, unavailable;
 public static Res.strings mode=Res.strings.bttv_settings_gif_render_mode_animate;
 public static boolean getBooleanFromSettings(bttv.settings.Settings s){return enabled.contains(s);}
 public static Res.strings getSelectedDropdownFromSettings(bttv.settings.Settings s){
  if(unavailable)throw new IllegalStateException();
  return disabled?Res.strings.bttv_settings_gif_render_mode_disabled:mode;
 }
 public static boolean selectedDropdownFromSettingsIs(bttv.settings.Settings s,Res.strings r){return getSelectedDropdownFromSettings(s)==r;}
 public static int getResourceId(Res.strings r){return r.ordinal()+1;}
 public static int getResourceId(android.content.Context c,Res.ids r){return r.ordinal()+1;}
}'''
SOURCES['bttv/emote/GifModesHostTest.java'] = '''package bttv.emote;
import bttv.Res;import bttv.ResUtil;import java.util.*;
public final class GifModesHostTest {
 static int checks;
 static void check(boolean v,String label){checks++;if(!v)throw new AssertionError(label);}
 static Map<String,Emote> map(Emotes.Source s){return s==Emotes.Source.FFZ?Emotes.globalEmotesFFZ:s==Emotes.Source.BTTV?Emotes.globalEmotesBTTV:Emotes.globalEmotes7TV;}
 public static void main(String[] args) {
  Res.strings[] modes={Res.strings.bttv_settings_gif_render_mode_animate_forever,Res.strings.bttv_settings_gif_render_mode_animate,Res.strings.bttv_settings_gif_render_mode_static,Res.strings.bttv_settings_gif_render_mode_disabled};
  boolean[][] expected={{true,true},{true,false},{false,false},{false,false}};
  for(Emotes.Source s:Emotes.Source.values()){
   map(s).clear();
   String root=s==Emotes.Source.BTTV?"https://cdn.betterttv.net/emote/":s==Emotes.Source.FFZ?"https://cdn.frankerfacez.com/emote/":"https://cdn.7tv.app/emote/";
   map(s).put(s+"Still",new Emote(s+"still",s,s+"Still",root+s+"/2x.png","png",null));
   map(s).put(s+"Motion",new Emote(s+"motion",s,s+"Motion",root+s+"/2x.webp","gif",null));
  }
  ResUtil.disabled=false;
  for(int i=0;i<modes.length;i++){
   ResUtil.mode=modes[i];
   for(int request=0;request<2;request++)check(GifPolicy.shouldAnimate(request==1)==expected[i][request],"mode/native static matrix "+i+"/"+request);
   for(Emotes.Source s:Emotes.Source.values()){
    for(String suffix:new String[]{"Still","Motion"}){
     String code=s+suffix;boolean eligible=i!=3||suffix.equals("Still");
     List<LiveEmotes.Part> parts=LiveEmotes.split("before "+code+" after","-1");
     check((parts!=null)==eligible,"chat eligibility "+code+" mode "+i);
     if(eligible){
      check(parts.size()==3&&parts.get(0).text.equals("before ")&&parts.get(2).text.equals(" after"),"text/offsets preserved");
      check(parts.get(1).text.equals(code),"code unchanged");
      check(LiveEmotes.urlForMarker(parts.get(1).marker,code).equals(map(s).get(code).url),"provider URL unchanged in every mode");
     }
     List<EmoteAutocompleteLogic.Suggestion> previews=EmoteAutocompleteCatalog.find("-1",code);
     check((previews.size()==1)==eligible,"autocomplete eligibility "+code);
     if(eligible)check(previews.get(0).url.equals(map(s).get(code).url),"preview source preserved");
    }
   }
   List<LiveEmotePicker.Entry> picker=LiveEmotePicker.snapshot("-1",Collections.emptySet());
   check(picker.size()==(i==3?3:6),"picker eligibility");
   for(LiveEmotePicker.Entry item:picker)check(item.url.equals(map(Emotes.Source.valueOf(item.provider)).get(item.code).url),"picker provider source preserved");
   check(LiveEmotePicker.snapshot("-1",Collections.singleton("BTTVStill")).size()==picker.size()-1,"native collision precedence unchanged");
  }
  check(LiveEmotes.urlForMarker("emotesv2_native","Kappa")==null,"native Twitch unaffected");
  check(LiveEmotes.split("ordinary unmatched text","-1")==null,"ordinary chat untouched");
  check(LiveEmoteGlobals.calls==0&&LiveEmoteGlobals.loads==0,"no HTTP added by policy or enumeration");
  for(int cycle=0;cycle<100;cycle++){
   ResUtil.mode=modes[cycle%4];
   check(GifPolicy.shouldAnimate(false)==expected[cycle%4][0],"new binding reads current preference");
  }
  ResUtil.unavailable=true;check(!GifPolicy.shouldAnimate(false),"unavailable preference cannot start animation");ResUtil.unavailable=false;
  check(android.util.Log.lines.isEmpty(),"no production traces");
  System.out.println("PASS Stage 5Q-C "+checks+" JVM policy/provider/chat/picker/autocomplete checks (no real HTTP)");
 }
}'''

if __name__ == '__main__':
    destination = Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        target = destination / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source + '\n', encoding='utf-8', newline='\n')
    print('Generated Stage 5Q-C JVM fixtures:', len(SOURCES))
