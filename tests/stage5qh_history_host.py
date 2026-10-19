"""Generate isolated fixtures around REAL usage/picker/resolver classes. No HTTP/device actions.

Run with an output directory below .tmp/stage5qh/phase2. Platform/native ABI doubles
are explicit; these tests do not claim ART, actual Twitch delivery or physical-device proof.
"""
from pathlib import Path
import runpy,sys
ROOT=Path(__file__).resolve().parents[1]
SOURCES=dict(runpy.run_path(str(ROOT/'tests/stage5q_emote_consistency_host.py'))['SOURCES'])
SOURCES['bttv/Data.java']='package bttv; public final class Data {public static android.content.Context ctx=new android.content.Context();}'
SOURCES['android/content/Context.java']='''package android.content;
public class Context {
 public static final int MODE_PRIVATE=0;
 public Context getApplicationContext(){return this;}
 public android.content.SharedPreferences getSharedPreferences(String n,int m){throw new UnsupportedOperationException();}
}'''
SOURCES['bttv/settings/Settings.java']=SOURCES['bttv/settings/Settings.java'].replace('GifRenderMode','GifRenderMode,AnonChatEnabled')
SOURCES['bttv/ResUtil.java']=SOURCES['bttv/ResUtil.java'].replace('public static boolean disabled, unavailable;', 'public static boolean disabled, unavailable, anon;').replace('return enabled.contains(s);','return s==bttv.settings.Settings.AnonChatEnabled ? anon : enabled.contains(s);')
# The new context adapter is tested separately from native bytecode/constructor/link checks.
bridge=SOURCES['bttvcompat/NativePickerBridge.java']
bridge=bridge.replace(' public static String pending;', ''' public static String account="123";
 public static String viewer(Object p){return account;}
 public static android.content.SharedPreferences accountPrefs;
 public static Object accountPreferences(Object p){return accountPrefs;}
 public static Object frequentSection(List<?> cells){Section s=new Section(cells);s.frequent=true;return s;}
 public static String pending;''')
