"""Generate isolated JVM fixtures exercising the actual quick-settings controller.

Android inflation, rendering, native Apply and navigation require phone acceptance.
"""
import importlib.util
from pathlib import Path
import sys

spec = importlib.util.spec_from_file_location('base', Path(__file__).with_name('stage5n_hide_stories_host.py'))
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
SOURCES = {n:s for n,s in base.SOURCES.items()
           if n.startswith(('android/', 'org/')) or n in ('bttv/Data.java', 'bttv/ResUtil.java')}
SOURCES.update({
'android/content/SharedPreferences.java': '''package android.content;
import java.util.Set;
public interface SharedPreferences {
 boolean getBoolean(String k,boolean d);String getString(String k,String d);
 Set<String> getStringSet(String k,Set<String>d);Editor edit();
 void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l);
 void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l);
 interface OnSharedPreferenceChangeListener {void onSharedPreferenceChanged(SharedPreferences p,String k);}
 interface Editor {Editor putBoolean(String k,boolean v);Editor putString(String k,String v);
 Editor putStringSet(String k,Set<String>v);void apply();}
}''',
'android/content/Context.java': '''package android.content;
import java.util.*;
public class Context {
 public static final Memory memory=new Memory();
 public SharedPreferences getSharedPreferences(String n,int m){if(!n.equals("BTTV"))throw new AssertionError();return memory;}
 public Context getApplicationContext(){return new Context();}
 public String getPackageName(){return "tv.twitch.bttvandroid.app";}
 public static final class Memory implements SharedPreferences,SharedPreferences.Editor {
  public final Map<String,Object> data=new HashMap<>();
  public final Set<OnSharedPreferenceChangeListener> listeners=new HashSet<>();
  public int writes,registers,unregisters;String pending;
  public boolean getBoolean(String k,boolean d){return data.containsKey(k)?(Boolean)data.get(k):d;}
  public String getString(String k,String d){return data.containsKey(k)?(String)data.get(k):d;}
  @SuppressWarnings("unchecked") public Set<String> getStringSet(String k,Set<String>d){return data.containsKey(k)?(Set<String>)data.get(k):d;}
  public Editor edit(){return this;}public Editor putBoolean(String k,boolean v){data.put(k,v);pending=k;return this;}
  public Editor putString(String k,String v){data.put(k,v);pending=k;return this;}
  public Editor putStringSet(String k,Set<String>v){data.put(k,new HashSet<>(v));pending=k;return this;}
  public void apply(){writes++;String key=pending;
   for(OnSharedPreferenceChangeListener l:new ArrayList<>(listeners))l.onSharedPreferenceChanged(this,key);}
  public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){registers++;listeners.add(l);}
  public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){unregisters++;listeners.remove(l);}
 }
}''',
'android/app/Activity.java': '''package android.app;
public class Activity extends android.content.Context {public boolean finishing,destroyed;}''',
'android/content/res/Resources.java': '''package android.content.res;
public class Resources {public int getIdentifier(String n,String t,String p){
 if(!n.equals("bttv_quick_settings")||!t.equals("id")||!p.equals("tv.twitch.bttvandroid.app"))throw new AssertionError();return 123;}}''',
'android/view/View.java': '''package android.view;
import android.content.Context;import java.util.*;
public class View {
 public static final int GONE=8; public int id,visibility;Object tag;Context context;boolean attached;
 public final List<View> children=new ArrayList<>();public final List<OnAttachStateChangeListener> listeners=new ArrayList<>();
 public OnClickListener click;public View(Context c){context=c;}
 public Context getContext(){return context;}public android.content.res.Resources getResources(){return new android.content.res.Resources();}
 public Object getTag(){return tag;}public void setTag(Object o){tag=o;}public void setVisibility(int v){visibility=v;}
 @SuppressWarnings("unchecked") public <T extends View>T findViewById(int i){if(id==i)return (T)this;
  for(View v:children){T r=v.findViewById(i);if(r!=null)return r;}return null;}
 @SuppressWarnings("unchecked") public <T extends View>T findViewWithTag(Object t){if(t.equals(tag))return (T)this;
  for(View v:children){T r=v.findViewWithTag(t);if(r!=null)return r;}return null;}
 public boolean isAttachedToWindow(){return attached;}
 public void addOnAttachStateChangeListener(OnAttachStateChangeListener l){listeners.add(l);}
 public void setOnClickListener(OnClickListener l){click=l;}public void tap(){if(click!=null)click.onClick(this);}
 public void attach(){attached=true;for(OnAttachStateChangeListener l:listeners)l.onViewAttachedToWindow(this);for(View v:children)v.attach();}
 public void detach(){attached=false;for(OnAttachStateChangeListener l:listeners)l.onViewDetachedFromWindow(this);for(View v:children)v.detach();}
 public interface OnClickListener {void onClick(View v);}
 public interface OnAttachStateChangeListener {void onViewAttachedToWindow(View v);void onViewDetachedFromWindow(View v);}
}''',
'android/widget/TextView.java': '''package android.widget;
public class TextView extends android.view.View {public TextView(android.content.Context c){super(c);}public CharSequence getText(){return "native label";}}''',
'android/widget/CompoundButton.java': '''package android.widget;
public class CompoundButton extends TextView {
 boolean checked;OnCheckedChangeListener listener;public CompoundButton(android.content.Context c){super(c);}
 public boolean isChecked(){return checked;}public void setChecked(boolean b){if(b!=checked){checked=b;if(listener!=null)listener.onCheckedChanged(this,b);}}
 public void toggle(){setChecked(!checked);}public void setContentDescription(CharSequence s){}
 public void setOnCheckedChangeListener(OnCheckedChangeListener l){listener=l;}
 public interface OnCheckedChangeListener {void onCheckedChanged(CompoundButton b,boolean c);}
}''',
'androidx/appcompat/widget/SwitchCompat.java': '''package androidx.appcompat.widget;
public class SwitchCompat extends android.widget.CompoundButton {public SwitchCompat(android.content.Context c){super(c);}}''',
'bttv/AnonChat.java': '''package bttv;public class AnonChat {public static int calls;public static boolean last;
 public static void reconnect(boolean b){calls++;last=b;}}''',
'bttv/highlight/Highlight.java': '''package bttv.highlight;public class Highlight {static final Highlight i=new Highlight();public static int opens;
 public static Highlight getInstance(){return i;}public void openDialog(android.app.Activity a){opens++;}}''',
'bttv/highlight/Blacklist.java': '''package bttv.highlight;public class Blacklist {static final Blacklist i=new Blacklist();public static int opens;
 public static Blacklist getInstance(){return i;}public void openDialog(android.app.Activity a){opens++;}}''',
'bttv/settings/SettingsNavigation.java': '''package bttv.settings;public class SettingsNavigation {public static int opens;
 public static android.app.Activity findActivity(android.content.Context c){
  if(!(c instanceof android.app.Activity))return null;android.app.Activity a=(android.app.Activity)c;
  return a.finishing||a.destroyed?null:a;}
 public static boolean open(android.content.Context c){if(findActivity(c)==null)return false;opens++;return true;}}''',
'bttv/settings/StreamQuickSettingsHostTest.java': '''package bttv.settings;
import android.app.Activity;import android.content.Context;import android.view.View;
import android.widget.TextView;import androidx.appcompat.widget.SwitchCompat;
import java.lang.reflect.*;import java.lang.ref.*;
public class StreamQuickSettingsHostTest {
 static int checks;static final Settings[] settings={Settings.SplitChatEnabled,Settings.ShowDeletedMessagesEnabled,Settings.AlwaysShowChatTimestamps,Settings.AnonChatEnabled};
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static View add(View p,View v,String tag){v.setTag(tag);p.children.add(v);return v;}
 static View screen(Activity a){View root=new View(a),section=new View(a);section.id=123;root.children.add(section);
  for(int i=0;i<4;i++){
   View row=add(section,new View(a),"bttv.quick.row."+i);
   add(row,new TextView(a),"bttv.quick.title."+i);add(row,new SwitchCompat(a),"bttv.quick.switch."+i);
  }
  for(String n:new String[]{"highlights","blacklist","settings"})add(section,new View(a),"bttv.quick."+n);
  return root;}
 static SwitchCompat toggle(View v,int i){return v.findViewWithTag("bttv.quick.switch."+i);}
 static void save(int i,boolean b){settings[i].entry.set(new Context(),new UserPreferences.Entry.BoolValue(b));}
 public static void main(String[] args)throws Exception {
  Activity a=new Activity();View root=screen(a),section=root.findViewById(123);Context.Memory p=Context.memory;
  StreamQuickSettings.bind(null);StreamQuickSettings.bind(new View(a));
  StreamQuickSettings.bind(root);Object controller=section.getTag();
  check(controller!=null,"controller installed");check(p.listeners.isEmpty(),"no listener before attachment");
  check(p.writes==0&&bttv.AnonChat.calls==0,"initialization no writes/reconnect");
  for(int i=0;i<4;i++)check(!toggle(root,i).isChecked(),"saved defaults "+i);
  root.attach();check(p.listeners.size()==1&&p.registers==1,"single attach listener");
  for(int n=0;n<100;n++)StreamQuickSettings.bind(root);
  check(section.getTag()==controller&&section.listeners.size()==1&&p.registers==1,"rebind idempotent");
  for(int i=0;i<4;i++){
   int writes=p.writes;root.findViewWithTag("bttv.quick.row."+i).tap();
   check(toggle(root,i).isChecked(),"row updates widget "+i);
   check(((UserPreferences.Entry.BoolEntry)settings[i].entry).get(a).get(),"existing storage "+i);
   check(p.writes==writes+1,"one write no feedback loop "+i);
  }
  check(bttv.AnonChat.calls==1&&bttv.AnonChat.last,"existing anonymous reconnect exactly once");
  for(int i=0;i<4;i++){save(i,false);check(!toggle(root,i).isChecked(),"full Settings external update "+i);}
  check(bttv.AnonChat.calls==1,"preference refresh does not reconnect");
  for(String n:new String[]{"highlights","blacklist","settings"})root.findViewWithTag("bttv.quick."+n).tap();
  check(bttv.highlight.Highlight.opens==1&&bttv.highlight.Blacklist.opens==1&&SettingsNavigation.opens==1,"existing actions");
  a.destroyed=true;for(String n:new String[]{"highlights","blacklist","settings"})root.findViewWithTag("bttv.quick."+n).tap();
  check(bttv.highlight.Highlight.opens==1&&bttv.highlight.Blacklist.opens==1&&SettingsNavigation.opens==1,"destroyed Activity rejected");a.destroyed=false;
  root.detach();check(p.listeners.isEmpty(),"detach unregisters");
  root.findViewWithTag("bttv.quick.settings").tap();check(SettingsNavigation.opens==1,"detached action rejected");
  save(0,true);check(!toggle(root,0).isChecked(),"detached UI not retained by preference refresh");
  root.attach();check(toggle(root,0).isChecked()&&section.getTag()==controller,"reattach saved state same controller");
  check(p.listeners.size()==1,"reattach no duplicate listener");
  root.detach();View reopened=screen(a);StreamQuickSettings.bind(reopened);reopened.attach();
  check(reopened.findViewById(123).getTag()!=controller&&toggle(reopened,0).isChecked(),"reopened view fresh state");
  check(p.listeners.size()==1,"only visible view registered");
  for(Field f:controller.getClass().getDeclaredFields())check(!Activity.class.isAssignableFrom(f.getType())&&!View.class.isAssignableFrom(f.getType()),"no strong Activity/View field "+f);
  Field f=controller.getClass().getDeclaredField("section");f.setAccessible(true);
  check(f.get(controller) instanceof WeakReference,"weak view ownership");reopened.detach();
  check(p.listeners.isEmpty(),"final disposal");
  View invalid=screen(a);invalid.findViewById(123).children.clear();StreamQuickSettings.bind(invalid);
  check(invalid.findViewById(123).visibility==View.GONE,"incomplete own section safely hidden");
  System.out.println("PASS Quick Settings JVM: "+checks+" checks");
 }
}'''
})

if __name__ == '__main__':
    output = Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        target = output/name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding='utf-8')
    print('Generated Quick Settings JVM fixtures:', len(SOURCES))
