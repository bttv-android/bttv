"""Generate isolated tests of actual badge sources; Android graphics use Robolectric, not stubs.

Run: python tests/stage5q_badges_host.py <new ignored output directory>
The runner supplies pinned test-only Robolectric/AndroidSVG dependencies separately.
"""
from pathlib import Path
import shutil
import sys

ROOT = Path(__file__).resolve().parents[1]
SOURCES = {
    'bttv/badge/BadgeHostTest.java': r'''package bttv.badge;
import org.junit.Test;import static org.junit.Assert.*;
import java.util.*;import java.io.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
public class BadgeHostTest {
 static String bt(String author,int type){return "[{\"providerId\":\""+author+"\",\"badge\":{\"type\":"+type+",\"description\":\"Developer\",\"svg\":\"https://cdn.betterttv.net/badges/developer.svg\"}}]";}
 static String ff(){return "{\"badges\":[{\"id\":3,\"title\":\"Supporter\",\"color\":\"#755000\",\"replaces\":\"moderator\",\"urls\":{\"2\":\"https://cdn.frankerfacez.com/badge/3/2\"}},{\"id\":2,\"title\":\"Bot\",\"color\":null,\"urls\":{\"1\":\"https://cdn.frankerfacez.com/badge/2/1\"}}],\"users\":{\"2\":[123,123],\"3\":[123,456]}}";}
 interface Failure {void run() throws Exception;}
 static void rejects(Failure f) throws Exception {try {f.run();fail("accepted invalid input");}catch(AssertionError e){throw e;}catch(Exception expected){}}
 @Test public void identities() {
  for(String invalid:new String[]{null,"","0","000","-1","+1"," 1","1 ","1e3","1.0","１２","123456789012345678901"})assertNull(invalid,BadgeCatalog.authorId(invalid));
  assertEquals("123",BadgeCatalog.authorId("00123"));assertEquals("99999999999999999999",BadgeCatalog.authorId("99999999999999999999"));
 }
 @Test public void numericOwnershipAndNamesNeverUsed() throws Exception {
  BadgeCatalog.Snapshot b=BadgeCatalog.parse(BadgeCatalog.Provider.BTTV,bt("123",1));
  assertEquals(1,b.forAuthor("123").size());assertTrue(b.forAuthor("124").isEmpty());assertTrue(b.forAuthor("Developer").isEmpty());
  String spoof=bt("123",1).replace("\"badge\":","\"name\":\"different\",\"displayName\":\"same\",\"badge\":");
  assertEquals(b.forAuthor("123").get(0).key(),BadgeCatalog.parse(BadgeCatalog.Provider.BTTV,spoof).forAuthor("123").get(0).key());
 }
 @Test public void ffzDedupOrderAndBackground() throws Exception {
  BadgeCatalog.Snapshot f=BadgeCatalog.parse(BadgeCatalog.Provider.FFZ,ff());
  assertEquals(2,f.forAuthor("123").size());assertEquals("2",f.forAuthor("123").get(0).id);assertEquals("3",f.forAuthor("123").get(1).id);
  assertEquals(Integer.valueOf(0xff755000),f.definitions.get("3").background);assertEquals(1,f.forAuthor("456").size());
  assertEquals("FFZ:3",f.forAuthor("456").get(0).key());
 }
 @Test public void immutableSnapshotsAndNamespaceIsolation() throws Exception {
  BadgeCatalog.Snapshot b=BadgeCatalog.parse(BadgeCatalog.Provider.BTTV,bt("123",2));
  BadgeCatalog.Snapshot f=BadgeCatalog.parse(BadgeCatalog.Provider.FFZ,ff());
  assertNotEquals(b.forAuthor("123").get(0).key(),f.forAuthor("123").get(0).key());
  rejects(()->b.definitions.clear());rejects(()->f.forAuthor("123").clear());
  assertTrue(BadgeCatalog.parse(BadgeCatalog.Provider.BTTV,"[]").forAuthor("123").isEmpty());
 }
 @Test public void malformedAndBoundedMetadata() throws Exception {
  for(String value:new String[]{"", "{", "null",bt("0",1),bt("123",1).replace("https://","http://"),bt("123",1).replace("Developer","<script>")})
   rejects(()->BadgeCatalog.parse(BadgeCatalog.Provider.BTTV,value));
  rejects(()->BadgeCatalog.parse(BadgeCatalog.Provider.FFZ,ff().replace("#755000","transparent")));
  rejects(()->BadgeCatalog.parse(BadgeCatalog.Provider.FFZ,ff().replace("\"3\":[123,456]","\"9\":[123]")));
  char[] huge=new char[BadgeCatalog.MAX_METADATA+1];Arrays.fill(huge,' ');rejects(()->BadgeCatalog.parse(BadgeCatalog.Provider.BTTV,new String(huge)));
  rejects(()->BadgeCatalog.parse(BadgeCatalog.Provider.BTTV,"[[[[[[[[[[[[[[]]]]]]]]]]]]]]"));
  StringJoiner many=new StringJoiner(",","[","]");for(int i=1;i<=65;i++)many.add(bt("123",i).substring(1,bt("123",i).length()-1));
  rejects(()->BadgeCatalog.parse(BadgeCatalog.Provider.BTTV,many.toString()));
 }
 @Test public void exactAssetOrigins() throws Exception {
  assertEquals("https://cdn.frankerfacez.com/badge/3/2",BadgeCatalog.assetUrl(BadgeCatalog.Provider.FFZ,"//cdn.frankerfacez.com/badge/3/2"));
  for(String url:new String[]{"http://cdn.betterttv.net/badges/developer.svg","https://cdn.betterttv.net.evil/badges/developer.svg","https://x@cdn.betterttv.net/badges/developer.svg","https://cdn.betterttv.net:443/badges/developer.svg","https://cdn.betterttv.net/badges/../x.svg","https://cdn.betterttv.net/badges/developer.svg?x=1","file:///badge.svg","https://127.0.0.1/badges/developer.svg","https://cdn.betterttv.net/badges/%64eveloper.svg","https://cdn.betterttv.net/badges/developer.svg#x"})
   rejects(()->BadgeCatalog.assetUrl(BadgeCatalog.Provider.BTTV,url));
 }
 @Test public void currentProviderFixturesFit() throws Exception {
  String fixtures=System.getProperty("badge.fixtures");if(fixtures==null)return;
  for(BadgeCatalog.Provider p:BadgeCatalog.Provider.values()){
   byte[] bytes=Files.readAllBytes(Paths.get(fixtures,p==BadgeCatalog.Provider.BTTV?"bttv.json":p==BadgeCatalog.Provider.FFZ?"ffz.json":"chatterino.json"));
   assertTrue(bytes.length<BadgeCatalog.MAX_METADATA);BadgeCatalog.Snapshot s=BadgeCatalog.parse(p,new String(bytes,StandardCharsets.UTF_8));
   assertFalse(s.definitions.isEmpty());assertTrue(s.definitions.size()<=64);
  }
 }
}
''',
    'bttv/badge/BadgeAndroidTest.java': r'''package bttv.badge;
import org.junit.Test;import org.junit.runner.RunWith;import static org.junit.Assert.*;
import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;import org.robolectric.annotation.GraphicsMode;
import android.graphics.*;import android.graphics.drawable.*;import android.text.*;
import java.io.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=30,manifest=Config.NONE) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BadgeAndroidTest {
 static byte[] fixture(String name)throws Exception{return Files.readAllBytes(Paths.get(System.getProperty("badge.fixtures"),name));}
 static void rejects(String xml)throws Exception{BadgeHostTest.rejects(()->BadgeImages.decode(BadgeCatalog.Provider.BTTV,xml.getBytes(StandardCharsets.UTF_8),null));}
 static String svg(String body){return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 18 18\">"+body+"</svg>";}
 @Test public void actualAndroidSvgRasterization()throws Exception{
  for(String name:new String[]{"developer","translator","support_volunteer","emote_approver"}){
   Bitmap b=BadgeImages.decode(BadgeCatalog.Provider.BTTV,fixture("bttv-"+name+".svg"),null);
   assertFalse(b.isMutable());assertEquals(72,b.getWidth());int opaque=0;
   for(int y=0;y<72;y++)for(int x=0;x<72;x++)if(Color.alpha(b.getPixel(x,y))>0)opaque++;
   assertTrue("actual nonblank SVG raster",opaque>500);
   for(int size:new int[]{18,36,72}){
    Bitmap dest=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);BitmapDrawable d=new BitmapDrawable(b);d.setBounds(0,0,size,size);d.draw(new Canvas(dest));
    assertTrue(Color.alpha(dest.getPixel(size/2,size/2))>0);
   }
  }
 }
 @Test public void svgIsStrictAndAndroidParserRejectsUnsafeContent()throws Exception{
  for(String body:new String[]{"<script>alert(1)</script>","<image href=\"https://evil/x\"/>","<foreignObject/>","<use href=\"#x\"/>","<style>path{fill:url(x)}</style>","<path onclick=\"x\" d=\"M0 0L1 1\"/>","<path fill=\"url(http://evil)\" d=\"M0 0L1 1\"/>","<animate/>","<path d=\"M0 0L1 1\"/><!--x-->"})rejects(svg(body));
  rejects("<!DOCTYPE svg [<!ENTITY x SYSTEM 'file:///etc/passwd'>]>"+svg("<path d=\"M0 0L1 1\"/>"));
  rejects(svg("<path d=\"M0 0L1 1\"/>").replace("width=\"18\"","width=\"999\""));
  rejects("<svg>");rejects(svg(""));rejects(svg("<path d=\"M0 0L1 1\"/>")+"<svg/>");
  byte[] huge=new byte[BadgeImages.MAX_ASSET+1];BadgeHostTest.rejects(()->BadgeImages.decode(BadgeCatalog.Provider.BTTV,huge,null));
 }
 @Test public void actualPngAndBackgroundComposition()throws Exception{
  for(int id:new int[]{1,2,3,4}){
   Bitmap plain=BadgeImages.decode(BadgeCatalog.Provider.FFZ,fixture("ffz-"+id+".png"),null);
   Bitmap colored=BadgeImages.decode(BadgeCatalog.Provider.FFZ,fixture("ffz-"+id+".png"),0xff755000);
   assertFalse(colored.isMutable());assertEquals(72,colored.getHeight());assertEquals(72*72*4,colored.getAllocationByteCount());
   boolean found=false;for(int y=4;y<68;y++)for(int x=4;x<68;x++)if(Color.alpha(plain.getPixel(x,y))==0){assertEquals(0xff755000,colored.getPixel(x,y));found=true;}
   assertTrue("background visible through transparent PNG",found);
  }
  BadgeHostTest.rejects(()->BadgeImages.decode(BadgeCatalog.Provider.FFZ,new byte[34],null));
 }
 @Test public void transactionsOrderingExpiryAndAuthorIsolation()throws Exception{
  BadgeImages.Slot slot=new BadgeImages.Slot();long first=slot.begin();assertEquals(-1,slot.begin());
  BadgeCatalog.Snapshot bt=BadgeCatalog.parse(BadgeCatalog.Provider.BTTV,BadgeHostTest.bt("123",1));
  BadgeCatalog.Snapshot ff=BadgeCatalog.parse(BadgeCatalog.Provider.FFZ,BadgeHostTest.ff());
  Bitmap bitmap=BadgeImages.decode(BadgeCatalog.Provider.FFZ,fixture("ffz-3.png"),null);
  Map<String,Bitmap> bm=new HashMap<>();for(BadgeCatalog.Badge b:bt.definitions.values())bm.put(b.key(),bitmap);
  Map<String,Bitmap> fm=new HashMap<>();for(BadgeCatalog.Badge b:ff.definitions.values())fm.put(b.key(),bitmap);
  BadgeImages.Ready b=new BadgeImages.Ready(bt,bm,100),f=new BadgeImages.Ready(ff,fm,100);
  assertTrue(slot.finish(first,b));long second=slot.begin();assertFalse(slot.finish(first,f));assertSame(b,slot.ready);assertTrue(slot.finish(second,null));assertSame(b,slot.ready);
  List<BadgeImages.ReadyBadge> ready=BadgeImages.ready("123",b,f,100);assertEquals(3,ready.size());assertTrue(ready.get(0).title.startsWith("BTTV"));assertEquals("FFZ Bot",ready.get(1).title);
  assertTrue(BadgeImages.ready("999",b,f,100).isEmpty());assertTrue(BadgeImages.ready("123",b,f,100+BadgeImages.EXPIRES_MS+1).isEmpty());
  assertEquals(1,BadgeImages.ready("456",b,f,100).size());assertEquals(3,BadgeImages.ready("123",b,f,100).size());
  bm.clear();fm.clear();assertEquals(3,BadgeImages.ready("123",b,f,100).size());
  BadgeImages.Ready missing=new BadgeImages.Ready(bt,Collections.emptyMap(),100);assertTrue(BadgeImages.ready("123",missing,f,100).isEmpty());
 }
 @Test public void boundedFailureAndRetryPolicy()throws Exception{
  for(int status:new int[]{301,302,403,404})assertEquals(-1,BadgeImages.retryDelay(new BadgeImages.HttpFailure(status,0),0));
  assertEquals(1000,BadgeImages.retryDelay(new BadgeImages.HttpFailure(500,0),0));assertEquals(4000,BadgeImages.retryDelay(new IOException(),1));
  assertEquals(9000,BadgeImages.retryDelay(new BadgeImages.HttpFailure(429,9000),0));assertEquals(-1,BadgeImages.retryDelay(new BadgeImages.HttpFailure(429,61000),0));assertEquals(-1,BadgeImages.retryDelay(new IOException(),2));
  BadgeImages.Slot s=new BadgeImages.Slot();final int[] requests={0};BadgeImages.load(BadgeCatalog.Provider.BTTV,s,(u,l,m)->{requests[0]++;throw new BadgeImages.HttpFailure(404,0);});assertEquals(1,requests[0]);assertNull(s.ready);
  BadgeHostTest.rejects(()->BadgeImages.fetch("https://evil/metadata",20,"application/json"));
 }
 @Test public void concurrentCompletionAndNoPerMessageRequests()throws Exception{
  BadgeImages.Slot a=new BadgeImages.Slot(),b=new BadgeImages.Slot();CountDownLatch entered=new CountDownLatch(2),release=new CountDownLatch(1);
  java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
  BadgeImages.Fetcher fetch=(u,l,m)->{calls.incrementAndGet();entered.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){throw new IOException(e);}return new BadgeImages.Response((u.contains("betterttv")?"[]":"{\"badges\":[],\"users\":{}}").getBytes(StandardCharsets.UTF_8),"application/json");};
  Thread ta=new Thread(()->BadgeImages.load(BadgeCatalog.Provider.BTTV,a,fetch)),tb=new Thread(()->BadgeImages.load(BadgeCatalog.Provider.FFZ,b,fetch));ta.start();tb.start();assertTrue(entered.await(3,TimeUnit.SECONDS));
  assertEquals(-1,a.begin());assertEquals(-1,b.begin());release.countDown();ta.join(3000);tb.join(3000);assertFalse(ta.isAlive());assertFalse(tb.isAlive());assertNotNull(a.ready);assertNotNull(b.ready);
  for(int i=0;i<10000;i++)assertTrue(BadgeImages.ready("123",a.ready,b.ready,android.os.SystemClock.elapsedRealtime()).isEmpty());assertEquals(2,calls.get());
  BadgeImages.Ready good=a.ready;BadgeImages.load(BadgeCatalog.Provider.BTTV,a,(u,l,m)->new BadgeImages.Response("{".getBytes(StandardCharsets.UTF_8),"application/json"));assertSame(good,a.ready);
 }
 @Test public void failedAssetProducesNoBlankBadge()throws Exception{
  BadgeImages.Slot slot=new BadgeImages.Slot();final int[] n={0};
  BadgeImages.load(BadgeCatalog.Provider.BTTV,slot,(u,l,m)->{n[0]++;if(n[0]>1)throw new BadgeImages.HttpFailure(404,0);return new BadgeImages.Response(BadgeHostTest.bt("123",1).getBytes(StandardCharsets.UTF_8),"application/json");});
  assertNotNull(slot.ready);assertTrue(BadgeImages.ready("123",slot.ready,null,android.os.SystemClock.elapsedRealtime()).isEmpty());assertEquals(2,n[0]);
 }
 @Test public void actualAndroidSpanCopyAndFreshDrawables()throws Exception{
  Bitmap bitmap=BadgeImages.decode(BadgeCatalog.Provider.FFZ,fixture("ffz-3.png"),0xff755000);
  Object nativeBadge=new Object(),click=new Object(),style=new Object();SpannableStringBuilder n=new SpannableStringBuilder("mod ");n.setSpan(nativeBadge,0,3,17);n.setSpan(click,0,3,17);n.setSpan(style,0,3,17);
  SpannedString original=new SpannedString(n);SpannableStringBuilder result=new SpannableStringBuilder(original);int start=result.length();result.append("FFZ Supporter");int end=result.length();result.append(" ");
  BitmapDrawable d1=new BitmapDrawable(bitmap),d2=new BitmapDrawable(bitmap);d1.setBounds(0,0,18,18);d2.setBounds(0,0,36,36);assertNotSame(d1,d2);assertEquals(18,d1.getBounds().width());
  result.setSpan(new android.text.style.ImageSpan(d1),start,end,17);SpannedString out=new SpannedString(result);
  assertEquals("mod ",original.toString());assertEquals(0,out.getSpanStart(nativeBadge));assertEquals(3,out.getSpanEnd(click));assertEquals(17,out.getSpanFlags(style));assertEquals(' ',out.charAt(end));assertEquals(start,out.getSpanStart(out.getSpans(start,end,android.text.style.ImageSpan.class)[0]));
 }
}
''',
}

def generate(destination):
    destination = Path(destination)
    if destination.exists():
        raise ValueError('Refusing to overwrite existing badge test fixtures')
    for name, text in SOURCES.items():
        path = destination / 'src/test/java' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding='utf-8')
    for name in ('BadgeCatalog', 'BadgeImages', 'ChatterinoHttp'):
        source = ROOT / 'mod/app/src/main/java/bttv/badge' / (name + '.java')
        target = destination / 'src/main/java/bttv/badge' / source.name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)

if __name__ == '__main__':
    generate(sys.argv[1])
