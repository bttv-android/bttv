"""Generate isolated JVM tests using real preference definitions and presentation policy.

Android UI inflation/navigation still require manual phone testing.
"""
import importlib.util
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('fixtures', Path(__file__).with_name('stage5n_hide_stories_host.py'))
fixtures = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fixtures)
SOURCES = {name:source for name,source in fixtures.SOURCES.items()
           if name.startswith(('android/','org/')) or name in ('bttv/Data.java','bttv/ResUtil.java')}
profiles=[c.get('profile','') for c in json.loads((ROOT/'.all-contributorsrc').read_text())['contributors']]
SOURCES['bttv/settings/SettingsPolishHostTest.java'] = '''package bttv.settings;
import bttv.*;import java.util.*;
public class SettingsPolishHostTest {
 static int checks;
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static void restart()throws Exception {
  for(String name:new String[]{"prefs","editor"}){
   java.lang.reflect.Field f=UserPreferences.class.getDeclaredField(name);f.setAccessible(true);f.set(null,null);
  }
  Data.ctx=new android.content.Context();
 }
 public static void main(String[] args)throws Exception {
  Map<SettingsSections,Integer> groups=new EnumMap<>(SettingsSections.class);
  int bools=0,dropdowns=0,links=0,hidden=0;
  for(Settings setting:Settings.values()){
   SettingsSections section=SettingsSections.sectionOf(setting);
   if(section==null){hidden++;continue;}
   groups.put(section,groups.getOrDefault(section,0)+1);
   UserPreferences.Entry e=setting.entry;
   check(e.primaryTextResource!=null,"visible row has existing label");
   if(e instanceof UserPreferences.Entry.BoolEntry){
    bools++;
    boolean initial=((UserPreferences.Entry.BoolValue)e.defaultValue).get();
    check(((UserPreferences.Entry.BoolEntry)e).get(Data.ctx).get()==initial,"unchanged default "+e.key);
    e.set(Data.ctx,new UserPreferences.Entry.BoolValue(!initial));restart();
    check(((UserPreferences.Entry.BoolEntry)e).get(Data.ctx).get()!=initial,"toggle persists "+e.key);
    e.set(Data.ctx,new UserPreferences.Entry.BoolValue(initial));
   } else if(e instanceof UserPreferences.Entry.DropDownEntry){
    dropdowns++;
    UserPreferences.Entry.DropDownEntry entry=(UserPreferences.Entry.DropDownEntry)e;
    UserPreferences.Entry.DropDownValue value=entry.get(Data.ctx);
    check(value.get().size()==4,"all original GIF modes");
    for(Res.strings mode:value.get()){
     entry.set(Data.ctx,value.newWithSelected(mode));restart();
     check(entry.get(Data.ctx).getSelected()==mode,"GIF choice persists "+mode);
    }
   } else {
    links++;check(e instanceof UserPreferences.Entry.LinkEntry,"original action control");
   }
  }
  check(bools==10&&dropdowns==1&&links==3,"exact original fourteen controls");
  check(hidden==5,"only backing sets/updater/developer options hidden");
  check(groups.get(SettingsSections.EMOTES)==4,"emote controls");
  check(groups.get(SettingsSections.CHAT)==6,"chat controls");
  check(groups.get(SettingsSections.OTHER)==3,"other controls");
  check(groups.get(SettingsSections.ABOUT)==1,"credits control");
  check(SettingsSections.sectionOf(Settings.OpenHighlightedWordsButton)==SettingsSections.CHAT,"keyword editor kept");
  check(SettingsSections.sectionOf(Settings.OpenBlacklistButton)==SettingsSections.CHAT,"blacklist editor kept");
  check(SettingsSections.sectionOf(Settings.OpenCreditsButton)==SettingsSections.ABOUT,"credits kept");
  check(SettingsSections.sectionOf(Settings.AnonChatEnabled)==SettingsSections.CHAT,"Anon control kept");
  Set<String> words=new HashSet<>(Arrays.asList("keyword","<sender>"));
  Settings.HighlightedKeyWords.entry.set(Data.ctx,new UserPreferences.Entry.StringSetValue(words));
  Settings.BlacklistedKeyWords.entry.set(Data.ctx,new UserPreferences.Entry.StringSetValue(Collections.singleton("blocked")));
  restart();
  check(((UserPreferences.Entry.StringSetValue)Settings.HighlightedKeyWords.entry.get(Data.ctx)).get().equals(words),"keyword preference unchanged");
  check(((UserPreferences.Entry.StringSetValue)Settings.BlacklistedKeyWords.entry.get(Data.ctx)).get().contains("blocked"),"blacklist preference unchanged");
  for(String value:new String[]{null,"","javascript:alert(1)","file:///tmp/x","https://user:password@example.com","https://","https://bad space"})
   check(CreditsLinks.destination(value)==null,"unverified destination stays plain text");
  for(String original:PROFILES)check(original.trim().equals(CreditsLinks.destination(original)),"historical destination retained without surrounding whitespace");
  check("https://github.com/FoseFx".equals(CreditsLinks.destination("https://github.com/FoseFx")),"original intro attribution");
  System.out.println("PASS Settings polish JVM: "+checks+" checks");
 }
 static final String[] PROFILES = {PROFILE_VALUES};
}'''.replace('PROFILE_VALUES',','.join(json.dumps(s) for s in profiles))

if __name__ == '__main__':
    output=Path(sys.argv[1]).resolve()
    for name,source in SOURCES.items():
        target=output/name
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_text(source,encoding='utf-8')
    print('Generated Stage 5P-B2 JVM fixtures:',len(SOURCES))
