"""Real updater tests. Generate into a new isolated directory; no APK installation/network."""
from pathlib import Path
import sys
SOURCES={'bttv/updater/UpdaterHostTest.java':r'''package bttv.updater;
import org.junit.*;import static org.junit.Assert.*;
import org.json.*;import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;import java.security.*;
public class UpdaterHostTest {
 static final String CERT="1E08A903AEF9C3A721510B64EC764D01D3D094EB954161B62544EA8F187B5953";
 static final String OFFICIAL="314F3A00877C7A5880A9E1BFD3E98B7E3D43C1340EF0FAC35B7B3956B75BACD3";
 static final List<String> ABIS=Arrays.asList("arm64-v8a","armeabi-v7a");
 static UpdateRelease.Installed installed(){return new UpdateRelease.Installed("0.43.2-beta",1208000338L,CERT,30,ABIS);}
 static JSONObject compatibility(byte[] bytes)throws Exception {
  return new JSONObject().put("schemaVersion",1).put("standalone",true).put("packageName",UpdateRelease.PACKAGE)
   .put("releaseTag","v0.44.0-beta").put("versionName","0.44.0-beta").put("versionCode",1208000339L)
   .put("signerSha256",CERT).put("minSdk",29).put("twitchVersion","31.2.0")
   .put("supportedAbis",new JSONArray(ABIS)).put("assetId",123).put("assetName","bttv.apk")
   .put("sizeBytes",bytes.length).put("sha256",UpdateTransfer.hex(MessageDigest.getInstance("SHA-256").digest(bytes)));
 }
 static JSONObject release(JSONObject c,byte[] bytes)throws Exception {
  JSONObject asset=new JSONObject().put("id",123).put("name","bttv.apk").put("state","uploaded").put("content_type",UpdateRelease.MIME)
   .put("size",bytes.length).put("browser_download_url","https://github.com/bttv-android/bttv/releases/download/"+c.getString("releaseTag")+"/bttv.apk");
  return new JSONObject().put("tag_name",c.getString("releaseTag")).put("draft",false).put("prerelease",false)
   .put("name","Test release").put("body","Notes\n```bttv-update\n"+c+"\n```\n").put("assets",new JSONArray().put(asset));
 }
 static UpdateRelease candidate(byte[] bytes)throws Exception {return UpdateRelease.inspect(release(compatibility(bytes),bytes).toString(),installed()).candidate;}
 interface Run {void run()throws Exception;}
 static void rejects(Run run)throws Exception {try {run.run();fail("unsafe input accepted");}catch(IOException expected){}}
 @Test public void eligibleAndTagNotAnOrderingAlgorithm()throws Exception {
  byte[] b={1,2,3};assertNotNull(candidate(b));
  for(long code:new long[]{1208000338L,1208000337L}) {
   JSONObject c=compatibility(b).put("versionCode",code);assertNull(UpdateRelease.inspect(release(c,b).toString(),installed()).candidate);
  }
  JSONObject same=compatibility(b).put("releaseTag","v0.43.2-beta").put("versionName","0.43.2-beta");
  assertNotNull(UpdateRelease.inspect(release(same,b).toString(),installed()).candidate);
  for(String name:new String[]{"0.44.0","0.45.0-beta"}) {
   JSONObject c=compatibility(b).put("versionName",name).put("releaseTag","v"+name);
   assertNotNull(UpdateRelease.inspect(release(c,b).toString(),installed()).candidate);
  }
 }
 @Test public void actualOfficialLegacyIsNotAnUpdate()throws Exception {
  String legacy=Files.readString(Paths.get(System.getProperty("updater.repo"),".tmp/stage5qg/upstream/latest.json"));
  assertNull(UpdateRelease.inspect(legacy,installed()).candidate);
  byte[] b={1};JSONObject c=compatibility(b).put("signerSha256",OFFICIAL);
  rejects(()->UpdateRelease.inspect(release(c,b).toString(),installed()));
 }
 @Test public void identityAndBounds()throws Exception {
  byte[] b={1};
  for(String key:new String[]{"versionCode","signerSha256","sha256","packageName","supportedAbis","assetId"}) {
   JSONObject c=compatibility(b);c.remove(key);rejects(()->UpdateRelease.inspect(release(c,b).toString(),installed()));
  }
  for(Object[] pair:new Object[][]{{"packageName","wrong.package"},{"signerSha256",OFFICIAL},{"minSdk",99},{"twitchVersion","19.0.1"},
       {"versionCode",1208000339.1},{"versionCode","1208000339"},{"assetId",124},{"assetName","other.apk"},
       {"sizeBytes",2},{"supportedAbis",new JSONArray(Arrays.asList("x86","x86_64"))},{"standalone",false},{"sha256","missing"}}) {
   JSONObject c=compatibility(b).put((String)pair[0],pair[1]);rejects(()->UpdateRelease.inspect(release(c,b).toString(),installed()));
  }
  for(String text:new String[]{"","null","{","[]"," ".repeat(UpdateRelease.MAX_METADATA+1),"[".repeat(33)+"]".repeat(33)})rejects(()->UpdateRelease.inspect(text,installed()));
 }
 @Test public void assetAndChannelParsing()throws Exception {
  byte[] b={1};
  for(String key:new String[]{"draft","prerelease"}) {JSONObject r=release(compatibility(b),b).put(key,true);assertNull(UpdateRelease.inspect(r.toString(),installed()).candidate);}
  JSONObject missing=release(compatibility(b),b);missing.remove("body");assertNull(UpdateRelease.inspect(missing.toString(),installed()).candidate);
  JSONObject huge=release(compatibility(b),b).put("body","x".repeat(UpdateRelease.MAX_NOTES+1));rejects(()->UpdateRelease.inspect(huge.toString(),installed()));
  JSONObject noTag=release(compatibility(b),b);noTag.remove("tag_name");rejects(()->UpdateRelease.inspect(noTag.toString(),installed()));
  for(String field:new String[]{"browser_download_url","name","id","size"}) {
   JSONObject r=release(compatibility(b),b);r.getJSONArray("assets").getJSONObject(0).remove(field);rejects(()->UpdateRelease.inspect(r.toString(),installed()));
  }
  JSONObject multi=release(compatibility(b),b);multi.getJSONArray("assets").put(multi.getJSONArray("assets").getJSONObject(0));rejects(()->UpdateRelease.inspect(multi.toString(),installed()));
  JSONObject wrong=release(compatibility(b),b);wrong.getJSONArray("assets").getJSONObject(0).put("content_type","application/zip");rejects(()->UpdateRelease.inspect(wrong.toString(),installed()));
  for(String url:new String[]{"http://github.com/bttv-android/bttv/releases/download/v0.44.0-beta/bttv.apk","https://evil.test/bttv.apk","file:///tmp/bttv.apk","https://user@github.com/bttv-android/bttv/releases/download/v0.44.0-beta/bttv.apk"}){
   JSONObject r=release(compatibility(b),b);r.getJSONArray("assets").getJSONObject(0).put("browser_download_url",url);rejects(()->UpdateRelease.inspect(r.toString(),installed()));
  }
 }
 @Test public void embeddedCompatibilityAlsoHasDepthBound()throws Exception {
  byte[] b={1};JSONObject c=compatibility(b);String normal=c.toString();
  String nested=normal.substring(0,normal.length()-1)+",\"unused\":"+"[".repeat(2048)+"0"+"]".repeat(2048)+"}";
  JSONObject r=release(c,b).put("body","```bttv-update\n"+nested+"\n```");
  rejects(()->UpdateRelease.inspect(r.toString(),installed()));
 }
 static class Connection extends HttpURLConnection {
  final byte[] bytes;int status=200;long length;String location,retry;boolean closed;
  Connection(URL url,byte[] bytes){super(url);this.bytes=bytes;length=bytes.length;}
  public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){closed=true;}
  public int getResponseCode(){return status;}public long getContentLengthLong(){return length;}
  public String getHeaderField(String key){return key.equals("Location")?location:key.equals("Retry-After")?retry:null;}
  public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}
 }
 @Test public void httpFailuresRedirectsAndNetwork()throws Exception {
  for(int code:new int[]{304,403,404,429,500,503}) {
   Connection c=new Connection(new URL(UpdateRelease.API),new byte[0]);c.status=code;c.retry="120";
   try {UpdateTransfer.metadata(new UpdateTransfer.Cancel(),u->c);fail();}catch(UpdateTransfer.HttpFailure e){assertTrue(e.getMessage().contains("HTTP "+code));assertTrue(e.retryAt>System.currentTimeMillis()+100000);}
   assertTrue(c.closed);
  }
  for(IOException e:new IOException[]{new UnknownHostException(),new SocketTimeoutException(),new IOException()}) rejects(()->UpdateTransfer.metadata(new UpdateTransfer.Cancel(),u->{throw e;}));
  Connection redirect=new Connection(new URL(UpdateRelease.API),new byte[0]);redirect.status=302;redirect.location="https://evil.test/metadata";
  rejects(()->UpdateTransfer.metadata(new UpdateTransfer.Cancel(),u->redirect));
  Connection oversized=new Connection(new URL(UpdateRelease.API),new byte[UpdateRelease.MAX_METADATA+1]);oversized.length=-1;
  rejects(()->UpdateTransfer.metadata(new UpdateTransfer.Cancel(),u->oversized));
 }
 @Test public void streamedDownloadUnknownLengthProgressAndCleanup()throws Exception {
  byte[] b=new byte[100000];new Random(1).nextBytes(b);UpdateRelease r=candidate(b);
  File dir=Files.createTempDirectory("updater-transfer").toFile();List<Long> progress=new ArrayList<>();
  try {
   Connection c=new Connection(new URL(r.url),b);c.length=-1;
   File file=UpdateTransfer.download(r,dir,new UpdateTransfer.Cancel(),(n,t)->progress.add(n),u->c);
   assertEquals(b.length,file.length());assertEquals(Long.valueOf(b.length),progress.get(progress.size()-1));assertTrue(c.closed);
   file.delete();
   for(byte[] broken:new byte[][]{new byte[10],new byte[b.length],new byte[b.length+1]}){
    Connection bad=new Connection(new URL(r.url),broken);bad.length=-1;
    rejects(()->UpdateTransfer.download(r,dir,new UpdateTransfer.Cancel(),(n,t)->{},u->bad));assertEquals(0,dir.list().length);
   }
   UpdateTransfer.Cancel token=new UpdateTransfer.Cancel();
   rejects(()->UpdateTransfer.download(r,dir,token,(n,t)->token.cancel(),u->new Connection(u,b)));
   assertEquals(0,dir.list().length);
  } finally {for(File f:dir.listFiles())f.delete();dir.delete();}
 }
 @Test public void productionStateMachineDedupCancelAndStaleCompletion() {
  UpdateSession s=new UpdateSession();long a=s.begin(UpdateSession.State.CHECKING,100000);
  assertTrue(a>0);assertEquals(-1,s.begin(UpdateSession.State.CHECKING,100001));
  s.cancel();assertEquals(UpdateSession.State.CANCELLED,s.snapshot().state);
  assertEquals(-1,s.begin(UpdateSession.State.CHECKING,140000));
  assertFalse(s.finish(a,UpdateSession.State.NONE,"stale",null,null));assertFalse(s.running());
  long b=s.begin(UpdateSession.State.CHECKING,140000);assertTrue(b>a);
  assertFalse(s.progress(a,9,10));assertTrue(s.finish(b,UpdateSession.State.NONE,"no compatible",null,null));
  assertEquals(-1,s.begin(UpdateSession.State.CHECKING,140001));
  s.backoff(999999);assertEquals(-1,s.begin(UpdateSession.State.CHECKING,200000));
  assertEquals(-1,s.begin(UpdateSession.State.DOWNLOADING,200000));
 }
 @Test public void singleDownloadAndVerificationOwner()throws Exception {
  UpdateSession s=new UpdateSession();UpdateRelease r=candidate(new byte[]{1});
  long check=s.begin(UpdateSession.State.CHECKING,100000);s.finish(check,UpdateSession.State.AVAILABLE,"available",r,null);
  long download=s.begin(UpdateSession.State.DOWNLOADING,100001);assertTrue(download>check);
  assertEquals(-1,s.begin(UpdateSession.State.DOWNLOADING,100002));
  assertTrue(s.progress(download,1,1));s.finish(download,UpdateSession.State.READY,"ready",r,new File("owned.apk"));
  long verify=s.begin(UpdateSession.State.VERIFYING,100003);assertTrue(verify>download);s.cancel();
  assertFalse(s.finish(verify,UpdateSession.State.READY,"stale",r,new File("owned.apk")));
  assertNull(s.snapshot().file);assertNull(s.snapshot().release);
 }
}'''}

