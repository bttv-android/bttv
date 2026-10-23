"""Generate JVM fixtures for the production cold-start AnonChat helper."""
from pathlib import Path
import sys

SOURCES = {
"android/content/SharedPreferences.java": """package android.content;
public interface SharedPreferences { boolean getBoolean(String key, boolean fallback); }
""",
"android/content/Context.java": """package android.content;
public abstract class Context { public abstract SharedPreferences getSharedPreferences(String name, int mode); }
""",
"android/widget/Toast.java": """package android.widget;
public final class Toast {
 public static final int LENGTH_LONG=1;
 public static int shown; public static String text;
 public static Toast makeText(android.content.Context c, CharSequence s, int d) { text=s.toString(); return new Toast(); }
 public void show() { shown++; }
}
""",
"bttv/AnonChatHostTest.java": """package bttv;
import android.content.Context;
import android.content.SharedPreferences;
public final class AnonChatHostTest {
 private static int checks;
 private static void check(boolean x,String s) { checks++; if(!x) throw new AssertionError(s); }
 private static final class Prefs implements SharedPreferences {
  boolean value; int reads;
  public boolean getBoolean(String key,boolean fallback) { check(key.equals("enable_anon_chat"),"key changed"); reads++; return value; }
 }
 private static final class App extends Context {
  final Prefs prefs=new Prefs(); boolean fail;
  public SharedPreferences getSharedPreferences(String name,int mode) {
   check(name.equals("BTTV") && mode==0,"store changed");
   if(fail) throw new IllegalStateException("fixture");
   return prefs;
  }
 }
 private static final class Owner implements AnonChat.Controller {
  int calls; public void setAnonymous(boolean ignored) { calls++; }
 }
 public static void main(String[] args) {
  boolean guest=args.length>0 && args[0].equals("on");
  boolean failure=args.length>0 && args[0].equals("failure");
  check(AnonChat.isAppliedAnonymous(),"uninitialized did not fail closed");
  App app=new App(); app.prefs.value=guest; app.fail=failure;
  AnonChat.initialize(app);
  if(failure) {
   check(AnonChat.isAppliedAnonymous(),"failed startup enabled auth");
   check(!AnonChat.allowComposer(true),"failed startup enabled composer");
   check(AnonChat.status().contains("unavailable"),"failure hidden");
   AnonChat.initialize(new App());
   check(AnonChat.isAppliedAnonymous(),"failed process silently changed mode");
  } else {
   check(AnonChat.isAppliedAnonymous()==guest,"startup mode");
   check(!AnonChat.isRestartPending(),"initially pending");
   check(AnonChat.allowComposer(true)==!guest,"composer mode");
   check(!AnonChat.allowComposer(false),"native permissions overridden");
   for(String channel:new String[]{"A","B","C","A","A"}) {
    Owner owner=new Owner();
    check(AnonChat.attach(owner)==guest,"new presenter changed mode "+channel);
    check(owner.calls==0,"live identity operation");
   }
   check(AnonChat.getViewerId(123)==(guest?0:123),"viewer mapping");
   check(AnonChat.getViewerId(456)==(guest?0:456),"stale account restored");
   app.prefs.value=!guest;
   AnonChat.reconnect(!guest);
   check(AnonChat.isRestartPending(),"desired change not pending");
   check(AnonChat.isAppliedAnonymous()==guest,"toggle changed applied mode");
   check(AnonChat.allowComposer(true)==!guest,"toggle changed composer");
   check(android.widget.Toast.text.contains("Restart pending"),"no explanation");
   check(AnonChat.status().contains("not verified"),"fabricated readiness");
   App other=new App(); other.prefs.value=!guest;
   AnonChat.initialize(other);
   check(AnonChat.isAppliedAnonymous()==guest,"reinitialization changed process mode");
   for(int i=0;i<5;i++) AnonChat.reconnect(!guest);
   check(AnonChat.isAppliedAnonymous()==guest,"repeated toggle changed mode");
   app.prefs.value=guest;
   AnonChat.reconnect(guest);
   check(!AnonChat.isRestartPending(),"returning preference remains pending");
   check(AnonChat.isAppliedAnonymous()==guest,"return changed mode");
   check(app.prefs.value==guest,"helper mutated stored preference");
  }
  System.out.println("PASS cold-start "+(failure?"failure":guest?"ON":"OFF")+" "+checks+" assertions (JVM state tests, not native/server identity tests)");
 }
}
"""
}

if __name__ == "__main__":
    out=Path(sys.argv[1]).resolve()
    for name,source in SOURCES.items():
        p=out/name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(source,encoding="utf-8")
    print("Generated cold-start JVM fixtures in",out)
