"""Generate fixtures for the real HideStories/Settings/UserPreferences Java code.

Usage: python tests/stage5n_hide_stories_host.py <ignored-output-directory>
These check preference and rebind behavior, not Android layout rendering.
"""
from pathlib import Path
import sys

SOURCES = {
'android/content/SharedPreferences.java': '''package android.content;
import java.util.Set;
public interface SharedPreferences {
 boolean getBoolean(String k,boolean d); String getString(String k,String d);
 Set<String> getStringSet(String k,Set<String> d); Editor edit();
 interface Editor { Editor putBoolean(String k,boolean v); Editor putString(String k,String v);
  Editor putStringSet(String k,Set<String> v); void apply(); }
}''',
'android/content/Context.java': '''package android.content;
import java.util.*;
public class Context {
 private static final Map<String,Memory> files=new HashMap<>();
 public SharedPreferences getSharedPreferences(String n,int mode) {return files.computeIfAbsent(n,k->new Memory());}
 static final class Memory implements SharedPreferences,SharedPreferences.Editor {
  final Map<String,Object> data=new HashMap<>();
  public boolean getBoolean(String k,boolean d){return data.containsKey(k)?(Boolean)data.get(k):d;}
  public String getString(String k,String d){return data.containsKey(k)?(String)data.get(k):d;}
  @SuppressWarnings("unchecked") public Set<String> getStringSet(String k,Set<String>d){return data.containsKey(k)?(Set<String>)data.get(k):d;}
  public SharedPreferences.Editor edit(){return this;}
  public SharedPreferences.Editor putBoolean(String k,boolean v){data.put(k,v);return this;}
  public SharedPreferences.Editor putString(String k,String v){data.put(k,v);return this;}
  public SharedPreferences.Editor putStringSet(String k,Set<String>v){data.put(k,new HashSet<>(v));return this;}
  public void apply(){}
 }
}''',
'android/util/Log.java': '''package android.util;
public class Log {public static int e(String t,String m){return 0;}
 public static int e(String t,String m,Throwable e){return 0;}}''',
'org/jetbrains/annotations/NotNull.java': 'package org.jetbrains.annotations; public @interface NotNull {}',
'bttv/Data.java': '''package bttv;
public class Data {public static android.content.Context ctx=new android.content.Context();}''',
'bttv/ResUtil.java': '''package bttv;
public class ResUtil {public static boolean fail;
 public static boolean getBooleanFromSettings(bttv.settings.Settings s){
  if(fail)throw new IllegalStateException();
  return ((bttv.settings.UserPreferences.Entry.BoolEntry)s.entry).get(Data.ctx).get();
 }}''',
'bttvcompat/NativeStoriesBridge.java': '''package bttvcompat;
public class NativeStoriesBridge {
 public static final class State {
  public final boolean stories; public final java.util.List<String> otherSections;
  public State(boolean s,java.util.List<String> o){stories=s;otherSections=o;}
 }
 public static final class Home {public boolean attached=true,shown; public int binds; public State last;}
 public static void rebind(Object owner,Object state){
  if(!(owner instanceof Home)||!(state instanceof State)||!((Home)owner).attached)return;
  Home h=(Home)owner; State s=(State)state; h.last=s; h.binds++;
  h.shown=bttv.HideStories.onBind(h,s,s.stories);
 }
}''',
'bttv/HideStoriesHostTest.java': '''package bttv;
import bttv.settings.*;
import bttvcompat.NativeStoriesBridge;
import bttvcompat.NativeStoriesBridge.Home;
import bttvcompat.NativeStoriesBridge.State;
import java.util.*;
public class HideStoriesHostTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static void save(boolean value){Settings.HideTwitchStoriesEnabled.entry.set(Data.ctx,new UserPreferences.Entry.BoolValue(value));}
 static void toggle(boolean value){save(value);HideStories.onSettingChanged();}
 public static void main(String[] args)throws Exception {
  check(!((UserPreferences.Entry.BoolValue)Settings.HideTwitchStoriesEnabled.entry.defaultValue).get(),"default not OFF");
  check(!((UserPreferences.Entry.BoolEntry)Settings.HideTwitchStoriesEnabled.entry).get(Data.ctx).get(),"initial persisted value not OFF");
  List<String> others=Collections.unmodifiableList(Arrays.asList("following","live","recommendations"));
  State original=new State(true,others); Home home=new Home();
  NativeStoriesBridge.rebind(home,original);
  check(home.shown,"OFF removed stock Stories");
  check(home.last==original&&home.last.otherSections==others,"OFF copied/mutated native data");
  UserPreferences.setBoolean(Data.ctx,"unrelated_setting",true);
  toggle(true);
  check(!home.shown,"ON did not suppress presentation flag");
  check(original.stories&&original.otherSections==others,"ON mutated native input");
  check(UserPreferences.getBoolean(Data.ctx,"unrelated_setting",false),"other preference reset");
  Home empty=new Home(); State absent=new State(false,Collections.emptyList());
  NativeStoriesBridge.rebind(empty,absent);
  check(!empty.shown,"empty/unavailable Stories created placeholder");
  check(empty.last.otherSections.isEmpty(),"empty input changed");
  check(!HideStories.onBind(null,null,true),"null inputs broke enabled preference");
  State refreshed=new State(true,Collections.singletonList("new-live-section"));
  for(int i=0;i<100;i++)NativeStoriesBridge.rebind(home,refreshed);
  check(!home.shown&&home.last==refreshed,"feed updates lost setting or latest state");
  int before=home.binds; toggle(false);
  check(home.shown&&home.binds==before+1,"OFF must rebind once, with latest native state");
  check(home.last.otherSections==refreshed.otherSections,"unrelated refreshed sections changed");
  check(!empty.shown,"OFF invented Stories when native had none");
  home.attached=false; before=home.binds; toggle(true);
  check(home.binds==before,"detached view rebound");
  home.attached=true; NativeStoriesBridge.rebind(home,refreshed);
  check(!home.shown,"reattach/return did not read current setting");
  for(String field:new String[]{"prefs","editor"}){
   java.lang.reflect.Field f=UserPreferences.class.getDeclaredField(field);f.setAccessible(true);f.set(null,null);
  }
  Data.ctx=new android.content.Context();
  check(((UserPreferences.Entry.BoolEntry)Settings.HideTwitchStoriesEnabled.entry).get(Data.ctx).get(),"setting did not survive preference reload");
  check(!HideStories.onBind(home,refreshed,true),"relaunch ignored persisted ON");
  check(UserPreferences.getBoolean(Data.ctx,"unrelated_setting",false),"restart reset other preferences");
  ResUtil.fail=true;
  check(HideStories.onBind(home,refreshed,true),"preference failure did not fail open");
  check(!HideStories.onBind(home,absent,false),"preference failure invented Stories");
  ResUtil.fail=false;
  java.lang.reflect.Field f=HideStories.class.getDeclaredField("states");f.setAccessible(true);
  check(f.get(null) instanceof WeakHashMap,"destroyed delegates retained strongly");
  toggle(false); check(!home.shown,"OFF did not preserve latest absent native state");
  NativeStoriesBridge.rebind(home,refreshed);
  check(home.shown,"OFF did not restore subsequently available stock Stories");
  System.out.println("PASS HideStories JVM: "+checks+" checks (persistence, presentation flag, refresh/rebind, immutable inputs, fail-open)");
 }
}''',
}

if __name__ == '__main__':
    output = Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        target = output/name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding='utf-8')
    print('Generated', len(SOURCES), 'Stage 5N JVM fixtures in', output)
