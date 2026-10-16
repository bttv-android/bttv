"""Generate isolated fixtures for actual ChatTimestamps/Settings/UserPreferences."""
import importlib.util
from pathlib import Path
import sys

spec = importlib.util.spec_from_file_location('fixtures', Path(__file__).with_name('stage5n_hide_stories_host.py'))
fixtures = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fixtures)
SOURCES = dict(fixtures.SOURCES)
SOURCES['bttv/ChatTimestampsHostTest.java'] = '''package bttv;
import bttv.settings.*;
public class ChatTimestampsHostTest {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static void save(boolean on){Settings.AlwaysShowChatTimestamps.entry.set(Data.ctx,new UserPreferences.Entry.BoolValue(on));}
 public static void main(String[] args)throws Exception {
  check(!((UserPreferences.Entry.BoolValue)Settings.AlwaysShowChatTimestamps.entry.defaultValue).get(),"default OFF");
  check(!ChatTimestamps.shouldShow(false),"missing preference preserves hidden stock");
  check(ChatTimestamps.shouldShow(true),"existing native timestamp retained");
  UserPreferences.setBoolean(Data.ctx,"unrelated_setting",true);
  save(true);
  check(ChatTimestamps.shouldShow(false),"ON enables native timestamp flag");
  check(ChatTimestamps.shouldShow(true),"ON preserves already-visible flag");
  for(int i=0;i<100;i++)check(ChatTimestamps.shouldShow(false),"repeated binds stable");
  for(String name:new String[]{"prefs","editor"}){
   java.lang.reflect.Field f=UserPreferences.class.getDeclaredField(name);f.setAccessible(true);f.set(null,null);
  }
  Data.ctx=new android.content.Context();
  check(ChatTimestamps.shouldShow(false),"restart reads persisted ON");
  check(UserPreferences.getBoolean(Data.ctx,"unrelated_setting",false),"other preferences preserved");
  save(false);
  check(!ChatTimestamps.shouldShow(false),"OFF restores hidden stock");
  check(ChatTimestamps.shouldShow(true),"OFF preserves visible stock");
  ResUtil.fail=true;
  check(!ChatTimestamps.shouldShow(false),"storage failure fails open");
  check(ChatTimestamps.shouldShow(true),"storage failure keeps native timestamp");
  System.out.println("PASS ChatTimestamps JVM: "+checks+" checks");
 }
}'''

if __name__ == '__main__':
    output = Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        target = output/name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding='utf-8')
    print('Generated Stage 5O fixtures:', len(SOURCES))