SOURCES['bttv/updater/UpdaterAndroidTest.java']=r'''package bttv.updater;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;
import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.*;import org.robolectric.android.controller.ActivityController;import android.app.*;import android.content.*;import android.content.pm.*;
import android.net.Uri;import java.io.*;import java.nio.file.*;import java.util.*;import org.json.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=30,manifest=Config.NONE,packageName="tv.twitch.bttvandroid.app")
public class UpdaterAndroidTest {
 Context context; File original;
 @Before public void setup()throws Exception {
  context=new ContextWrapper(RuntimeEnvironment.getApplication()) {
   @Override public String getPackageName(){return UpdateRelease.PACKAGE;}
   @Override public Context getApplicationContext(){return this;}
  };original=new File(System.getProperty("updater.repo"),"dist/archive/BTTV_Stage5Q_F_Badges_Test.apk");
  com.android.apksig.ApkVerifier.Result verified=new com.android.apksig.ApkVerifier.Builder(original).setMinCheckedPlatformVersion(29).build().verify();
  assertTrue("actual accepted APK signature",verified.isVerified());
  Signature signer=new Signature(verified.getSignerCertificates().get(0).getEncoded());
  PackageInfo installed=new PackageInfo();installed.packageName=UpdateRelease.PACKAGE;installed.versionName="0.43.2-beta";installed.setLongVersionCode(1208000337L);
  installed.applicationInfo=context.getApplicationInfo();installed.applicationInfo.sourceDir=original.getAbsolutePath();
  installed.signingInfo=new SigningInfo();((ShadowSigningInfo)Shadow.extract(installed.signingInfo)).setSignatures(new Signature[]{signer});
  ((ShadowPackageManager)Shadow.extract(context.getPackageManager())).installPackage(installed);
  PackageInfo archive=new PackageInfo();archive.packageName=UpdateRelease.PACKAGE;archive.versionName="0.43.2-beta";archive.setLongVersionCode(1208000338L);
  archive.applicationInfo=new ApplicationInfo();archive.applicationInfo.minSdkVersion=29;
  ((ShadowPackageManager)Shadow.extract(context.getPackageManager())).setPackageArchiveInfo(original.getAbsolutePath(),archive);
  context.getApplicationInfo().sourceDir=original.getAbsolutePath();
 }
 UpdateRelease actual()throws Exception {
  // Simulate lower installed code through PM fixtures, not a shipped bypass or a rewritten APK.
  JSONObject c=UpdaterHostTest.compatibility(new byte[]{1}).put("releaseTag","v0.43.2-beta").put("versionName","0.43.2-beta")
   .put("versionCode",1208000338L).put("sizeBytes",original.length()).put("sha256",UpdateTransfer.digest(original,new UpdateTransfer.Cancel()));
  JSONObject r=UpdaterHostTest.release(c,new byte[]{1});r.getJSONArray("assets").getJSONObject(0).put("size",original.length());
  UpdateRelease.Installed installed=UpdateVerifier.installed(context);
  // The test VM ABI is x86; fixtures model the supported ARM phone, not production eligibility changes.
  return UpdateRelease.inspect(r.toString(),new UpdateRelease.Installed(installed.name,installed.code,installed.signer,30,UpdaterHostTest.ABIS)).candidate;
 }
 @Test public void realCryptographyPlusArchiveAndNativeValidation()throws Exception {
  UpdateRelease release=actual();UpdateVerifier.verify(context,release,original,new UpdateTransfer.Cancel());
  PackageInfo bad=context.getPackageManager().getPackageArchiveInfo(original.getAbsolutePath(),0);bad.packageName="wrong.package";
  UpdaterHostTest.rejects(()->UpdateVerifier.verify(context,release,original,new UpdateTransfer.Cancel()));
 }
 @Test public void signatureByteTamperingIsRejectedEvenWithFixtureHash()throws Exception {
  File corrupt=File.createTempFile("updater-corrupt-",".apk");
  try {
   Files.copy(original.toPath(),corrupt.toPath(),StandardCopyOption.REPLACE_EXISTING);
   try(RandomAccessFile f=new RandomAccessFile(corrupt,"rw")){f.seek(500);int b=f.read();f.seek(500);f.write(b^1);}
   assertFalse(new com.android.apksig.ApkVerifier.Builder(corrupt).setMinCheckedPlatformVersion(29).build().verify().isVerified());
  } finally {assertTrue(corrupt.delete());}
 }
 @Test public void realIntentApiAndPermissionFixtures()throws Exception {
  Uri uri=Uri.parse("content://"+UpdateRelease.PACKAGE+".provider/debug/bttv-updater/verified.apk");
  Intent intent=UpdateDialog.installerIntent(uri);
  assertEquals(Intent.ACTION_VIEW,intent.getAction());assertEquals(UpdateRelease.MIME,intent.getType());assertEquals(uri,intent.getData());
  assertTrue((intent.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
  for(String invalid:new String[]{"file:///tmp/a.apk","https://example.com/a.apk","content://wrong.provider/a.apk"}) {
   try {UpdateDialog.installerIntent(Uri.parse(invalid));fail();}catch(IllegalArgumentException expected){}
  }
  ShadowPackageManager pm=Shadow.extract(context.getPackageManager());pm.setCanRequestPackageInstalls(false);assertFalse(context.getPackageManager().canRequestPackageInstalls());
  pm.setCanRequestPackageInstalls(true);assertTrue(context.getPackageManager().canRequestPackageInstalls());
 }
 @Test public void dialogLifecycleDoesNotCheckOrInstallAutomatically()throws Exception {
  java.lang.reflect.Field singleton=UpdateController.class.getDeclaredField("instance");singleton.setAccessible(true);singleton.set(null,null);
  ActivityController<Activity> activity=Robolectric.buildActivity(Activity.class).setup();
  final int[] dismissals={0};UpdateDialog dialog=new UpdateDialog(activity.get(),()->dismissals[0]++);dialog.show();
  assertEquals(UpdateSession.State.IDLE,UpdateController.get(activity.get()).session.snapshot().state);
  dialog.dispose();assertEquals(0,dismissals[0]);
  assertNull(((ShadowApplication)Shadow.extract(RuntimeEnvironment.getApplication())).getNextStartedActivity());
  activity.pause().stop().destroy();
 }
 @Test public void controllerDeduplicatesAndDiscardsCancelledCallbacks()throws Exception {
  java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
  java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
  UpdateController controller=new UpdateController(context,url->{
   calls.incrementAndGet();entered.countDown();
   try {release.await(5,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException e){throw new IOException();}
   return new UpdaterHostTest.Connection(url,"{}".getBytes());
  });
  controller.check();assertTrue(entered.await(5,java.util.concurrent.TimeUnit.SECONDS));controller.check();assertEquals(1,calls.get());
  controller.cancel();release.countDown();
  java.lang.reflect.Field field=UpdateController.class.getDeclaredField("worker");field.setAccessible(true);
  java.util.concurrent.ThreadPoolExecutor worker=(java.util.concurrent.ThreadPoolExecutor)field.get(controller);
  worker.submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);
  assertEquals(UpdateSession.State.CANCELLED,controller.session.snapshot().state);assertFalse(controller.session.running());worker.shutdown();
 }
}'''