SOURCES['bttvcompat/NativePickerBridge.java']=bridge
SOURCES['bttv/emote/UsageResolverHostTest.java']=r'''package bttv.emote;
import java.util.*;import java.lang.reflect.*;import bttv.Res;import bttv.ResUtil;import bttv.settings.Settings;
public class UsageResolverHostTest {
 static int checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static Emote e(String p,String id,String code){return new Emote(id,Emotes.Source.valueOf(p),code,"https://cdn.example.test/"+id,"png",null);}
 static Map<String,Emote> global(String p){return p.equals("FFZ")?Emotes.globalEmotesFFZ:p.equals("BTTV")?Emotes.globalEmotesBTTV:Emotes.globalEmotes7TV;}
 @SuppressWarnings("unchecked") static void channel(String p,Emote e)throws Exception{
  Field f=LiveEmoteChannels.class.getDeclaredField("CATALOGS");f.setAccessible(true);Map<String,Object> catalogs=(Map<String,Object>)f.get(null);
  Object catalog=catalogs.get("100");if(catalog==null){Constructor<?> c=Class.forName("bttv.emote.LiveEmoteChannels$Catalog").getDeclaredConstructor();c.setAccessible(true);catalog=c.newInstance();catalogs.put("100",catalog);}
  Field providers=catalog.getClass().getDeclaredField("providers");providers.setAccessible(true);Object req=((Object[])providers.get(catalog))[Emotes.Source.valueOf(p).ordinal()];
  Field snapshot=req.getClass().getDeclaredField("snapshot");snapshot.setAccessible(true);snapshot.set(req,Collections.singletonMap(e.code,e));
 }
 static void reset()throws Exception{
  for(String p:new String[]{"FFZ","BTTV","STV"})global(p).clear();
  Field f=LiveEmoteChannels.class.getDeclaredField("CATALOGS");f.setAccessible(true);((Map<?,?>)f.get(null)).clear();
  ResUtil.enabled.clear();ResUtil.enabled.addAll(EnumSet.allOf(Settings.class));ResUtil.disabled=false;
 }
 static Emote add(int i,String code)throws Exception{
  String p=new String[]{"FFZ","FFZ","BTTV","BTTV","STV","STV"}[i];Emote e=e(p,"id"+i,code);
  if(i%2==0)global(p).put(code,e);else channel(p,e);return e;
 }
 public static void main(String[]args)throws Exception{
  for(int a=0;a<6;a++)for(int b=a+1;b<6;b++){
   reset();Emote first=add(a,"Code");add(b,"Code");
   List<LiveEmotePicker.Entry> out=LiveEmotes.usageLoaded("Code Code Code","100",Collections.emptySet());
   check(out.size()==3,"three occurrences");check(out.get(0).id.equals(first.id),"precedence pair "+a+"/"+b);
   check(out.get(0).channel==(a%2==1),"scope");
  }
  reset();for(int i=0;i<6;i++)add(i,"Code");check(LiveEmotes.usageLoaded("Code","100",Collections.emptySet()).get(0).provider.equals("FFZ"),"six-way");
  check(LiveEmotes.usageLoaded("Code","100",Collections.singleton("Code")).isEmpty(),"native exact reservation");
  check(LiveEmotes.usageLoaded("(Code!)","100",Collections.singleton("Code")).isEmpty(),"native punctuation conservative");
  check(LiveEmotes.usageLoaded("xCode CodeX @Code https://host/Code www.Code","100",Collections.emptySet()).isEmpty(),"no embedded/mention/URL");
  check(LiveEmotes.usageLoaded("(Code!)\tCode\nCode","100",Collections.emptySet()).size()==3,"same whitespace/outer punctuation matcher");
  check(LiveEmotes.usageLoaded("code","100",Collections.emptySet()).isEmpty(),"case-sensitive code");
  ResUtil.enabled.remove(Settings.FFZEmotesEnabled);check(LiveEmotes.usageLoaded("Code","100",Collections.emptySet()).get(0).provider.equals("BTTV"),"provider toggle");
  reset();Emote gif=add(0,"Code");gif.imageType="gif";Emote still=add(2,"Code");ResUtil.disabled=true;
  check(LiveEmotes.usageLoaded("Code","100",Collections.emptySet()).get(0).id.equals(still.id),"disabled GIF winner");
  reset();Emote invalid=add(0,"Code");invalid.url="http://bad";add(2,"Code");
  check(LiveEmotes.usageLoaded("Code","100",Collections.emptySet()).isEmpty(),"malformed winner no substitution");
  reset();add(5,"Alias");check(LiveEmotes.usageLoaded("Alias","200",Collections.emptySet()).isEmpty(),"wrong channel");
  check(LiveEmotes.usageLoaded("Alias","100",Collections.emptySet()).size()==1,"valid channel");
  check(LiveEmoteGlobals.calls==0 && LiveEmoteGlobals.loads==0,"loaded-only: no provider requests");
  System.out.println("PASS "+checks+" actual resolver checks; no delivery/device claim");
 }
}'''
ANDROID_TEST=r'''package bttv.emote;
import org.junit.*;import static org.junit.Assert.*;
import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.Shadows;
import android.app.Activity;import android.widget.EditText;import android.content.SharedPreferences;
import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;
import bttv.Data;import bttv.ResUtil;import bttv.settings.Settings;import bttvcompat.NativePickerBridge;
@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE,sdk=30)
public class UsageAndroidTest {
 Activity activity;EditText input;Object presenter;PickerRuntime.Stream stream;NativePickerBridge.Capture capture;Object meta;
 static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 void idle()throws Exception{
  Object store=field(EmoteUsageStore.class,"instance").get(null);
  if(store!=null)((ThreadPoolExecutor)field(EmoteUsageStore.class,"queue").get(store)).submit(()->{}).get(10,TimeUnit.SECONDS);
  org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle();
 }
 EmoteUsageHistory.State state()throws Exception{
  Object store=field(EmoteUsageStore.class,"instance").get(null);
  return ((EmoteUsageStore.Worker)field(EmoteUsageStore.class,"worker").get(store)).state;
 }
 void stopStore()throws Exception{
  Object store=field(EmoteUsageStore.class,"instance").get(null);
  if(store!=null)((ThreadPoolExecutor)field(EmoteUsageStore.class,"queue").get(store)).shutdown();
  field(EmoteUsageStore.class,"instance").set(null,null);
 }
 @Before public void setup()throws Exception{
  Data.ctx=RuntimeEnvironment.getApplication();stopStore();Data.ctx.getSharedPreferences("BTTV_EmoteUsage",0).edit().clear().commit();
  NativePickerBridge.account="123";NativePickerBridge.accountPrefs=Data.ctx.getSharedPreferences("native-account-fixture",0);
  ResUtil.anon=false;ResUtil.disabled=false;ResUtil.enabled.clear();ResUtil.enabled.addAll(EnumSet.allOf(Settings.class));
  Emotes.globalEmotesFFZ=new HashMap<>();Emotes.globalEmotesBTTV=new HashMap<>();Emotes.globalEmotes7TV=new HashMap<>();
  Emotes.globalEmotesFFZ.put("Code",new Emote("ffz-id",Emotes.Source.FFZ,"Code","https://cdn.example.test/ffz-id","png",null));
  activity=Robolectric.buildActivity(Activity.class).setup().visible().get();input=new EditText(activity);activity.setContentView(input);input.requestFocus();
  LiveEmoteAutocomplete.attach(input);LiveEmoteAutocomplete.setChannel(input,"100");presenter=new Object();meta=new Object();
 }
 void open(int natives)throws Exception{
  if(stream!=null)stream.dispose();
  Object bound=PickerRuntime.bind(new Object[]{presenter,"100"});assertNotNull(bound);
  capture=new NativePickerBridge.Capture();PickerRuntime.open(new Object(),capture,bound);stream=capture.stream;
  List<Object> cells=new ArrayList<>();for(int i=0;i<natives;i++)cells.add(NativePickerBridge.cell("native"+i,"Twitch"+i,false));
  Object section=NativePickerBridge.frequentSection(cells);
  stream.next(new Object[]{Collections.singletonList(section),meta});idle();
 }
 Object latest(){return capture.values.get(capture.values.size()-1);}
 List<?> cells(){return NativePickerBridge.cells(NativePickerBridge.sections(latest()).get(0));}
 @After public void cleanup()throws Exception{
  if(stream!=null)stream.dispose();idle();stopStore();
  activity.setContentView(new android.view.View(activity));activity.finish();
  ((Map<?,?>)field(LiveEmoteAutocomplete.class,"CONTROLLERS").get(null)).clear();
 }
 @Test public void actualForwardedCoordinatorAndPersistence()throws Exception{
  // No seed: fail closed rather than infer native eligibility from third-party catalogs.
  EmoteUsageStore.submitted(input,"123","100","Code");assertNull(field(EmoteUsageStore.class,"instance").get(null));
  open(24);assertEquals(24,cells().size());Object nativeFirst=cells().get(0);
  EmoteUsageStore.submitted(input,"123","100","Code Code Code");idle();
  assertEquals(3,state().list("123").get(0).count);assertEquals(25,cells().size());assertSame(nativeFirst,cells().get(0));
  assertSame(meta,((Object[])latest())[1]);
  Object saved=cells().get(24);String id=NativePickerBridge.id(saved);assertTrue(id.startsWith("bttv31pk:"));
  assertTrue(PickerRuntime.click(presenter,id));input.setText("");input.setSelection(0);
  assertTrue(PickerRuntime.insert(input,NativePickerBridge.pending));assertEquals("Code ",input.getText().toString());
  input.setText("");idle();assertEquals(3,state().list("123").get(0).count); // insertion/deletion are not use
  EmoteUsageStore.submitted(input,"123","100","Code");EmoteUsageStore.submitted(input,"123","100","Code");idle();
  assertEquals(5,state().list("123").get(0).count);
  stream.dispose();stopStore();open(24);assertEquals(25,cells().size());assertEquals(5,state().list("123").get(0).count);
 }
 @Test public void rejectionPrivacyAndStaleAccounts()throws Exception{
  open(1);
  for(String text:new String[]{"", "/me Code","@Code","https://x/Code","ordinary"})EmoteUsageStore.submitted(input,"123","100",text);
  EmoteUsageStore.submitted(input,"999","100","Code");EmoteUsageStore.submitted(input,"123","200","Code");
  EmoteUsageStore.submitted(null,"123","100","Code");ResUtil.anon=true;EmoteUsageStore.submitted(input,"123","100","Code");ResUtil.anon=false;idle();
  assertTrue(state().accounts.isEmpty());
  EmoteUsageStore.submitted(input,"123","100","Code");idle();String old=NativePickerBridge.id(cells().get(1));
  NativePickerBridge.account="456";NativePickerBridge.accountPrefs.edit().putString("identity-fixture","changed").commit();idle();
  assertEquals("",PickerRuntime.url(old));assertTrue(PickerRuntime.click(presenter,old));
  presenter=new Object();open(1);assertEquals(1,cells().size());assertTrue(state().list("456").isEmpty());
 }
 @Test public void nativeCollisionAndAbsentFrequentFallback()throws Exception{
  open(0);
  Object nativeCell=NativePickerBridge.cell("twitch-id","Code",false);
  stream.next(new Object[]{Collections.singletonList(NativePickerBridge.frequentSection(Collections.singletonList(nativeCell))),meta});
  EmoteUsageStore.submitted(input,"123","100","(Code!)");idle();assertTrue(state().accounts.isEmpty());
  stream.next(new Object[]{Collections.emptyList(),meta});EmoteUsageStore.submitted(input,"123","100","Code");idle();
  long sections=NativePickerBridge.sections(latest()).stream().filter(NativePickerBridge::frequent).count();assertEquals(1,sections);
  assertEquals(1,cells().size());assertSame(meta,((Object[])latest())[1]);
 }
 @Test public void allProvidersQueuedSendsAndNoOffMainRecording()throws Exception{
  Emotes.globalEmotesBTTV.put("Other",new Emote("bttv-id",Emotes.Source.BTTV,"Other","https://cdn.example.test/b","png",null));
  Emotes.globalEmotes7TV.put("Third",new Emote("stv-id",Emotes.Source.STV,"Third","https://cdn.example.test/s","png",null));
  open(0);
  // These are actual coordinator invocations modelling forwarded native sends, not delivered messages.
  for(int i=0;i<12;i++)EmoteUsageStore.submitted(input,"123","100","Code Other Third");
  idle();assertEquals(3,state().list("123").size());
  for(EmoteUsageHistory.Use use:state().list("123"))assertEquals(12,use.count);
  Thread t=new Thread(()->EmoteUsageStore.submitted(input,"123","100","Code"));t.start();t.join();idle();
  for(EmoteUsageHistory.Use use:state().list("123"))assertEquals(12,use.count);
  assertEquals(3,cells().size());
 }
 @Test public void providerRevalidationAndActivityRecreation()throws Exception{
  open(1);EmoteUsageStore.submitted(input,"123","100","Code");idle();String old=NativePickerBridge.id(cells().get(1));
  ResUtil.enabled.remove(Settings.FFZEmotesEnabled);EmoteCatalogEvents.published("FFZ",null);idle();assertEquals(1,cells().size());
  assertEquals(1,state().list("123").size());assertEquals("",PickerRuntime.url(old));
  ResUtil.enabled.add(Settings.FFZEmotesEnabled);EmoteCatalogEvents.published("FFZ",null);idle();assertEquals(2,cells().size());
  EditText stale=input;activity.setContentView(new android.view.View(activity));activity.finish();
  activity=Robolectric.buildActivity(Activity.class).setup().visible().get();input=new EditText(activity);activity.setContentView(input);
  LiveEmoteAutocomplete.attach(input);LiveEmoteAutocomplete.setChannel(input,"100");presenter=new Object();open(1);
  assertEquals(2,cells().size());assertEquals("",PickerRuntime.url(old));
  EmoteUsageStore.submitted(stale,"123","100","Code");idle();assertEquals(1,state().list("123").get(0).count);
 }
 @Test public void nativeForwardingFailureIsolationModel(){
  // Java control-flow equivalent of the *single* protected smali invoke. Binary handler checked independently.
  for(Throwable failure:Arrays.asList(new RuntimeException(),new NoClassDefFoundError(),new NoSuchMethodError(),new ExceptionInInitializerError())){
   int[] calls={0};try{try{throw failure;}catch(Throwable ignored){}calls[0]++;calls[0]++;}catch(Throwable wrong){fail();}
   assertEquals(2,calls[0]);
  }
  try{try{}catch(Throwable ignored){}throw new IllegalStateException("stock");}catch(IllegalStateException original){assertEquals("stock",original.getMessage());}
 }
}'''
if __name__=='__main__':
 dest=Path(sys.argv[1]).resolve();dest.relative_to((ROOT/'.tmp/stage5qh/phase2').resolve())
 for name,code in SOURCES.items():
  target=dest/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(code,encoding='utf-8')
 print('Generated',len(SOURCES),'explicit isolated fixtures')
