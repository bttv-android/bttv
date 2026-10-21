"""Host Android-view fixtures execute production UI binding and actual browser navigation."""
from pathlib import Path
import runpy
import sys

SOURCES = dict(runpy.run_path(str(Path(__file__).with_name('stage5qk_web_purchases_host.py')))['SOURCES'])
SOURCES['android/content/Context.java'] = SOURCES['android/content/Context.java'].replace('public class Context {', 'public class Context {\n public android.content.res.Resources getResources(){return new android.content.res.Resources();}')
SOURCES['bttv/ResUtil.java'] = SOURCES['bttv/ResUtil.java'].replace('public class ResUtil {', '''public class ResUtil {
 public static int getResourceId(android.content.Context c,String n,String t){return n.equals("viewer_card_social_actions")?42:0;}
 public static String getLocaleString(android.content.Context c,Res.strings s){return s.name();}''')
SOURCES.update({
'android/content/res/Resources.java': '''package android.content.res;
public class Resources {public static class Metrics {public float density=1;}public Metrics getDisplayMetrics(){return new Metrics();}}''',
'android/view/View.java': '''package android.view;
import android.content.Context;import android.content.res.Resources;
public class View {public int id;public Object tag;public ViewGroup parent;public boolean attached=true,shown=true;
 public OnClickListener listener;public final Context context;public View(Context c){context=c;}
 public interface OnClickListener{void onClick(View v);}public Context getContext(){return context;}
 public Resources getResources(){return context.getResources();}public ViewGroup getParent(){return parent;}
 public Object getTag(){return tag;}public void setTag(Object t){tag=t;}
 public boolean isAttachedToWindow(){return attached&&(parent==null||parent.isAttachedToWindow());}
 public boolean isShown(){return shown&&(parent==null||parent.isShown());}
 public void setOnClickListener(OnClickListener l){listener=l;}public void performClick(){listener.onClick(this);}
 public View findViewById(int i){return i==id?this:null;}}''',
'android/view/ViewGroup.java': '''package android.view;
import java.util.*;import android.content.Context;
public class ViewGroup extends View {public final List<View> children=new ArrayList<>();public ViewGroup(Context c){super(c);}
 public static class LayoutParams{public static final int MATCH_PARENT=-1,WRAP_CONTENT=-2;public int w,h;public LayoutParams(int a,int b){w=a;h=b;}}
 public int getChildCount(){return children.size();}public View getChildAt(int i){return children.get(i);}
 public int indexOfChild(View v){return children.indexOf(v);}public void removeView(View v){children.remove(v);v.parent=null;}
 public void addView(View v,LayoutParams p){addView(v,children.size(),p);}
 public void addView(View v,int i,LayoutParams p){if(v.parent!=null)throw new AssertionError();children.add(i,v);v.parent=this;}
 public View findViewById(int i){View v=super.findViewById(i);if(v!=null)return v;for(View c:children){v=c.findViewById(i);if(v!=null)return v;}return null;}}''',
'android/widget/LinearLayout.java': '''package android.widget;
import android.view.*;import android.content.Context;
public class LinearLayout extends ViewGroup{public static final int VERTICAL=1;public LinearLayout(Context c){super(c);}
 public void setOrientation(int i){}public void setPadding(int a,int b,int c,int d){}
 public static class LayoutParams extends ViewGroup.LayoutParams{public LayoutParams(int a,int b){super(a,b);}}}''',
'android/widget/Button.java': '''package android.widget;import android.view.View;import android.content.Context;
public class Button extends View{public CharSequence text;public Button(Context c){super(c);}public void setText(CharSequence s){text=s;}public void setAllCaps(boolean b){}}''',
'android/widget/Toast.java': '''package android.widget;import android.content.Context;
public class Toast{public static final int LENGTH_SHORT=0;public static int shown;public static Toast makeText(Context c,CharSequence s,int d){return new Toast();}public void show(){shown++;}}''',
'bttv/commerce/BrowserUiHostTest.java': r'''package bttv.commerce;
import android.app.*;import android.content.pm.*;import android.os.*;import android.view.*;import android.widget.*;
public final class BrowserUiHostTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static final class Source implements BrowserPurchaseUi.Source {String value="Channel_A";boolean loaded=true;public String login(){return loaded?value:null;}}
 static Activity activity(){Activity a=new Activity();ResolveInfo r=new ResolveInfo();r.activityInfo.packageName="browser.one";a.pm.handlers.add(r);return a;}
 static void add(LinearLayout p,View c){p.addView(c,new LinearLayout.LayoutParams(-1,-2));}
 static Button button(LinearLayout parent,int i){return (Button)((LinearLayout)parent.getChildAt(1)).getChildAt(i);}
 static void destination(Activity a,String login){check(a.last.data.toString().equals("https://www.twitch.tv/subs/"+login),"initiating channel URL");check(a.last.component.pkg.equals("browser.one"),"external browser");}
 public static void main(String[] args){
  bttv.Data.ctx=new android.content.Context();Activity a=activity();Source s=new Source();
  LinearLayout p=new LinearLayout(a);View anchor=new View(a);add(p,anchor);View nativeTab=new View(a);add(p,nativeTab);
  final int[] nativeClicks={0};anchor.setOnClickListener(v->nativeClicks[0]++);
  BrowserPurchaseUi.profile(a,anchor,s);check(p.getChildCount()==3,"add exactly one row");
  check(((LinearLayout)p.getChildAt(1)).getChildCount()==2,"visible distinct Subscribe and channel Gift");
  check(button(p,0).text.toString().equals("bttv_web_subscribe"),"Subscribe label");
  check(button(p,1).text.toString().equals("bttv_web_gift_subs"),"Gift label");
  button(p,0).performClick();destination(a,"channel_a");check(a.starts==1,"explicit web works with preferences OFF");
  button(p,1).performClick();check(a.starts==1,"rapid same URL duplicate suppressed");
  SystemClock.now+=2000;button(p,1).performClick();check(a.starts==2,"deliberate Gift later launches once");
  Button old=button(p,0);BrowserPurchaseUi.profile(a,anchor,s);check(p.getChildCount()==3,"rebind no duplicates");
  old.performClick();check(a.starts==2,"removed old row cannot launch");
  for(String login:new String[]{"Channel_B","Channel_A"}){s.value=login;BrowserPurchaseUi.profile(a,anchor,s);button(p,0).performClick();destination(a,login.toLowerCase(java.util.Locale.ROOT));}
  check(a.starts==4,"A B A destinations not duplicate-suppressed");
  s.value="Channel_B";button(p,0).performClick();check(a.starts==4,"stale A action cannot launch B or A");
  BrowserPurchaseUi.profile(a,anchor,s);s.loaded=false;button(p,1).performClick();check(a.starts==4,"loaded state invalidated safe");
  BrowserPurchaseUi.profile(a,anchor,s);check(p.getChildCount()==2,"unavailable source removes only web row");
  anchor.performClick();check(nativeClicks[0]==1&&p.getChildAt(1)==nativeTab,"native listener/tabs preserved");
  s.loaded=true;s.value="channel_a";BrowserPurchaseUi.profile(a,anchor,s);
  p.attached=false;button(p,0).performClick();check(a.starts==4,"detached orientation-old view safe");p.attached=true;
  a.destroyed=true;button(p,0).performClick();check(a.starts==4,"destroyed Activity no launch");a.destroyed=false;
  a.pm.handlers.clear();int feedback=Toast.shown;SystemClock.now+=2000;button(p,0).performClick();check(a.starts==4&&Toast.shown>feedback,"missing browser feedback, no checkout");
  a=activity();LinearLayout root=new LinearLayout(a);LinearLayout body=new LinearLayout(a);add(root,body);View social=new View(a);social.id=42;add(body,social);
  s.value="broadcaster";BrowserPurchaseUi.viewer(a,root,s);check(body.getChildCount()==2,"viewer link placed in actual social action parent");
  Button gift=button(body,0);check(gift.text.toString().equals("bttv_web_gift_manual"),"manual recipient label");
  gift.performClick();destination(a,"broadcaster");check(!a.last.data.toString().contains("recipient"),"no recipient URL injection");
  BrowserPurchaseUi.viewer(a,root,null);check(body.getChildCount()==1,"loading clears stale viewer action");
  gift.performClick();check(a.starts==1,"old viewer action cannot launch after loaded state gone");
  BrowserPurchaseUi.viewer(a,root,s);a.fail=true;SystemClock.now+=2000;button(body,0).performClick();check(a.starts==1,"browser refusal never native checkout");
  a.fail=false;button(body,0).performClick();check(a.starts==2,"failed launch does not poison retry");
  for(String bad:new String[]{null,"","12345","../recipient","a?token=x","bad space"}){s.value=bad;BrowserPurchaseUi.viewer(a,root,s);check(body.getChildCount()==1,"invalid login absent, no guessing");}
  for(String bad:new String[]{null,"","0","-1","uuid-value","1x"})check(!BrowserPurchaseUi.validChannelId(bad),"reject invalid saved channel");
  check(BrowserPurchaseUi.validChannelId("207813352"),"numeric channel context accepted but not used in URL");
  BrowserPurchaseUi.profile(null,anchor,s);BrowserPurchaseUi.viewer(null,root,s);BrowserPurchaseUi.profile(a,null,s);
  System.out.println("PASS actual browser UI binding/action/navigation JVM: "+checks+" checks");
 }
}''',
})


def generate(output):
    for name, source in SOURCES.items():
        p=Path(output)/name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(source, encoding='utf-8')


if __name__ == '__main__': generate(sys.argv[1])