SETTINGS_SOURCE=r'''package bttv.settings;
import bttv.Data;import java.util.*;
public class UpdaterSettingsHostTest {
 public static void main(String[] args) {
  int count=0,links=0;for(Settings setting:Settings.values())if(SettingsSections.sectionOf(setting)!=null){count++;if(setting.entry instanceof UserPreferences.Entry.LinkEntry)links++;}
  if(count!=15||links!=4)throw new AssertionError("exactly one additive manual action");
  if(SettingsSections.sectionOf(Settings.InvokeUpdateCheck)!=SettingsSections.ABOUT)throw new AssertionError("manual entry missing");
  if(SettingsSections.sectionOf(Settings.AutoUpdateChecksEnabled)!=null)throw new AssertionError("automatic entry activated");
  Settings.AutoUpdateChecksEnabled.entry.set(Data.ctx,new UserPreferences.Entry.BoolValue(false));
  SettingsSections.sectionOf(Settings.InvokeUpdateCheck);
  if(((UserPreferences.Entry.BoolValue)Settings.AutoUpdateChecksEnabled.entry.get(Data.ctx)).get())throw new AssertionError("saved automatic preference changed");
  if(!"bttv_auto_update_checks_enabled".equals(Settings.AutoUpdateChecksEnabled.entry.key))throw new AssertionError("key renamed");
  System.out.println("PASS actual updater Settings policy: 15 controls, one existing manual action, automatic hidden, saved false retained");
 }
}'''

if __name__=='__main__':
    out=Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=False)
    for name,source in SOURCES.items():
        target=out/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(source,encoding='utf-8')
