"""Stage I scope checks and fixtures exercising actual catalog/image production code."""
from pathlib import Path
import importlib.util
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
HOST = r'''package bttv.badge;
import org.junit.Test;import static org.junit.Assert.*;
import java.nio.file.*;import java.util.*;
public class ChatterinoHostTest {
 static String item(String family,String users){return "{\"tooltip\":\"Chatterino "+family+"\",\"image2\":\"https://fourtf.com/chatterino/badges/"+family+"2x.png\",\"image1\":\"https://fourtf.com/chatterino/badges/"+family+".png\",\"users\":"+users+"}";}
 static BadgeCatalog.Snapshot parse(String items)throws Exception{return BadgeCatalog.parse(BadgeCatalog.Provider.CHATTERINO,"{\"badges\":["+items+"]}");}
 @Test public void capturedSchemaFiveDefinitions()throws Exception{
  String json=Files.readString(Paths.get(System.getProperty("badge.fixtures"),"chatterino.json"));
  BadgeCatalog.Snapshot s=BadgeCatalog.parse(BadgeCatalog.Provider.CHATTERINO,json);
  assertEquals(new HashSet<>(Arrays.asList("dev","helper","supporter","topd","pepe")),s.definitions.keySet());
  for(BadgeCatalog.Badge b:s.definitions.values()){assertTrue(b.url.endsWith("2x.png"));assertNotNull(b.fallbackUrl);assertTrue(b.key().startsWith("CHATTERINO:"));}
 }
 @Test public void numericOnlyDedupMultipleAndOrdering()throws Exception{
  BadgeCatalog.Snapshot s=parse(item("helper","[\"123\",\"00123\",\"name\",\"0\",null,\"1.2\",\"-5\"]")+","+item("dev","[123,123,456]")+","+item("dev","[123]"));
  assertEquals(2,s.forAuthor("123").size());assertEquals("dev",s.forAuthor("123").get(0).id);assertEquals("helper",s.forAuthor("123").get(1).id);
  assertEquals(1,s.forAuthor("456").size());assertTrue(s.forAuthor("name").isEmpty());assertTrue(s.forAuthor("999").isEmpty());
  BadgeHostTest.rejects(()->s.definitions.clear());BadgeHostTest.rejects(()->s.forAuthor("123").clear());
 }
 @Test public void independentFallbackAndConflicts()throws Exception{
  String one=item("dev","[123]");
  BadgeCatalog.Badge b=parse(one.replace("https://fourtf.com/chatterino/badges/dev2x.png","https://evil/dev2x.png")).definitions.get("dev");
  assertEquals("https://fourtf.com/chatterino/badges/dev.png",b.url);assertNull(b.fallbackUrl);
  assertNull(parse(one.replace("https://fourtf.com/chatterino/badges/dev.png","https://evil/dev.png")).definitions.get("dev").fallbackUrl);
  BadgeHostTest.rejects(()->parse(one+","+one.replace("Chatterino dev","different")));
  BadgeHostTest.rejects(()->parse(one.replace("/dev.png","/helper.png")));
  assertTrue(parse(one.replace("https://fourtf.com/","http://fourtf.com/")).definitions.isEmpty());
 }
 @Test public void strictPrivateAssetPolicy()throws Exception{
  for(String bad:new String[]{"https://fourtf.com/chatterino/badges/other.png","https://fourtf.com/chatterino/badges/dev2x.png?q=1","https://x@fourtf.com/chatterino/badges/dev.png","https://fourtf.com:443/chatterino/badges/dev.png","//fourtf.com/chatterino/badges/dev.png","https://fourtf.com/chatterino/badges/%64ev.png","https://fourtf.com/chatterino/badges/../dev.png"})
   BadgeHostTest.rejects(()->BadgeCatalog.assetUrl(BadgeCatalog.Provider.CHATTERINO,bad));
  assertTrue(parse("").definitions.isEmpty());
 }
}
'''
ANDROID = r'''package bttv.badge;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;
import org.robolectric.*;import org.robolectric.annotation.*;
import android.graphics.*;import android.os.SystemClock;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.io.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=30,manifest=Config.NONE) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ChatterinoAndroidTest {
 final List<BadgeImages.Lazy> instances=new ArrayList<>();
 BadgeImages.Lazy lazy(BadgeImages.Fetcher f){BadgeImages.Lazy l=new BadgeImages.Lazy(f);instances.add(l);return l;}
 @After public void cleanup()throws Exception{for(BadgeImages.Lazy l:instances){java.lang.reflect.Field f=l.getClass().getDeclaredField("workers");f.setAccessible(true);ThreadPoolExecutor e=(ThreadPoolExecutor)f.get(l);e.shutdownNow();assertTrue(e.awaitTermination(3,TimeUnit.SECONDS));}}
 byte[] png()throws Exception{return Files.readAllBytes(Paths.get(System.getProperty("badge.fixtures"),"chatterino-topd.png"));}
 byte[] json(String items){return ("{\"badges\":["+items+"]}").getBytes(StandardCharsets.UTF_8);}
 List<BadgeImages.ReadyBadge> lookup(BadgeImages.Lazy l,String author){return l.append(author,Collections.emptyList(),SystemClock.elapsedRealtime());}
 List<BadgeImages.ReadyBadge> await(BadgeImages.Lazy l,int count)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);List<BadgeImages.ReadyBadge> v;do{v=lookup(l,"123");if(v.size()==count)return v;Thread.sleep(5);}while(System.nanoTime()<end);throw new AssertionError("publication not observed");}
 @Test public void tallContainAndPngLimits()throws Exception{
  Bitmap b=BadgeImages.decode(BadgeCatalog.Provider.CHATTERINO,png(),null);assertFalse(b.isMutable());assertEquals(72,b.getWidth());
  for(int y=0;y<72;y++){assertEquals(0,Color.alpha(b.getPixel(0,y)));assertEquals(0,Color.alpha(b.getPixel(71,y)));}
  int count=0;for(int y=0;y<72;y++)for(int x=0;x<72;x++)if(Color.alpha(b.getPixel(x,y))>0)count++;assertTrue(count>100);
  BadgeHostTest.rejects(()->BadgeImages.decode(BadgeCatalog.Provider.CHATTERINO,new byte[0],null));
  BadgeHostTest.rejects(()->BadgeImages.decode(BadgeCatalog.Provider.CHATTERINO,new byte[BadgeImages.MAX_ASSET+1],null));
  byte[] truncated=Arrays.copyOf(png(),png().length-3);BadgeHostTest.rejects(()->BadgeImages.decode(BadgeCatalog.Provider.CHATTERINO,truncated,null));
  Bitmap huge=Bitmap.createBitmap(257,2,Bitmap.Config.ARGB_8888);ByteArrayOutputStream out=new ByteArrayOutputStream();huge.compress(Bitmap.CompressFormat.PNG,100,out);
  BadgeHostTest.rejects(()->BadgeImages.decode(BadgeCatalog.Provider.CHATTERINO,out.toByteArray(),null));
 }
 @Test public void lazyDefinitionDedupNoRowNetworkOrViews()throws Exception{
  byte[] p=png();AtomicInteger metadata=new AtomicInteger(),assets=new AtomicInteger();CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);Thread caller=Thread.currentThread();
  BadgeImages.Lazy l=lazy((u,n,m)->{if(m.equals("application/json")){metadata.incrementAndGet();return new BadgeImages.Response(json(ChatterinoHostTest.item("dev","[123,456]")),m);}assertNotSame(caller,Thread.currentThread());assets.incrementAndGet();started.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){throw new IOException(e);}return new BadgeImages.Response(p,m);});
  l.load();assertEquals(1,metadata.get());assertEquals(0,assets.get());assertTrue(lookup(l,"999").isEmpty());assertEquals(0,assets.get());
  assertTrue(lookup(l,"123").isEmpty());assertTrue(started.await(3,TimeUnit.SECONDS));for(int i=0;i<10000;i++){assertTrue(lookup(l,i%2==0?"123":"456").isEmpty());}assertEquals(1,assets.get());
  release.countDown();assertEquals(1,await(l,1).size());assertEquals(1,lookup(l,"456").size());assertEquals(1,assets.get());assertEquals(1,metadata.get());
  assertTrue(l.append("123",Collections.emptyList(),SystemClock.elapsedRealtime()+BadgeImages.EXPIRES_MS+1).isEmpty());
 }
 @Test public void fallbackFailureIsolationAndCap()throws Exception{
  byte[] p=png();AtomicInteger assets=new AtomicInteger();BadgeImages.Lazy l=lazy((u,n,m)->{if(m.equals("application/json"))return new BadgeImages.Response(json(ChatterinoHostTest.item("dev","[123]")),m);assets.incrementAndGet();if(u.endsWith("2x.png"))throw new IOException();return new BadgeImages.Response(p,m);});
  l.load();await(l,1);assertEquals(2,assets.get());
  List<BadgeImages.ReadyBadge> old=new ArrayList<>();for(int i=0;i<8;i++)old.add(new BadgeImages.ReadyBadge("existing"+i,null));assertSame(old,l.append("123",old,SystemClock.elapsedRealtime()));
  old.remove(7);List<BadgeImages.ReadyBadge> all=l.append("123",old,SystemClock.elapsedRealtime());assertEquals(8,all.size());for(int i=0;i<7;i++)assertSame(old.get(i),all.get(i));assertEquals("Chatterino dev",all.get(7).title);
  BadgeImages.Lazy fail=lazy((u,n,m)->{throw new IOException();});fail.load();assertSame(old,fail.append("123",old,SystemClock.elapsedRealtime()));
 }
 @Test public void staleMetadataAndImageCannotPublish()throws Exception{
  byte[] p=png();AtomicInteger loads=new AtomicInteger(),assets=new AtomicInteger();CountDownLatch assetIn=new CountDownLatch(1),assetOut=new CountDownLatch(1),oldIn=new CountDownLatch(1),oldOut=new CountDownLatch(1);
  BadgeImages.Lazy l=lazy((u,n,m)->{
   if(m.equals("application/json")){int g=loads.incrementAndGet();if(g==3){oldIn.countDown();try{oldOut.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){throw new IOException(e);}}return new BadgeImages.Response(json(ChatterinoHostTest.item(g==1?"dev":"helper",g==1?"[123]":g==3?"[123]":"[456]")),m);}
   assets.incrementAndGet();assetIn.countDown();try{assetOut.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){throw new IOException(e);}return new BadgeImages.Response(p,m);});
  l.load();lookup(l,"123");assertTrue(assetIn.await(3,TimeUnit.SECONDS));l.load();assetOut.countDown();assertTrue(lookup(l,"123").isEmpty());
  Thread old=new Thread(l::load);old.start();assertTrue(oldIn.await(3,TimeUnit.SECONDS));l.load();oldOut.countDown();old.join(3000);assertFalse(old.isAlive());assertTrue(lookup(l,"123").isEmpty());
  java.lang.reflect.Field workers=l.getClass().getDeclaredField("workers");workers.setAccessible(true);ThreadPoolExecutor e=(ThreadPoolExecutor)workers.get(l);assertEquals(2,e.getMaximumPoolSize());assertTrue(e.allowsCoreThreadTimeOut());assertTrue(e.getQueue().remainingCapacity()<=64);
 }
 @Test public void failedAssetOnceAndMalformedCatalogRetainsGood()throws Exception{
  AtomicInteger loads=new AtomicInteger(),assets=new AtomicInteger();CountDownLatch attempted=new CountDownLatch(2);
  BadgeImages.Lazy l=lazy((u,n,m)->{if(m.equals("application/json"))return new BadgeImages.Response(loads.incrementAndGet()==1?json(ChatterinoHostTest.item("dev","[123]")):new byte[]{123},m);assets.incrementAndGet();attempted.countDown();throw new IOException();});
  l.load();lookup(l,"123");assertTrue(attempted.await(3,TimeUnit.SECONDS));l.load();for(int i=0;i<10000;i++)assertTrue(lookup(l,"123").isEmpty());assertEquals(2,assets.get());
 }
}
'''


