"""Generate isolated JVM fixtures for the actual redirect and preference code.

No HTTP, Android device, checkout or purchase occurs in this harness.
"""
from pathlib import Path
import runpy
import sys

BASE = runpy.run_path(str(Path(__file__).with_name('stage5p_settings_polish_host.py')))['SOURCES']
SOURCES = {k: v for k, v in BASE.items() if k in {
    'android/content/Context.java', 'android/content/SharedPreferences.java',
    'android/util/Log.java', 'org/jetbrains/annotations/NotNull.java',
    'bttv/Data.java', 'bttv/ResUtil.java',
}}
SOURCES.update({
'android/net/Uri.java': '''package android.net;
public final class Uri { public final String value; private Uri(String s){value=s;}
 public static Uri parse(String s){return new Uri(s);}public String toString(){return value;} }''',
'android/content/ComponentName.java': '''package android.content;
public class ComponentName {public final String pkg,name;
 public ComponentName(String p,String n){pkg=p;name=n;}}''',
'android/content/Intent.java': '''package android.content;
public class Intent {public static final String ACTION_VIEW="android.intent.action.VIEW",
 CATEGORY_BROWSABLE="android.intent.category.BROWSABLE";
 public final String action;public final android.net.Uri data;public String category;
 public ComponentName component;public Intent(String a,android.net.Uri u){action=a;data=u;}
 public Intent addCategory(String c){category=c;return this;}
 public Intent setComponent(ComponentName c){component=c;return this;}}''',
'android/content/IntentFilter.java': '''package android.content;
public class IntentFilter {public boolean https=true,browsable=true;public int authorities;
 public boolean hasDataScheme(String s){return https&&s.equals("https");}
 public boolean hasCategory(String c){return browsable&&c.equals(Intent.CATEGORY_BROWSABLE);}
 public int countDataAuthorities(){return authorities;}}''',
'android/content/pm/ApplicationInfo.java': '''package android.content.pm;
public class ApplicationInfo {public boolean enabled=true;}''',
'android/content/pm/ActivityInfo.java': '''package android.content.pm;
public class ActivityInfo {public boolean enabled=true,exported=true;public String packageName,name="Browser";
 public ApplicationInfo applicationInfo=new ApplicationInfo();}''',
'android/content/pm/ResolveInfo.java': '''package android.content.pm;
public class ResolveInfo {public ActivityInfo activityInfo=new ActivityInfo();
 public android.content.IntentFilter filter=new android.content.IntentFilter();}''',
'android/content/pm/PackageManager.java': '''package android.content.pm;
import java.util.*;import android.content.Intent;
public class PackageManager {public static final int MATCH_DEFAULT_ONLY=65536,GET_RESOLVED_FILTER=64;
 public List<ResolveInfo> handlers=new ArrayList<>();public ResolveInfo preferred;public int queries;
 public List<ResolveInfo> queryIntentActivities(Intent i,int f){queries++;
  if(!i.data.toString().equals("https://example.invalid/")||!Intent.CATEGORY_BROWSABLE.equals(i.category)
   ||f!=(MATCH_DEFAULT_ONLY|GET_RESOLVED_FILTER))throw new AssertionError("unsafe probe");return handlers;}
 public ResolveInfo resolveActivity(Intent i,int f){return preferred;}}''',
'android/os/SystemClock.java': '''package android.os;
public class SystemClock {public static long now=2000;public static long uptimeMillis(){return now;}}''',
'android/app/Activity.java': '''package android.app;
import android.content.*;import android.content.pm.*;
public class Activity extends Context {public boolean finishing,destroyed,fail;
 public int starts,attempts;public Intent last;public final PackageManager pm=new PackageManager();
 public boolean isFinishing(){return finishing;}public boolean isDestroyed(){return destroyed;}
 public String getPackageName(){return "tv.twitch.bttvandroid.app";}
 public PackageManager getPackageManager(){return pm;}
 public void startActivity(Intent i){attempts++;if(fail)throw new SecurityException();last=i;starts++;}}''',
'bttv/commerce/WebPurchaseHostTest.java': r'''package bttv.commerce;
import android.app.*;import android.content.*;import android.content.pm.*;import android.os.*;
import bttv.*;import bttv.settings.*;import java.util.*;
public final class WebPurchaseHostTest {
 static int checks;static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static void set(Settings s,boolean b){s.entry.set(Data.ctx,new UserPreferences.Entry.BoolValue(b));}
 static ResolveInfo handler(String p){ResolveInfo r=new ResolveInfo();r.activityInfo.packageName=p;return r;}
 static Activity activity(){Activity a=new Activity();a.pm.handlers.add(handler("browser.one"));return a;}
 public static void main(String[] args)throws Exception{
  Data.ctx=new Context();Activity a=activity();
  check(!((UserPreferences.Entry.BoolValue)Settings.WebBitsPurchasesEnabled.entry.defaultValue).get(),"Bits default OFF");
  check(!((UserPreferences.Entry.BoolValue)Settings.WebSubscriptionsEnabled.entry.defaultValue).get(),"subs default OFF");
  check(!WebPurchaseRedirect.tryBits(a)&&!WebPurchaseRedirect.trySubscribe(a,"channel_a"),"OFF retains native");
  check(a.pm.queries==0&&a.starts==0,"OFF never resolves or launches");
  set(Settings.WebBitsPurchasesEnabled,true);
  check(WebPurchaseRedirect.tryBits(a)&&a.starts==1,"Bits ON launches exactly once");
  check(a.last.data.toString().equals("https://www.twitch.tv/bits"),"official Bits URL");
  check(a.last.action.equals(Intent.ACTION_VIEW)&&a.last.category.equals(Intent.CATEGORY_BROWSABLE),"HTTPS browser intent");
  check(a.last.component.pkg.equals("browser.one"),"explicit external handler");
  check(!WebPurchaseRedirect.trySubscribe(a,"channel_a"),"independent subs preference");
  check(WebPurchaseRedirect.tryBits(a)&&a.starts==1,"duplicate suppressed as handled, no native fallthrough");
  SystemClock.now+=1001;check(WebPurchaseRedirect.tryBits(a)&&a.starts==2,"later deliberate tap allowed");
  set(Settings.WebSubscriptionsEnabled,true);
  for(String login:new String[]{"Channel_A","other_b","Channel_A"}){
   SystemClock.now+=1001;check(WebPurchaseRedirect.trySubscribe(a,login),"A/B/A launch");
   check(a.last.data.toString().equals("https://www.twitch.tv/subs/"+login.toLowerCase(Locale.ROOT)),"action-local channel");
  }
  int count=a.starts;
  for(String invalid:new String[]{null,"","12345678","a/b","a?gift=x","@name","a b","a%2Fb","a#x","a.b","a\\b","é","abcdefghijklmnopqrstuvwxyz"}){
   check(!WebPurchaseRedirect.trySubscribe(a,invalid),"invalid login falls back");
  }
  check(a.starts==count,"invalid identity never launches");
  for(String valid:new String[]{"a","A_1","legacy_name","a123456789012345678901234"})check(WebPurchaseRedirect.validLogin(valid),"valid login");
  set(Settings.WebBitsPurchasesEnabled,false);
  check(!WebPurchaseRedirect.tryBits(a),"OFF overrides recent duplicate guard");
  check(WebPurchaseRedirect.trySubscribe(activity(),"new_channel"),"subs independent of Bits");
  set(Settings.WebBitsPurchasesEnabled,true);
  for(String f:new String[]{"prefs","editor"}){java.lang.reflect.Field field=UserPreferences.class.getDeclaredField(f);field.setAccessible(true);field.set(null,null);}
  Data.ctx=new Context();check(WebPurchaseRedirect.tryBits(activity()),"Bits persisted after preference reload");
  check(WebPurchaseRedirect.trySubscribe(activity(),"reload"),"subs persisted independently");
  Activity missing=activity();missing.pm.handlers.clear();check(!WebPurchaseRedirect.tryBits(missing),"no browser fallback");
  missing.pm.handlers=null;check(!WebPurchaseRedirect.tryBits(missing),"null handler list fallback");
  Activity fail=activity();fail.fail=true;check(!WebPurchaseRedirect.tryBits(fail)&&fail.attempts==1&&fail.starts==0,"failed launch falls back once");
  fail.fail=false;check(WebPurchaseRedirect.tryBits(fail)&&fail.starts==1,"failed attempt not cached");
  check(!WebPurchaseRedirect.tryBits(null),"null Activity fallback");
  Activity dead=activity();dead.finishing=true;check(!WebPurchaseRedirect.tryBits(dead),"finishing fallback");
  dead.finishing=false;dead.destroyed=true;check(!WebPurchaseRedirect.tryBits(dead),"destroyed fallback");
  for(int i=0;i<7;i++){
   Activity bad=activity();ResolveInfo r=bad.pm.handlers.get(0);
   switch(i){case 0:r.activityInfo.packageName=bad.getPackageName();break;
    case 1:r.filter.authorities=1;break;case 2:r.activityInfo.exported=false;break;
    case 3:r.activityInfo.enabled=false;break;case 4:r.activityInfo.applicationInfo.enabled=false;break;
    case 5:r.filter.https=false;break;case 6:r.filter.browsable=false;break;}
   check(!WebPurchaseRedirect.tryBits(bad)&&bad.starts==0,"unsafe/self/app-link handler rejected");
  }
  Activity preferred=activity();ResolveInfo second=handler("browser.two");preferred.pm.handlers.add(second);preferred.pm.preferred=second;
  check(WebPurchaseRedirect.tryBits(preferred)&&preferred.last.component.pkg.equals("browser.two"),"default eligible browser honored");
  Activity official=activity();ResolveInfo twitch=handler("tv.twitch.android.app");twitch.filter.authorities=1;
  official.pm.handlers.add(0,twitch);official.pm.preferred=twitch;
  check(WebPurchaseRedirect.tryBits(official)&&official.last.component.pkg.equals("browser.one"),"official Twitch app-link not captured");
  ResUtil.fail=true;check(!WebPurchaseRedirect.tryBits(activity()),"preference failure fail-open");ResUtil.fail=false;
  int nativeCalls=0;set(Settings.WebBitsPurchasesEnabled,false);if(!WebPurchaseRedirect.tryBits(activity()))nativeCalls++;
  set(Settings.WebBitsPurchasesEnabled,true);if(!WebPurchaseRedirect.tryBits(activity()))nativeCalls++;
  Activity noBrowser=activity();noBrowser.pm.handlers.clear();if(!WebPurchaseRedirect.tryBits(noBrowser))nativeCalls++;
  check(nativeCalls==2,"OFF/launch-failed native, successful browser no native");
  for(java.lang.reflect.Field f:WebPurchaseRedirect.class.getDeclaredFields())check(!Activity.class.isAssignableFrom(f.getType()),"no strong Activity singleton");
  System.out.println("PASS Stage 5Q-K production redirect + actual preferences JVM: "+checks+" checks");
 }
}''',
})


def generate(output):
    for name, source in SOURCES.items():
        target = Path(output)/name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding='utf-8')


if __name__ == '__main__':
    generate(Path(sys.argv[1]))
