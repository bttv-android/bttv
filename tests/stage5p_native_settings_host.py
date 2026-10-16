"""Generate isolated JVM fixtures for the actual SettingsNavigation helper."""
from pathlib import Path
import sys

SOURCES = {
'android/content/Context.java': 'package android.content; public class Context {}',
'android/content/ContextWrapper.java': '''package android.content;
public class ContextWrapper extends Context {public Context base; public ContextWrapper(Context b){base=b;}
 public Context getBaseContext(){return base;}}''',
'android/content/Intent.java': '''package android.content;
public class Intent {public final Context source;public final Class<?> target;public int flags;
 public Intent(Context c,Class<?> t){source=c;target=t;}}''',
'android/app/Activity.java': '''package android.app;
public class Activity extends android.content.ContextWrapper {
 public boolean finishing,destroyed,fail; public int starts; public android.content.Intent last;
 public Activity(){super(null);} public boolean isFinishing(){return finishing;}
 public boolean isDestroyed(){return destroyed;} public void startActivity(android.content.Intent i){
  if(fail)throw new IllegalStateException();last=i;starts++;}
}''',
'android/view/Menu.java': '''package android.view;
public interface Menu {int size();MenuItem getItem(int i);MenuItem add(CharSequence title);}''',
'android/view/MenuItem.java': '''package android.view;
public interface MenuItem {int SHOW_AS_ACTION_ALWAYS=2;CharSequence getTitle();void setShowAsAction(int v);
 MenuItem setOnMenuItemClickListener(OnMenuItemClickListener l);
 interface OnMenuItemClickListener {boolean onMenuItemClick(MenuItem i);}}''',
'bttv/settings/SettingsActivity.java': 'package bttv.settings; public class SettingsActivity extends android.app.Activity {}',
'bttv/ResUtil.java': '''package bttv; public class ResUtil {
 public static String getLocaleString(android.content.Context c,Res.strings id){
  if(id!=Res.strings.bttv_settings_entry_title)throw new AssertionError();return "BTTV Settings";}}''',
'bttv/settings/SettingsNavigationHostTest.java': '''package bttv.settings;
import android.app.Activity;import android.content.*;import android.view.*;
import java.lang.ref.*;import java.lang.reflect.*;import java.util.*;
public class SettingsNavigationHostTest {
 static int checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static class Item implements MenuItem {
  CharSequence title;int action;OnMenuItemClickListener listener;Item(CharSequence s){title=s;}
  public CharSequence getTitle(){return title;}public void setShowAsAction(int v){action=v;}
  public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener l){listener=l;return this;}
 }
 static class Items extends ArrayList<Item> implements Menu {
  public MenuItem getItem(int i){return get(i);}public MenuItem add(CharSequence s){Item i=new Item(s);add(i);return i;}
 }
 public static void main(String[] args)throws Exception {
  Activity a=new Activity(); Context app=new Context();ContextWrapper wrapper=new ContextWrapper(new ContextWrapper(a));
  check(SettingsNavigation.findActivity(a)==a,"direct Activity");
  check(SettingsNavigation.findActivity(wrapper)==a,"wrapped Activity");
  check(SettingsNavigation.findActivity(app)==null,"application context rejected");
  check(SettingsNavigation.findActivity(null)==null,"null rejected");
  ContextWrapper cycle=new ContextWrapper(null);cycle.base=cycle;
  check(SettingsNavigation.findActivity(cycle)==null,"self wrapper cycle");
  ContextWrapper other=new ContextWrapper(cycle);cycle.base=other;
  check(SettingsNavigation.findActivity(other)==null,"multi wrapper cycle");
  a.finishing=true;check(!SettingsNavigation.open(a),"finishing rejected");a.finishing=false;
  a.destroyed=true;check(!SettingsNavigation.open(wrapper),"destroyed rejected");a.destroyed=false;
  check(!SettingsNavigation.open(app),"no application task launch");
  check(SettingsNavigation.open(wrapper),"native row launch");
  check(a.starts==1&&a.last.target==SettingsActivity.class,"existing destination");
  check(a.last.source==a&&a.last.flags==0,"same Activity task, no new task flags");
  a.fail=true;check(!SettingsNavigation.open(a),"launch failure safe");a.fail=false;
  check(SettingsNavigation.nativeEntryTitle(a).equals("BTTV Settings"),"localized title");
  Object stock=new Object(),row=new Object(),lookalike=new Object();
  List<Object> rows=new ArrayList<>(Arrays.asList(stock));
  check(!SettingsNavigation.hasNativeRow(rows),"stock list unchanged");
  SettingsNavigation.markNativeRow(row);rows.add(row);
  check(SettingsNavigation.hasNativeRow(rows),"once marker");
  check(SettingsNavigation.isNativeRow(row),"injected click recognized");
  check(!SettingsNavigation.isNativeRow(stock)&&!SettingsNavigation.isNativeRow(lookalike),"stock/lookalike fall through");
  check(!SettingsNavigation.isNativeRow(null),"null row");
  SettingsNavigation.markNativeRow(null);check(SettingsNavigation.hasNativeRow(null),"null list cannot append");
  for(int i=0;i<100;i++)if(!SettingsNavigation.hasNativeRow(rows))rows.add(new Object());
  check(rows.size()==2&&rows.get(0)==stock,"repeat insert leaves stock/order unchanged");
  List<Object> reopened=new ArrayList<>(Arrays.asList(stock));
  check(!SettingsNavigation.hasNativeRow(reopened),"fresh screen can receive row");
  Field f=SettingsNavigation.class.getDeclaredField("nativeRows");f.setAccessible(true);
  Map<?,?> map=(Map<?,?>)f.get(null);check(map instanceof WeakHashMap,"weak model registry");
  check(map.values().stream().allMatch(Boolean.TRUE::equals),"no Activity/presenter values");
  Items menu=new Items();menu.add((CharSequence)"Native action");
  SettingsNavigation.install(a,menu);SettingsNavigation.install(a,menu);
  check(menu.size()==1&&menu.get(0).title.equals("Native action"),"B2 preserves native toolbar without BTTV duplicate");
  check(a.starts==1,"retired toolbar entry cannot launch or change navigation");
  System.out.println("PASS Native Settings JVM: "+checks+" checks");
 }
}''',
}

if __name__ == '__main__':
    output=Path(sys.argv[1]).resolve()
    for name,source in SOURCES.items():
        target=output/name
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_text(source,encoding='utf-8')
    print('Generated Native Settings JVM fixtures:',len(SOURCES))
