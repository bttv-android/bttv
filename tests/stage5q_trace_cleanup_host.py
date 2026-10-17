"""Generate isolated JVM checks for the real cleaned emote and keyword-editor code.

Run: python tests/stage5q_trace_cleanup_host.py <ignored-fixture-directory>
Uses the existing picker host fixtures and a local org.json JAR; no provider HTTP.
"""
from pathlib import Path
import runpy
import sys

ROOT = Path(__file__).resolve().parents[1]
SOURCES = dict(runpy.run_path(str(ROOT / 'tests/stage5e_host.py'))['SOURCES'])
SOURCES.pop('bttv/emote/PickerCoordinatorHostTest.java')
SOURCES.pop('bttv/emote/LiveEmoteChannels.java')
SOURCES.update({
    'android/content/Context.java': 'package android.content; public class Context {}',
    'androidx/annotation/NonNull.java': 'package androidx.annotation; public @interface NonNull {}',
    'androidx/annotation/Nullable.java': 'package androidx.annotation; public @interface Nullable {}',
    'android/view/View.java': '''package android.view;
public class View {
 public interface OnAttachStateChangeListener {void onViewAttachedToWindow(View v);void onViewDetachedFromWindow(View v);}
 public interface OnClickListener {void onClick(View v);}
 public boolean attached=true; final java.util.List<OnAttachStateChangeListener> listeners=new java.util.ArrayList<>();
 public final java.util.Map<Integer,View> children=new java.util.HashMap<>();private OnClickListener click;
 public void addOnAttachStateChangeListener(OnAttachStateChangeListener l){listeners.add(l);}
 public boolean isAttachedToWindow(){return attached;} public boolean hasFocus(){return true;}
 public void detach(){attached=false;for(OnAttachStateChangeListener l:listeners)l.onViewDetachedFromWindow(this);}
 @SuppressWarnings("unchecked") public <T extends View>T findViewById(int id){return (T)children.get(id);}
 public void setOnClickListener(OnClickListener c){click=c;}public void click(){click.onClick(this);}
}''',
    'android/view/ViewGroup.java': 'package android.view; public class ViewGroup extends View {}',
    'android/view/LayoutInflater.java': '''package android.view;
public class LayoutInflater {public static LayoutInflater from(android.content.Context c){return new LayoutInflater();}
 public View inflate(int id,ViewGroup parent,boolean attach){return parent;}}''',
    'android/widget/TextView.java': 'package android.widget; public class TextView extends android.view.View {public void setText(CharSequence text){}}',
    'android/widget/ImageButton.java': 'package android.widget; public class ImageButton extends android.view.View {}',
    'android/widget/ArrayAdapter.java': '''package android.widget;
public class ArrayAdapter<T> {
 final android.content.Context context;final java.util.List<T> items;public int notifications;
 public ArrayAdapter(android.content.Context c,int r,java.util.List<T> values){context=c;items=values;}
 public android.content.Context getContext(){return context;}public T getItem(int position){return items.get(position);}
 public void notifyDataSetChanged(){notifications++;} public android.view.View getView(int p,android.view.View v,android.view.ViewGroup g){return v;}
}''',
    'bttv/highlight/StringSetUI.java': '''package bttv.highlight;
public class StringSetUI {public final java.util.Set<String> saved=new java.util.HashSet<>();public void remove(String word){saved.remove(word);}}''',
    'android/util/Log.java': '''package android.util;
public final class Log {
 public static final java.util.List<String> lines = new java.util.concurrent.CopyOnWriteArrayList<>();
 public static int i(String tag,String value){lines.add(tag+":"+value);return 0;}
 public static int w(String tag,String value){return i(tag,value);}
 public static int w(String tag,String value,Throwable t){return i(tag,value);}
}''',
    'android/os/SystemClock.java': '''package android.os;
public final class SystemClock {public static long now=100000;public static long elapsedRealtime(){return now;}}''',
    'bttv/Data.java': '''package bttv; public final class Data {public static Object ctx=new Object();}''',
    'bttv/ResUtil.java': '''package bttv;
public final class ResUtil {
 public static final java.util.Set<bttv.settings.Settings> enabled=java.util.EnumSet.allOf(bttv.settings.Settings.class);
 public static boolean disabled;
 public static boolean getBooleanFromSettings(bttv.settings.Settings s){return enabled.contains(s);}
 public static boolean selectedDropdownFromSettingsIs(bttv.settings.Settings s,Res.strings r){return disabled;}
 public static int getResourceId(Res.strings r){return r.ordinal()+1;}
 public static int getResourceId(android.content.Context c,Res.ids r){return r.ordinal()+1;}
}''',
    'bttv/emote/LiveEmoteGlobals.java': '''package bttv.emote;
public final class LiveEmoteGlobals {
 public static volatile int calls,loads,status;
 public static class HttpFailure extends java.io.IOException {public final int status;public HttpFailure(int s){status=s;}}
 public static void load(Emotes.Source s){loads++;}
 public static String fetch(String ignored) throws java.io.IOException {
  calls++;if(status!=0)throw new HttpFailure(status);
  return "{\\"channelEmotes\\":[{\\"id\\":\\"channel-emote\\",\\"code\\":\\"ChannelCode\\",\\"imageType\\":\\"png\\"}],\\"sharedEmotes\\":[]}";
 }
}''',
    'bttv/emote/TraceCleanupHostTest.java': '''package bttv.emote;
import java.util.*;import java.lang.reflect.*;import java.util.concurrent.*;
import android.util.Log;import android.widget.MultiAutoCompleteTextView;import bttv.ResUtil;import bttv.settings.Settings;
public final class TraceCleanupHostTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static Emote emote(String id,Emotes.Source s,String code,String type){return new Emote(id,s,code,"https://cdn.betterttv.net/emote/"+id+"/2x",type,null);}
 static void idle() throws Exception {
  Field f=LiveEmoteChannels.class.getDeclaredField("WORKERS");f.setAccessible(true);ThreadPoolExecutor e=(ThreadPoolExecutor)f.get(null);
  for(int i=0;i<300;i++){if(e.getActiveCount()==0&&e.getQueue().isEmpty())return;Thread.sleep(10);}
  throw new AssertionError("worker timeout");
 }
 public static void main(String[] args) throws Exception {
  check(!LiveEmoteAutocomplete.DEBUG_LOGGING,"production diagnostics disabled");
  for(Emotes.Source s:Emotes.Source.values()){
   Map<String,Emote> map=s==Emotes.Source.FFZ?Emotes.globalEmotesFFZ:s==Emotes.Source.BTTV?Emotes.globalEmotesBTTV:Emotes.globalEmotes7TV;
   map.put(s.name(),emote(s.name(),s,s.name(),"png"));
  }
  for(String code:new String[]{"FFZ","BTTV","STV"}){
   List<LiveEmotes.Part> result=LiveEmotes.split("before "+code+" after","123456789");
   check(result!=null&&result.size()==3,"provider match "+code);
   check(result.get(0).text.equals("before ")&&result.get(2).text.equals(" after"),"text preserved");
   check(result.get(1).text.equals(code),"display code preserved");
   check(LiveEmotes.urlForMarker(result.get(1).marker,code).equals("https://cdn.betterttv.net/emote/"+code+"/2x"),"URL preserved");
  }
  check(LiveEmotes.split("ordinary unmatched text","123456789")==null,"unmatched text unchanged");
  check(LiveEmotes.urlForMarker("emotesv2_native","Native")==null,"native URL path unchanged");
  Emotes.globalEmotesBTTV.put("Animated",emote("anim",Emotes.Source.BTTV,"Animated","gif"));
  check(LiveEmotes.split("Animated","123456789")!=null,"animation remains eligible");
  ResUtil.disabled=true;check(LiveEmotes.split("Animated","123456789")==null,"disabled GIF policy unchanged");ResUtil.disabled=false;
  check(Log.lines.isEmpty(),"matches and URL resolution log nothing");
  check(LiveEmoteGlobals.calls==0&&LiveEmoteGlobals.loads==0,"token matching adds no requests");
  MultiAutoCompleteTextView input=new MultiAutoCompleteTextView();
  LiveEmoteAutocomplete.attach(input);LiveEmoteAutocomplete.attach(input);
  LiveEmoteAutocomplete.setChannel(input,"123456789");
  Object owner=LiveEmoteAutocomplete.uniqueLifecycle("123456789").controller.get();
  Method log=owner.getClass().getDeclaredMethod("log",String.class);log.setAccessible(true);log.invoke(owner,"private-debug-value");
  input.detach();check(LiveEmoteAutocomplete.uniqueLifecycle("123456789")==null,"detach invalidates lifecycle");
  input.attached=true;((android.view.View.OnAttachStateChangeListener)owner).onViewAttachedToWindow(input);
  check(LiveEmoteAutocomplete.uniqueLifecycle("123456789").controller.get()==owner,"reattach preserves owner");
  check(Log.lines.isEmpty(),"normal lifecycle emits no logs");
  ResUtil.enabled.clear();ResUtil.enabled.add(Settings.BTTVEmotesEnabled);
  for(int i=0;i<30;i++)LiveEmoteChannels.ensureChannel("123456789");idle();
  check(LiveEmoteGlobals.calls==1,"channel loading remains single-flight");
  check(LiveEmoteChannels.snapshot("123456789",Emotes.Source.BTTV).containsKey("ChannelCode"),"channel catalog published");
  check(Log.lines.isEmpty(),"successful channel lifecycle emits no logs");
  LiveEmoteGlobals.status=403;LiveEmoteChannels.ensureChannel("987654321");idle();
  check(Log.lines.size()==1&&Log.lines.get(0).contains("http=403"),"provider failure category retained");
  check(!Log.lines.get(0).contains("987654321"),"failure omits channel ID");
  int calls=LiveEmoteGlobals.calls;LiveEmoteChannels.ensureChannel("987654321");idle();check(LiveEmoteGlobals.calls==calls,"retry deadline unchanged");
  android.os.SystemClock.now+=61000;LiveEmoteChannels.ensureChannel("987654321");idle();check(LiveEmoteGlobals.calls==calls+1,"retry eligibility unchanged");
  LiveEmoteGlobals.status=404;LiveEmoteChannels.ensureChannel("444444444");idle();calls=LiveEmoteGlobals.calls;
  LiveEmoteChannels.ensureChannel("444444444");idle();check(LiveEmoteGlobals.calls==calls,"404 remains loaded empty");
  check(Log.lines.stream().anyMatch(v->v.contains("http=404 (empty)")),"404 reporting retained");
  check(Log.lines.stream().noneMatch(v->v.contains("444444444")||v.contains("private-debug-value")),"privacy-safe logs");
  Method failure=LiveEmoteAutocomplete.class.getDeclaredMethod("failure",String.class,Throwable.class);failure.setAccessible(true);
  failure.invoke(null,"hostCheck",new IllegalStateException("private-exception-message"));
  check(Log.lines.get(Log.lines.size()-1).contains("error=IllegalStateException"),"compatibility category retained");
  check(Log.lines.stream().noneMatch(v->v.contains("private-exception-message")),"exception message not logged");
  for(String word:new String[]{"private-highlight-word","private-blacklist-word"}){
   int logs=Log.lines.size();android.content.Context context=new android.content.Context();
   bttv.highlight.StringSetUI set=new bttv.highlight.StringSetUI();set.saved.add(word);
   List<String> words=new ArrayList<>(Arrays.asList(word,"remaining"));
   bttv.highlight.StringSetUIAdapter adapter=new bttv.highlight.StringSetUIAdapter(context,1,words,set);
   int[] callbacks={0};adapter.afterRemovedListener=()->callbacks[0]++;
   android.view.ViewGroup row=new android.view.ViewGroup();android.widget.ImageButton button=new android.widget.ImageButton();
   row.children.put(bttv.Res.ids.bttv_highlight_dia_list_text.ordinal()+1,new android.widget.TextView());
   row.children.put(bttv.Res.ids.bttv_highlight_dia_list_btn.ordinal()+1,button);
   adapter.getView(0,null,row);button.click();
   check(!set.saved.contains(word)&&words.equals(Arrays.asList("remaining")),"editor removes saved and visible entry");
   check(adapter.notifications==1&&callbacks[0]==1,"editor refresh and callback unchanged");
   check(Log.lines.size()==logs,"removed private word is never logged");
  }
  System.out.println("PASS Stage 5Q-B "+checks+" JVM emote/lifecycle/fetch-policy/privacy checks (no real HTTP)");
 }
}'''
})

if __name__ == '__main__':
    destination = Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        target = destination / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source + '\n', encoding='utf-8', newline='\n')
    print('Generated Stage 5Q-B isolated JVM fixtures:', len(SOURCES))