def static_checks():
    for name in ('Data.java',):
        path='mod/app/src/main/java/bttv/'+name
        assert (ROOT/path).read_bytes() == subprocess.check_output(['git','show','HEAD:'+path],cwd=ROOT)
    for p in (ROOT/'patches').rglob('*.patch'):
        if 'notifications' in p.as_posix(): continue
        rel=p.relative_to(ROOT).as_posix()
        assert p.read_bytes()==subprocess.check_output(['git','show','HEAD:'+rel],cwd=ROOT),rel
    source=(ROOT/'mod/app/src/main/java/bttv/badge/BadgeImages.java').read_text()
    assert 'List<ReadyBadge> existing = ready(author, BTTV.ready, FFZ.ready, now)' in source
    assert 'return existing;' in source and 'current.generation != expected.generation' in source
    assert 'bttv-chatterino-image' in source and 'allowCoreThreadTimeOut(true)' in source
    assert 'Math.min((float) RASTER_SIZE / raster.getWidth()' in source
    original=subprocess.check_output(['git','show','HEAD:mod/app/src/main/java/bttv/badge/BadgeImages.java'],cwd=ROOT,text=True)
    def method(text, signature):
        start=text.index(signature);opening=text.index('{',start);depth=1;end=opening+1
        while depth:
            if text[end]=='{': depth+=1
            elif text[end]=='}': depth-=1
            end+=1
        return text[start:end]
    for signature in ('static void load(', 'static long retryDelay(', 'static List<ReadyBadge> ready(String author,',
                      'static Response fetch(', 'static String utf8(', 'static String safeSvg(', 'static void validatePng('):
        assert method(source,signature)==method(original,signature),signature
    build=(ROOT/'buildsource').read_text()
    added=build[build.index('# Export only exact, hash-validated private resources'):build.index('# copy contributors')]
    old_build=subprocess.check_output(['git','show','HEAD:buildsource'],cwd=ROOT,text=True)
    assert build.replace(added,'',1)==old_build
    print('PASS Stage I scope/native hooks, old-result-first, lazy/generation/contain guards')


def generate(destination):
    spec=importlib.util.spec_from_file_location('badge_fixtures',ROOT/'tests/stage5q_badges_host.py')
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    module.generate(destination)
    for name,code in [('ChatterinoHostTest',HOST),('ChatterinoAndroidTest',ANDROID)]:
        p=Path(destination)/'src/test/java/bttv/badge'/ (name+'.java')
        p.write_text(code,encoding='utf-8')


if __name__ == '__main__':
    static_checks()
    if len(sys.argv)>1: generate(sys.argv[1])
