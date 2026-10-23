"""Offline tests of the real policy, parser, controller and Android lifecycle/UI bridge."""
from pathlib import Path
import runpy
import sys

ROOT=Path(__file__).resolve().parents[1]
SOURCES=runpy.run_path(str(ROOT/'tests/stage5q_updater_host.py'))['SOURCES'].copy()
SOURCES['bttv/updater/AutoUpdateStateTest.java']=r'''package bttv.updater;
import org.junit.*;import static org.junit.Assert.*;import java.util.*;import org.json.*;
public class AutoUpdateStateTest {
 static class Store implements AutoUpdateState.Store {
  boolean enabled=true,fail; final Map<String,Long> values=new HashMap<>();
  public boolean enabled(){return enabled;}
  public long get(String k){return values.getOrDefault(k,0L);}
  public void checked(long s,long a,long r){if(fail)throw new IllegalStateException();values.put(AutoUpdateState.SUCCESS,s);values.put(AutoUpdateState.ATTEMPT,a);values.put(AutoUpdateState.RETRY,r);}
  public void notified(long c){if(fail)throw new IllegalStateException();values.put(AutoUpdateState.NOTIFIED,c);}
 }
 Store store;AutoUpdateState state;final long now=1000000;
 @Before public void setup(){store=new Store();state=new AutoUpdateState(store);}
 UpdateRelease release(long code)throws Exception {
  byte[] b={1};return UpdateRelease.inspect(UpdaterHostTest.release(UpdaterHostTest.compatibility(b).put("versionCode",code),b).toString(),UpdaterHostTest.installed()).candidate;
 }
 void finish(UpdateRelease.Outcome o,UpdateRelease r){long t=state.begin(now);assertTrue(t>0);assertTrue(state.complete(t,now,o,r,0));}
 @Test public void defaultOnAllowsFirstCheck(){assertTrue(state.begin(now)>0);}
 @Test public void explicitOffNeverStarts(){store.enabled=false;assertEquals(-1,state.begin(now));assertNull(state.notice());}
 @Test public void savedOffSurvivesNewCoordinator(){store.enabled=false;assertEquals(-1,new AutoUpdateState(store).begin(now));assertFalse(store.enabled);}
 @Test public void savedOnAndMissingTimestampsAreDue(){assertTrue(new AutoUpdateState(store).begin(now)>0);}
 @Test public void successfulCheckPersistsAndThrottles24Hours(){finish(UpdateRelease.Outcome.NOT_NEWER,null);assertEquals(now,store.get(AutoUpdateState.SUCCESS));assertEquals(-1,state.begin(now+AutoUpdateState.DAY-1));assertTrue(state.begin(now+AutoUpdateState.DAY)>0);}
 @Test public void processRecreationRetainsSuccessfulThrottle(){finish(UpdateRelease.Outcome.NOT_NEWER,null);assertEquals(-1,new AutoUpdateState(store).begin(now+100));}
 @Test public void repeatedResumeCannotParallelize(){long t=state.begin(now);for(int i=0;i<100;i++)assertEquals(-1,state.begin(now+i));state.abort(t);assertTrue(state.begin(now)>t);}
 @Test public void failedCheckGetsHourBackoff(){long t=state.begin(now);assertFalse(state.complete(t,now,UpdateRelease.Outcome.NETWORK,null,0));assertEquals(-1,state.begin(now+AutoUpdateState.BACKOFF-1));assertTrue(state.begin(now+AutoUpdateState.BACKOFF)>0);assertEquals(0,store.get(AutoUpdateState.SUCCESS));}
 @Test public void serverRetryAfterIsRespected(){long t=state.begin(now);state.complete(t,now,UpdateRelease.Outcome.RATE_LIMIT,null,now+2*AutoUpdateState.BACKOFF);assertEquals(-1,state.begin(now+AutoUpdateState.BACKOFF));assertTrue(state.begin(now+2*AutoUpdateState.BACKOFF)>0);}
 @Test public void malformedMetadataGetsBackoffNotSuccess(){long t=state.begin(now);state.complete(t,now,UpdateRelease.Outcome.INVALID_METADATA,null,0);assertEquals(0,store.get(AutoUpdateState.SUCCESS));assertEquals(-1,state.begin(now+1));}
 @Test public void knownIncompatibleReleaseIsSuccessfulCheck(){finish(UpdateRelease.Outcome.UNSUPPORTED,null);assertEquals(-1,state.begin(now+1));assertNull(state.notice());}
 @Test public void missingAssetIsNotAnOffer(){finish(UpdateRelease.Outcome.MISSING_ASSET,null);assertNull(state.notice());assertEquals(now,store.get(AutoUpdateState.SUCCESS));}
 @Test public void eligibleReleaseBecomesNotice()throws Exception{UpdateRelease r=release(1208000339L);finish(UpdateRelease.Outcome.AVAILABLE,r);assertSame(r,state.notice());}
 @Test public void shownReleaseIsNotRepeatedAfterDismissOrRestart()throws Exception{UpdateRelease r=release(1208000339L);finish(UpdateRelease.Outcome.AVAILABLE,r);assertTrue(state.notified(r));assertNull(state.notice());state=new AutoUpdateState(store);long t=state.begin(now+AutoUpdateState.DAY);state.complete(t,now+AutoUpdateState.DAY,UpdateRelease.Outcome.AVAILABLE,r,0);assertNull(state.notice());}
 @Test public void genuinelyHigherCodeCanNotify()throws Exception{UpdateRelease first=release(1208000339L);finish(UpdateRelease.Outcome.AVAILABLE,first);state.notified(first);long t=state.begin(now+AutoUpdateState.DAY);UpdateRelease second=release(1208000340L);state.complete(t,now+AutoUpdateState.DAY,UpdateRelease.Outcome.AVAILABLE,second,0);assertSame(second,state.notice());}
 @Test public void offMidFlightRejectsStaleSuccess()throws Exception{long t=state.begin(now);store.enabled=false;state.disabled();assertFalse(state.complete(t,now,UpdateRelease.Outcome.AVAILABLE,release(1208000339L),0));assertNull(state.notice());assertEquals(0,store.get(AutoUpdateState.SUCCESS));}
 @Test public void offSuppressesReadyNotice()throws Exception{finish(UpdateRelease.Outcome.AVAILABLE,release(1208000339L));store.enabled=false;assertNull(state.notice());}
 @Test public void staleCompletionCannotReplaceNewRequest(){long a=state.begin(now);state.disabled();long b=state.begin(now);assertFalse(state.complete(a,now,UpdateRelease.Outcome.NETWORK,null,0));assertTrue(state.complete(b,now,UpdateRelease.Outcome.NOT_NEWER,null,0));}
 @Test public void cancellationDoesNotPretendSuccessfulCheck(){long t=state.begin(now);assertFalse(state.complete(t,now,UpdateRelease.Outcome.CANCELLED,null,0));assertEquals(0,store.get(AutoUpdateState.SUCCESS));}
 @Test public void preferenceWriteFailureStillThrottlesInMemory(){long t=state.begin(now);store.fail=true;try{state.complete(t,now,UpdateRelease.Outcome.NETWORK,null,0);fail();}catch(IllegalStateException expected){}assertEquals(-1,state.begin(now+1));}
 @Test public void actualParserSameAndOlderNeverOffer()throws Exception{for(long code:new long[]{1208000338L,1208000337L}){byte[] b={1};UpdateRelease.Inspection i=UpdateRelease.inspect(UpdaterHostTest.release(UpdaterHostTest.compatibility(b).put("versionCode",code),b).toString(),UpdaterHostTest.installed());assertNull(i.candidate);assertEquals(UpdateRelease.Outcome.NOT_NEWER,i.outcome);}}
 @Test public void actualParserNewerEligible()throws Exception{assertNotNull(release(1208000339L));}
 @Test public void actualParserRejectsWrongTwitchBase()throws Exception{byte[] b={1};try{UpdateRelease.inspect(UpdaterHostTest.release(UpdaterHostTest.compatibility(b).put("twitchVersion","19.0.1"),b).toString(),UpdaterHostTest.installed());fail();}catch(UpdateRelease.Problem p){assertEquals(UpdateRelease.Outcome.UNSUPPORTED,p.outcome);}}
 @Test public void actualParserMissingApkCategory()throws Exception{byte[] b={1};JSONObject j=UpdaterHostTest.release(UpdaterHostTest.compatibility(b),b).put("assets",new JSONArray());try{UpdateRelease.inspect(j.toString(),UpdaterHostTest.installed());fail();}catch(UpdateRelease.Problem p){assertEquals(UpdateRelease.Outcome.MISSING_ASSET,p.outcome);}}
 @Test public void actualParserPrereleaseAndDraftPolicyUnchanged()throws Exception{byte[] b={1};for(String key:new String[]{"draft","prerelease"}){JSONObject j=UpdaterHostTest.release(UpdaterHostTest.compatibility(b),b).put(key,true);UpdateRelease.Inspection r=UpdateRelease.inspect(j.toString(),UpdaterHostTest.installed());assertNull(r.candidate);assertEquals(UpdateRelease.Outcome.UNSUPPORTED,r.outcome);}}
 @Test public void malformedMetadataIsCategoricalAndSafe()throws Exception{for(String j:new String[]{"{","[]","null",""})try{UpdateRelease.inspect(j,UpdaterHostTest.installed());fail();}catch(UpdateRelease.Problem p){assertEquals(UpdateRelease.Outcome.INVALID_METADATA,p.outcome);}}
}
'''
SOURCES['tv/twitch/android/feature/viewer/landing/ViewerLandingActivity.java']=r'''package tv.twitch.android.feature.viewer.landing;
public class ViewerLandingActivity extends android.app.Activity {
 @Override public boolean hasWindowFocus(){return true;}
}
'''
SOURCES['bttv/updater/AutoUpdateAndroidTest.java']=r'''package bttv.updater;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;
import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.*;import org.robolectric.android.controller.ActivityController;import org.robolectric.util.ReflectionHelpers;
import android.app.*;import android.content.*;import android.content.pm.*;import android.os.*;import android.view.*;import android.widget.*;
import java.io.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.time.Duration;import org.json.*;
import tv.twitch.android.feature.viewer.landing.ViewerLandingActivity;
@RunWith(RobolectricTestRunner.class) @Config(sdk=30,manifest=Config.NONE,application=AutoUpdateAndroidTest.TestApplication.class) @LooperMode(LooperMode.Mode.PAUSED)
public class AutoUpdateAndroidTest {
 public static class TestApplication extends Application {
  @Override public String getPackageName(){return UpdateRelease.PACKAGE;}
 }
 Application app;SharedPreferences prefs;UpdateController controller;AutoUpdateChecker checker;
 AtomicInteger calls;String body;int status;IOException network;CountDownLatch entered,gate;boolean allow=true;
 UpdateRelease.Outcome outcome;UpdateRelease found;long retry;
 // Typed extraction avoids unrelated API31+ overloads while compiling API30 fixtures.
 static final class Shadows {
  static ShadowLooper shadowOf(Looper value){return Shadow.extract(value);}
  static ShadowApplication shadowOf(Application value){return Shadow.extract(value);}
 }
 @Before public void setup()throws Exception{
  app=RuntimeEnvironment.getApplication();prefs=app.getSharedPreferences("BTTV",0);prefs.edit().clear().commit();calls=new AtomicInteger();status=200;
  ReflectionHelpers.setStaticField(Build.class,"SUPPORTED_ABIS",new String[]{"arm64-v8a","armeabi-v7a"});
  Signature signer=new Signature(new byte[]{1,2,3});PackageInfo p=new PackageInfo();p.packageName=UpdateRelease.PACKAGE;p.versionName="0.43.2-beta";p.setLongVersionCode(1208000338L);
  p.applicationInfo=app.getApplicationInfo();p.signingInfo=new SigningInfo();((ShadowSigningInfo)Shadow.extract(p.signingInfo)).setSignatures(new Signature[]{signer});
  ((ShadowPackageManager)Shadow.extract(app.getPackageManager())).installPackage(p);
  byte[] b={1};JSONObject c=UpdaterHostTest.compatibility(b).put("signerSha256",UpdateVerifier.installed(app).signer);body=UpdaterHostTest.release(c,b).toString();
  controller=new UpdateController(app,url->{
   assertEquals("metadata URL only",UpdateRelease.API,url.toString());calls.incrementAndGet();
   if(entered!=null)entered.countDown();
   if(gate!=null)try{if(!gate.await(5,TimeUnit.SECONDS))throw new IOException("fixture timeout");}catch(InterruptedException e){throw new IOException();}
   if(network!=null)throw network;
   UpdaterHostTest.Connection conn=new UpdaterHostTest.Connection(url,body.getBytes(java.nio.charset.StandardCharsets.UTF_8));conn.status=status;return conn;
  });
  singleton("instance",controller,UpdateController.class);singleton("instance",null,AutoUpdateChecker.class);checker=new AutoUpdateChecker(app);
 }
 static void singleton(String n,Object value,Class<?> cls)throws Exception{Field f=cls.getDeclaredField(n);f.setAccessible(true);f.set(null,value);}
 <T>T field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return (T)f.get(o);}
 void done()throws Exception{ThreadPoolExecutor worker=field(controller,"worker");worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();}
 long automatic(){return controller.checkAutomatically(new UpdateController.AutomaticCheck(){public boolean enabled(){return allow;}public void finished(long id,UpdateRelease.Outcome o,UpdateRelease r,long at){outcome=o;found=r;retry=at;}});}
 @After public void cleanup()throws Exception{if(gate!=null)gate.countDown();if(controller!=null){ThreadPoolExecutor worker=field(controller,"worker");worker.shutdownNow();}singleton("instance",null,UpdateController.class);singleton("instance",null,AutoUpdateChecker.class);}
 @Test public void sharedControllerAutomaticMetadataOnly()throws Exception{assertTrue(automatic()>0);done();assertEquals(1,calls.get());assertEquals(UpdateRelease.Outcome.AVAILABLE,outcome);assertNotNull(found);assertEquals(UpdateSession.State.AVAILABLE,controller.session.snapshot().state);assertFalse(new File(app.getCacheDir(),"bttv-updater").exists());assertNull(Shadows.shadowOf(app).getNextStartedActivity());}
 @Test public void manualChecksWorkWithAutoOff()throws Exception{prefs.edit().putBoolean(AutoUpdateState.ENABLED,false).commit();controller.check();done();assertEquals(1,calls.get());assertEquals(UpdateSession.State.AVAILABLE,controller.session.snapshot().state);}
 @Test public void automaticAndManualCannotRaceTwoRequests()throws Exception{entered=new CountDownLatch(1);gate=new CountDownLatch(1);assertTrue(automatic()>0);assertTrue(entered.await(5,TimeUnit.SECONDS));assertEquals(-1,automatic());controller.check();assertEquals(1,calls.get());gate.countDown();done();}
 @Test public void networkUnavailableClassification()throws Exception{network=new java.net.UnknownHostException();automatic();done();assertEquals(UpdateRelease.Outcome.NETWORK,outcome);assertNull(found);}
 @Test public void apiRateLimitClassification()throws Exception{status=429;automatic();done();assertEquals(UpdateRelease.Outcome.RATE_LIMIT,outcome);assertTrue(retry>0);}
 @Test public void serverFailureClassification()throws Exception{status=503;automatic();done();assertEquals(UpdateRelease.Outcome.SERVER,outcome);}
 @Test public void otherHttpFailureClassification()throws Exception{status=404;automatic();done();assertEquals(UpdateRelease.Outcome.HTTP_ERROR,outcome);}
 @Test public void malformedResponseClassification()throws Exception{body="{";automatic();done();assertEquals(UpdateRelease.Outcome.INVALID_METADATA,outcome);}
 @Test public void offMidFlightCancelsOnlyAutomaticOwner()throws Exception{entered=new CountDownLatch(1);gate=new CountDownLatch(1);automatic();assertTrue(entered.await(5,TimeUnit.SECONDS));allow=false;controller.cancelAutomatic();gate.countDown();done();assertEquals(UpdateRelease.Outcome.DISABLED,outcome);assertEquals(UpdateSession.State.CANCELLED,controller.session.snapshot().state);assertNull(Shadows.shadowOf(app).getNextStartedActivity());}
 @Test public void cancelAutomaticDoesNotCancelManual()throws Exception{entered=new CountDownLatch(1);gate=new CountDownLatch(1);controller.check();assertTrue(entered.await(5,TimeUnit.SECONDS));controller.cancelAutomatic();assertEquals(UpdateSession.State.CHECKING,controller.session.snapshot().state);gate.countDown();done();assertEquals(UpdateSession.State.AVAILABLE,controller.session.snapshot().state);}
 @Test public void manualDialogProtectsItsSelectionFromAuto()throws Exception{ActivityController<Activity> a=Robolectric.buildActivity(Activity.class).setup();UpdateDialog dialog=new UpdateDialog(a.get(),()->{});dialog.show();assertEquals(-1,automatic());assertEquals(0,calls.get());dialog.dispose();a.pause().stop().destroy();}
 @Test public void foregroundSchedulesOneCheckAndShowsNonmodalNotice()throws Exception{ActivityController<ViewerLandingActivity> a=Robolectric.buildActivity(ViewerLandingActivity.class).setup();checker.onActivityResumed(a.get());checker.onActivityResumed(a.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));done();assertEquals(1,calls.get());assertTrue(prefs.getLong(AutoUpdateState.SUCCESS,0)>0);assertNotNull(field(checker,"banner"));assertNull(Shadows.shadowOf(app).getNextStartedActivity());checker.onActivityPaused(a.get());assertNull(field(checker,"banner"));checker.onActivityResumed(a.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));assertEquals(1,calls.get());assertNull(field(checker,"banner"));checker.onActivityPaused(a.get());a.pause().stop().destroy();}
 @Test public void destroyedOwnerNeverReceivesBanner()throws Exception{ActivityController<ViewerLandingActivity> a=Robolectric.buildActivity(ViewerLandingActivity.class).setup();entered=new CountDownLatch(1);gate=new CountDownLatch(1);checker.onActivityResumed(a.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));assertTrue(entered.await(5,TimeUnit.SECONDS));checker.onActivityDestroyed(a.get());a.pause().stop().destroy();gate.countDown();done();assertNull(field(checker,"banner"));assertNull(((java.lang.ref.WeakReference<?>)field(checker,"resumed")).get());assertNull(Shadows.shadowOf(app).getNextStartedActivity());}
 @Test public void offBeforeForegroundHasNoRequestAndSavedChoiceIsPreserved()throws Exception{prefs.edit().putBoolean(AutoUpdateState.ENABLED,false).commit();ActivityController<ViewerLandingActivity> a=Robolectric.buildActivity(ViewerLandingActivity.class).setup();checker.onActivityResumed(a.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000));assertEquals(0,calls.get());assertFalse(prefs.getBoolean(AutoUpdateState.ENABLED,true));checker.onActivityPaused(a.get());a.pause().stop().destroy();}
 @Test public void laterAndViewAreNotAutomaticDownloads()throws Exception{ActivityController<ViewerLandingActivity> a=Robolectric.buildActivity(ViewerLandingActivity.class).setup();checker.onActivityResumed(a.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));done();LinearLayout row=field(checker,"banner");assertNotNull(row);((Button)row.getChildAt(1)).performClick();assertNotNull(field(checker,"viewed"));assertNull(field(checker,"banner"));assertEquals(UpdateSession.State.AVAILABLE,controller.session.snapshot().state);assertEquals(1,calls.get());assertNull(Shadows.shadowOf(app).getNextStartedActivity());checker.onActivityPaused(a.get());assertNull(field(checker,"viewed"));a.pause().stop().destroy();}
 @Test public void initializationRegistersOnlyOnceAndNoJobService()throws Exception{AutoUpdateChecker.initialize(app);Object first=AutoUpdateChecker.class.getDeclaredField("instance");AutoUpdateChecker.initialize(app);Object callbacks=ReflectionHelpers.getField(app,"mActivityLifecycleCallbacks");int n=0;for(Object callback:(List<?>)callbacks)if(callback instanceof AutoUpdateChecker)n++;assertEquals(1,n);assertEquals(0,((android.app.job.JobScheduler)app.getSystemService(Context.JOB_SCHEDULER_SERVICE)).getAllPendingJobs().size());assertEquals(0,calls.get());}
 @Test public void actualPreferenceOffListenerCancelsAndSuppressesNotice()throws Exception{prefs.registerOnSharedPreferenceChangeListener(checker);ActivityController<ViewerLandingActivity> a=Robolectric.buildActivity(ViewerLandingActivity.class).setup();entered=new CountDownLatch(1);gate=new CountDownLatch(1);checker.onActivityResumed(a.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));assertTrue(entered.await(5,TimeUnit.SECONDS));prefs.edit().putBoolean(AutoUpdateState.ENABLED,false).commit();Shadows.shadowOf(Looper.getMainLooper()).idle();gate.countDown();done();assertEquals(UpdateSession.State.CANCELLED,controller.session.snapshot().state);assertNull(field(checker,"banner"));assertEquals(0,prefs.getLong(AutoUpdateState.SUCCESS,0));prefs.unregisterOnSharedPreferenceChangeListener(checker);checker.onActivityPaused(a.get());a.pause().stop().destroy();}
 @Test public void laterDismissesWithoutDownloadOrRepeatedNotice()throws Exception{ActivityController<ViewerLandingActivity> a=Robolectric.buildActivity(ViewerLandingActivity.class).setup();checker.onActivityResumed(a.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));done();LinearLayout row=field(checker,"banner");assertNotNull(row);assertFalse(prefs.contains(AutoUpdateState.ENABLED));((Button)row.getChildAt(2)).performClick();assertNull(field(checker,"banner"));assertEquals(1208000339L,prefs.getLong(AutoUpdateState.NOTIFIED,0));checker.onActivityPaused(a.get());checker=new AutoUpdateChecker(app);checker.onActivityResumed(a.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));assertNull(field(checker,"banner"));assertEquals(1,calls.get());assertNull(Shadows.shadowOf(app).getNextStartedActivity());checker.onActivityPaused(a.get());a.pause().stop().destroy();}
 @Test public void nonHostLoginLikeActivityDefersNoticeToViewer()throws Exception{ActivityController<Activity> login=Robolectric.buildActivity(Activity.class).setup();checker.onActivityResumed(login.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));done();assertNull(field(checker,"banner"));checker.onActivityPaused(login.get());login.pause().stop().destroy();ActivityController<ViewerLandingActivity> viewer=Robolectric.buildActivity(ViewerLandingActivity.class).setup();checker.onActivityResumed(viewer.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));assertNotNull(field(checker,"banner"));assertEquals(1,calls.get());checker.onActivityPaused(viewer.get());viewer.pause().stop().destroy();}
 @Test public void readyManualApkCannotBeInvalidatedByAutomaticCheck()throws Exception{UpdateRelease r=UpdaterHostTest.candidate(new byte[]{1});long c=controller.session.begin(UpdateSession.State.CHECKING,System.currentTimeMillis());controller.session.finish(c,UpdateSession.State.AVAILABLE,"fixture",r,null);long d=controller.session.begin(UpdateSession.State.DOWNLOADING,System.currentTimeMillis());File file=File.createTempFile("ready-updater-",".apk");controller.session.finish(d,UpdateSession.State.READY,"fixture",r,file);assertEquals(-1,automatic());assertSame(file,controller.session.snapshot().file);assertTrue(file.isFile());assertEquals(0,calls.get());assertTrue(file.delete());}
 @Test public void manualAvailableChoiceCannotBeReplacedByAuto()throws Exception{UpdateRelease r=UpdaterHostTest.candidate(new byte[]{1});long c=controller.session.begin(UpdateSession.State.CHECKING,System.currentTimeMillis());controller.session.finish(c,UpdateSession.State.AVAILABLE,"fixture",r,null);assertEquals(-1,automatic());assertSame(r,controller.session.snapshot().release);assertEquals(0,calls.get());}
 @Test public void aLaterManualResultInvalidatesOldAutomaticNotice()throws Exception{ActivityController<Activity> login=Robolectric.buildActivity(Activity.class).setup();checker.onActivityResumed(login.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));done();assertEquals(UpdateSession.State.AVAILABLE,controller.session.snapshot().state);long manual=controller.session.begin(UpdateSession.State.CHECKING,System.currentTimeMillis()+31000);assertTrue(manual>0);controller.session.finish(manual,UpdateSession.State.NONE,"manual result",null,null);checker.onActivityPaused(login.get());login.pause().stop().destroy();ActivityController<ViewerLandingActivity> viewer=Robolectric.buildActivity(ViewerLandingActivity.class).setup();checker.onActivityResumed(viewer.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));assertNull(field(checker,"banner"));assertEquals(1,calls.get());checker.onActivityPaused(viewer.get());viewer.pause().stop().destroy();}
}
'''

if __name__=='__main__':
    out=Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=False)
    for name,source in SOURCES.items():
        p=out/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source,encoding='utf-8')
    print('Generated real updater + automatic policy/lifecycle tests:',len(SOURCES),'source files')
