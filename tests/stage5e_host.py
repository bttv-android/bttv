"""Generate isolated JVM fixtures for the real picker coordinator (not an Android/device test).

Usage: python tests/stage5e_host.py <ignored-output-directory>
Compile these fixtures with the production picker/C2 sources and JUnit; never package them.
"""
from pathlib import Path
import sys

SOURCES = {
'android/os/Looper.java': '''package android.os;
public final class Looper {
 public static boolean main=true; private static final Looper MAIN=new Looper();
 public static Looper myLooper(){return main?MAIN:null;} public static Looper getMainLooper(){return MAIN;}
}''',
'android/os/Handler.java': '''package android.os;
public class Handler {
 static final java.util.List<Runnable> queue=new java.util.ArrayList<>();
 public Handler(Looper l){} public boolean post(Runnable r){queue.add(r);return true;}
 public static void drain(){Looper.main=true;while(!queue.isEmpty())queue.remove(0).run();}
}''',
'android/view/View.java': '''package android.view;
public class View {
 public interface OnAttachStateChangeListener {void onViewAttachedToWindow(View v);void onViewDetachedFromWindow(View v);}
 public boolean attached=true; final java.util.List<OnAttachStateChangeListener> listeners=new java.util.ArrayList<>();
 public void addOnAttachStateChangeListener(OnAttachStateChangeListener l){listeners.add(l);}
 public boolean isAttachedToWindow(){return attached;} public boolean hasFocus(){return true;}
 public void detach(){attached=false;for(OnAttachStateChangeListener l:listeners)l.onViewDetachedFromWindow(this);}
}''',
'android/text/Editable.java': '''package android.text;
public interface Editable extends CharSequence {Editable replace(int s,int e,CharSequence value);}''',
'android/widget/EditText.java': '''package android.widget;
public class EditText extends android.view.View {
 public static class Text implements android.text.Editable {
  final StringBuilder b=new StringBuilder(); public int length(){return b.length();}
  public char charAt(int i){return b.charAt(i);} public CharSequence subSequence(int s,int e){return b.substring(s,e);}
  public String toString(){return b.toString();} public android.text.Editable replace(int s,int e,CharSequence v){b.replace(s,e,v.toString());return this;}
 }
 public final Text text=new Text(); public int start,end;
 public android.text.Editable getText(){return text;} public int getSelectionStart(){return start;}
 public int getSelectionEnd(){return end;} public void setSelection(int p){start=end=p;}
}''',
'android/widget/MultiAutoCompleteTextView.java': '''package android.widget;
public class MultiAutoCompleteTextView extends EditText {
 public ListAdapter getAdapter(){return null;} public void dismissDropDown(){}
}''',
'android/util/Log.java': '''package android.util; public class Log { public static int i(String t,String m){return 0;} }''',
'bttv/settings/Settings.java': '''package bttv.settings;
public enum Settings {FFZEmotesEnabled,BTTVEmotesEnabled,SevenTVEmotesEnabled,GifRenderMode}''',
'bttv/ResUtil.java': '''package bttv;
public class ResUtil {
 public static boolean getBooleanFromSettings(bttv.settings.Settings s){return true;}
 public static boolean selectedDropdownFromSettingsIs(bttv.settings.Settings s,Res.strings r){return false;}
 public static int getResourceId(Res.strings r){return r.ordinal()+1;}
}''',
'bttv/emote/Emotes.java': '''package bttv.emote;
public class Emotes {
 public enum Source {FFZ,BTTV,STV}
 public static java.util.Map<String,Emote> globalEmotesFFZ=new java.util.HashMap<>();
 public static java.util.Map<String,Emote> globalEmotesBTTV=new java.util.HashMap<>();
 public static java.util.Map<String,Emote> globalEmotes7TV=new java.util.HashMap<>();
}''',
'bttv/emote/LiveEmoteChannels.java': '''package bttv.emote;
public class LiveEmoteChannels {
 public static final java.util.Map<String,java.util.Map<String,Emote>> maps=new java.util.HashMap<>();
 static java.util.Map<String,Emote> snapshot(String c,Emotes.Source s){return maps.getOrDefault(c+s.name(),java.util.Collections.emptyMap());}
}''',
'bttvcompat/NativePickerBridge.java': '''package bttvcompat;
import java.util.*; import android.widget.EditText; import bttv.emote.PickerRuntime;
public class NativePickerBridge {
 public static class Cell {public String id,code;public boolean unlocked=true;Cell(String i,String c){id=i;code=c;}}
 public static class Section {public boolean frequent;public List<?> cells;Section(List<?> c){cells=c;}}
 public static class Capture {public PickerRuntime.Stream stream;public final List<Object> values=new ArrayList<>();public Throwable error;public boolean complete;}
 public static String pending; public static int requests,cancels;
 public static Object presenter(Object c){return ((Object[])c)[0];} public static String channel(Object c){return (String)((Object[])c)[1];}
 public static String id(Object c){return ((Cell)c).id;} public static String code(Object c){return ((Cell)c).unlocked?((Cell)c).code:null;}
 public static List<?> sections(Object p){return (List<?>)((Object[])p)[0];} public static List<?> cells(Object s){return ((Section)s).cells;}
 public static boolean frequent(Object s){return ((Section)s).frequent;}
 public static Object copySection(Object s,List<?> cells){Section n=new Section(cells);n.frequent=((Section)s).frequent;return n;}
 public static Object pair(Object original,List<?> s){return new Object[]{s,((Object[])original)[1]};}
 public static Object cell(String id,String code,boolean a){return new Cell(id,code);}
 public static Object section(int title,boolean c,List<?> cells){return new Section(cells);}
 public static void dispatch(Object presenter,String text,String id){pending=text;}
 public static EditText input(Object composer){return (EditText)composer;}
 public static void subscribe(Object upstream,Object emitter,Object stream){Capture c=(Capture)emitter;c.stream=(PickerRuntime.Stream)stream;c.stream.subscribed(upstream);}
 public static void cancel(Object s){cancels++;} public static void request(Object s){requests++;}
 public static void emit(Object e,Object p){((Capture)e).values.add(p);}
 public static void error(Object e,Throwable t){((Capture)e).error=t;}
 public static void complete(Object e){((Capture)e).complete=true;}
}''',
'bttv/emote/PickerCoordinatorHostTest.java': '''package bttv.emote;
import java.util.*; import org.junit.*; import static org.junit.Assert.*;
import android.widget.MultiAutoCompleteTextView; import android.os.*; import bttvcompat.NativePickerBridge;
public class PickerCoordinatorHostTest {
 final List<MultiAutoCompleteTextView> inputs=new ArrayList<>(); final List<NativePickerBridge.Capture> captures=new ArrayList<>();
 Object presenter; MultiAutoCompleteTextView input; Object original; NativePickerBridge.Capture capture;
 @Before public void setup(){
  Looper.main=true;Emotes.globalEmotesFFZ.clear();Emotes.globalEmotesBTTV.clear();Emotes.globalEmotes7TV.clear();LiveEmoteChannels.maps.clear();
  presenter=new Object(); input=input("101"); original=new Object[]{new ArrayList<>(),Boolean.TRUE};
 }
 MultiAutoCompleteTextView input(String c){MultiAutoCompleteTextView i=new MultiAutoCompleteTextView();inputs.add(i);LiveEmoteAutocomplete.attach(i);LiveEmoteAutocomplete.setChannel(i,c);return i;}
 void open(){Object binding=PickerRuntime.bind(new Object[]{presenter,"101"});assertNotNull(binding);capture=new NativePickerBridge.Capture();captures.add(capture);PickerRuntime.open(new Object(),capture,binding);capture.stream.next(original);}
 void publish(String channel){Map<String,Emote> m=new HashMap<>();m.put("Alias",new Emote("id",Emotes.Source.STV,"Alias","https://cdn.example.test/x","png",null));LiveEmoteChannels.maps.put(channel+"STV",m);EmoteCatalogEvents.published("STV",channel);}
 Object latest(){return capture.values.get(capture.values.size()-1);}
 List<?> sections(){return NativePickerBridge.sections(latest());}
 String id(){return NativePickerBridge.id(NativePickerBridge.cells(sections().get(sections().size()-1)).get(0));}
 @After public void cleanup(){for(NativePickerBridge.Capture c:captures)c.stream.dispose();for(MultiAutoCompleteTextView i:inputs)i.detach();Handler.drain();NativePickerBridge.pending=null;}
 @Test public void coldCompletionAndReemissionAreIdempotent(){open();assertTrue(sections().isEmpty());publish("101");assertEquals(1,sections().size());capture.stream.next(original);assertEquals(1,sections().size());assertTrue(((List<?>)((Object[])original)[0]).isEmpty());assertSame(Boolean.TRUE,((Object[])latest())[1]);}
 @Test public void completionBeforeRegistrationSeedsImmediately(){publish("101");open();assertEquals(1,sections().size());}
 @Test public void otherChannelDoesNotRefresh(){open();int n=capture.values.size();publish("202");assertEquals(n,capture.values.size());}
 @Test public void switchReturnAndQueuedCompletionCannotResurrect(){open();publish("101");String old=id();Looper.main=false;publish("101");Looper.main=true;LiveEmoteAutocomplete.setChannel(input,"202");LiveEmoteAutocomplete.setChannel(input,"101");Handler.drain();assertSame(original,latest());assertEquals("",PickerRuntime.url(old));assertTrue(PickerRuntime.click(presenter,old));assertNull(NativePickerBridge.pending);}
 @Test public void detachRemovesProviderState(){open();publish("101");input.detach();assertSame(original,latest());}
 @Test public void missingAndAmbiguousComposersRemainStockOnly(){input.detach();assertNull(PickerRuntime.bind(new Object[]{presenter,"101"}));input("101");input("101");assertNull(PickerRuntime.bind(new Object[]{presenter,"101"}));}
 @Test public void identityRouteInsertsOnceWithSpace(){open();publish("101");assertTrue(PickerRuntime.click(presenter,id()));String token=NativePickerBridge.pending;assertEquals("Alias",token);assertFalse(PickerRuntime.insert(input,new String(token)));assertTrue(PickerRuntime.insert(input,token));assertEquals("Alias ",input.text.toString());assertTrue(PickerRuntime.insert(input,token));assertEquals("Alias ",input.text.toString());}
 @Test public void wrongComposerConsumesWithoutInsertion(){open();publish("101");PickerRuntime.click(presenter,id());String token=NativePickerBridge.pending;MultiAutoCompleteTextView b=input("202");assertTrue(PickerRuntime.insert(b,token));assertTrue(PickerRuntime.insert(input,token));assertEquals("",input.text.toString());assertEquals("",b.text.toString());}
 @Test public void catalogRemovalInvalidatesUrlAndPendingSelection(){open();publish("101");String old=id();PickerRuntime.click(presenter,old);LiveEmoteChannels.maps.clear();assertEquals("",PickerRuntime.url(old));assertTrue(PickerRuntime.insert(input,NativePickerBridge.pending));assertEquals("",input.text.toString());}
 @Test public void upstreamErrorsPropagateAndDisposalStopsRefresh(){open();Throwable e=new IllegalStateException("native");capture.stream.error(e);assertSame(e,capture.error);int n=capture.values.size();publish("101");assertEquals(n,capture.values.size());}
 @Test public void nativeSectionsAndOnlyPickerHistoryArePreserved(){
  Object stock=NativePickerBridge.cell("twitch-id","Native",false); Object ac=NativePickerBridge.cell("bttv31ac://STV/id","AC",false);
  Object stale=NativePickerBridge.cell("bttv31pk:old","Stale",false);
  NativePickerBridge.Section recent=(NativePickerBridge.Section)NativePickerBridge.section(1,false,Arrays.asList(stock,ac,stale));recent.frequent=true;
  Object untouched=NativePickerBridge.section(2,false,Arrays.asList(stock));original=new Object[]{Arrays.asList(recent,untouched),Boolean.FALSE};open();
  assertEquals(2,NativePickerBridge.cells(sections().get(0)).size());assertSame(stock,NativePickerBridge.cells(sections().get(0)).get(0));assertSame(ac,NativePickerBridge.cells(sections().get(0)).get(1));assertSame(untouched,sections().get(1));assertSame(Boolean.FALSE,((Object[])latest())[1]);
 }
 @Test public void onlyUnlockedNativeCodeCollides(){
  Object cell=NativePickerBridge.cell("twitch-id","Alias",false);original=new Object[]{Arrays.asList(NativePickerBridge.section(1,false,Arrays.asList(cell))),Boolean.TRUE};publish("101");open();assertEquals(1,sections().size());((NativePickerBridge.Cell)cell).unlocked=false;capture.stream.next(original);assertEquals(2,sections().size());
 }
}'''
}

if __name__ == '__main__':
    output=Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        target=output/name
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_text(source+'\n',encoding='utf-8')
    print(f'Generated {len(SOURCES)} isolated JVM fixtures in {output}')
