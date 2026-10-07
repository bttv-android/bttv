"""Generate isolated JVM fixtures for classic DeletedMessages.reSpan behavior.

Usage: python tests/stage5g_deleted_messages.py <ignored-output-directory>
Compile the generated stubs and runner with the production DeletedMessages.java.
"""

from pathlib import Path
import sys


SOURCES = {
    "android/graphics/Typeface.java": """package android.graphics;
public final class Typeface { public static final int ITALIC = 2; }
""",
    "android/text/Spanned.java": """package android.text;
public interface Spanned extends CharSequence {
    <T> T[] getSpans(int start, int end, Class<T> type);
    int getSpanStart(Object span);
    int getSpanEnd(Object span);
}
""",
    "android/text/SpannableString.java": """package android.text;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
public class SpannableString implements Spanned {
    static final class Record { final Object span; final int start, end;
        Record(Object span, int start, int end) { this.span=span; this.start=start; this.end=end; }}
    protected final String text;
    protected final List<Record> spans = new ArrayList<>();
    public SpannableString(CharSequence source) {
        text=source.toString();
        if (source instanceof SpannableStringBuilder) spans.addAll(((SpannableStringBuilder)source).spans);
        else if (source instanceof SpannableString) spans.addAll(((SpannableString)source).spans);
    }
    public void setSpan(Object span, int start, int end, int flags) { spans.add(new Record(span,start,end)); }
    public int length(){return text.length();} public char charAt(int i){return text.charAt(i);}
    public CharSequence subSequence(int s,int e){return text.substring(s,e);} public String toString(){return text;}
    @SuppressWarnings("unchecked") public <T> T[] getSpans(int start,int end,Class<T> type){
        List<T> out=new ArrayList<>(); for(Record r:spans) if(type.isInstance(r.span)&&r.end>start&&r.start<end) out.add((T)r.span);
        return out.toArray((T[])Array.newInstance(type,out.size())); }
    public int getSpanStart(Object span){for(Record r:spans)if(r.span==span)return r.start;return -1;}
    public int getSpanEnd(Object span){for(Record r:spans)if(r.span==span)return r.end;return -1;}
}
""",
    "android/text/SpannableStringBuilder.java": """package android.text;
import java.util.ArrayList;
import java.util.List;
public class SpannableStringBuilder extends SpannableString {
    private final StringBuilder builder;
    public SpannableStringBuilder(CharSequence source){super(source);builder=new StringBuilder(source);}
    public SpannableStringBuilder append(CharSequence value){
        int offset=builder.length(); builder.append(value);
        if(value instanceof SpannableString) for(Record r:((SpannableString)value).spans)
            spans.add(new Record(r.span,offset+r.start,offset+r.end));
        return this;
    }
    public void removeSpan(Object span){spans.removeIf(r->r.span==span);}
    public SpannableStringBuilder replace(int start,int end,CharSequence value){
        int delta=value.length()-(end-start);
        List<Record> old=new ArrayList<>(spans); spans.clear();
        for(Record r:old){
            if(r.end<=start)spans.add(r);
            else if(r.start>=end)spans.add(new Record(r.span,r.start+delta,r.end+delta));
            else if(r.start<start&&r.end>end)spans.add(new Record(r.span,r.start,r.end+delta));
        }
        if(value instanceof SpannableString) for(Record r:((SpannableString)value).spans)
            spans.add(new Record(r.span,start+r.start,start+r.end));
        builder.replace(start,end,value.toString());
        return this;
    }
    public int length(){return builder.length();} public char charAt(int i){return builder.charAt(i);}
    public CharSequence subSequence(int s,int e){return builder.substring(s,e);} public String toString(){return builder.toString();}
}
""",
    "android/text/SpannedString.java": """package android.text;
public class SpannedString extends SpannableString { public SpannedString(CharSequence source){super(source);} }
""",
    "android/text/style/StyleSpan.java": """package android.text.style;
public final class StyleSpan { private final int style; public StyleSpan(int style){this.style=style;} public int getStyle(){return style;} }
""",
    "android/text/style/ForegroundColorSpan.java": """package android.text.style;
public final class ForegroundColorSpan { private final int color; public ForegroundColorSpan(int color){this.color=color;} public int getForegroundColor(){return color;} }
""",
    "android/text/style/ClickableSpan.java": """package android.text.style;
public final class ClickableSpan {}
""",
    "android/text/style/URLSpan.java": """package android.text.style;
public final class URLSpan { public URLSpan(String url) {} }
""",
    "android/content/Context.java": """package android.content; public class Context {}\n""",
    "bttv/Data.java": """package bttv; public final class Data { public static final android.content.Context ctx=new android.content.Context(); }\n""",
    "bttv/Res.java": """package bttv; public final class Res { public enum strings { bttv_removed_by_mod } }\n""",
    "bttv/settings/Settings.java": """package bttv.settings; public enum Settings { ShowDeletedMessagesEnabled }\n""",
    "bttv/ResUtil.java": """package bttv;
public final class ResUtil {
    public static boolean enabled;
    public static int localeCalls;
    public static boolean getBooleanFromSettings(bttv.settings.Settings setting){return enabled;}
    public static String getLocaleString(android.content.Context context,Res.strings value){localeCalls++;return "removed";}
}
""",
    "bttv/api/DeletedMessagesHostTest.java": """package bttv.api;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
public final class DeletedMessagesHostTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        SpannableString original=new SpannableString("hello🙂");
        Object nativeTwitchEmoteSpan=new Object();
        Object thirdPartyEmoteSpan=new Object();
        ClickableSpan clickableSpan=new ClickableSpan();
        URLSpan linkSpan=new URLSpan("https://example.invalid");
        StyleSpan existingStyleSpan=new StyleSpan(1);
        original.setSpan(nativeTwitchEmoteSpan,0,1,17);
        original.setSpan(thirdPartyEmoteSpan,1,2,17);
        original.setSpan(clickableSpan,2,3,33);
        original.setSpan(linkSpan,3,5,33);
        original.setSpan(existingStyleSpan,5,7,33);

        bttv.ResUtil.enabled=false;
        bttv.ResUtil.localeCalls=0;
        check(DeletedMessages.reSpan(original)==null,"disabled setting did not return null");
        check(bttv.ResUtil.localeCalls==0,"disabled setting did not exit before formatting");

        bttv.ResUtil.enabled=true;
        Spanned result=DeletedMessages.reSpan(original);
        check("hello🙂(removed)".contentEquals(result),"classic suffix or no-space behavior changed");
        check(result.getSpanStart(nativeTwitchEmoteSpan)==0&&result.getSpanEnd(nativeTwitchEmoteSpan)==1,
            "native Twitch emote-like span was not preserved");
        check(result.getSpanStart(thirdPartyEmoteSpan)==1&&result.getSpanEnd(thirdPartyEmoteSpan)==2,
            "third-party emote-like span was not preserved");
        check(result.getSpanStart(clickableSpan)==2&&result.getSpanEnd(clickableSpan)==3,
            "clickable span was not preserved");
        check(result.getSpanStart(linkSpan)==3&&result.getSpanEnd(linkSpan)==5,
            "link span was not preserved");
        check(result.getSpanStart(existingStyleSpan)==5&&result.getSpanEnd(existingStyleSpan)==7,
            "existing style-like original span was not preserved");
        StyleSpan[] styles=result.getSpans(7,result.length(),StyleSpan.class);
        check(styles.length==1,"italic suffix span missing");
        check(styles[0].getStyle()==Typeface.ITALIC,"suffix is not italic");
        check(result.getSpanStart(styles[0])==7&&result.getSpanEnd(styles[0])==16,
            "italic span escaped suffix");
        check(bttv.ResUtil.localeCalls==1,"localized removed marker was not used exactly once");

        Spanned retainedOriginal=original;
        Spanned rebound=DeletedMessages.reSpan(retainedOriginal);
        check("hello🙂".contentEquals(retainedOriginal),"retained original was mutated");
        check("hello🙂(removed)".contentEquals(rebound),"rebind did not derive from untouched retained original");
        check(!rebound.toString().contains("(removed)(removed)"),"rebind duplicated removed suffix");

        SpannableString body=new SpannableString("hello");
        Object bodySpan=new Object(); body.setSpan(bodySpan,0,5,17);
        Spanned restoredBody=DeletedMessages.reSpan(body);
        SpannableString container=new SpannableString("[badge] User: <message deleted>!");
        Object badgeSpan=new Object(); container.setSpan(badgeSpan,0,7,17);
        ClickableSpan authorSpan=new ClickableSpan(); container.setSpan(authorSpan,8,12,33);
        ForegroundColorSpan authorColor=new ForegroundColorSpan(0x123456); container.setSpan(authorColor,8,12,33);
        Object placeholder=new Object(); container.setSpan(placeholder,14,31,33);
        Object suffixSpan=new Object(); container.setSpan(suffixSpan,31,32,17);
        Object crossingSpan=new Object(); container.setSpan(crossingSpan,0,32,17);
        Spanned restored=DeletedMessages.restoreNativeContainer(container,placeholder,restoredBody);
        check("[badge] User: hello(removed)!".contentEquals(restored),"native container body replacement failed");
        check(restored.getSpanStart(badgeSpan)==0&&restored.getSpanEnd(badgeSpan)==7,"native badge span moved");
        check(restored.getSpanStart(authorSpan)==8&&restored.getSpanEnd(authorSpan)==12,"native author click span moved");
        check(restored.getSpanStart(authorColor)==8&&restored.getSpanEnd(authorColor)==12,"native author color span moved");
        check(restored.getSpanStart(placeholder)==-1,"deleted placeholder span survived replacement");
        check(restored.getSpanStart(bodySpan)==14&&restored.getSpanEnd(bodySpan)==19,"restored body spans were not inserted");
        check(restored.getSpanStart(suffixSpan)==28&&restored.getSpanEnd(suffixSpan)==29,"native suffix span was not retained");
        check(restored.getSpanStart(crossingSpan)==0&&restored.getSpanEnd(crossingSpan)==29,"crossing native span was not retained");
        check("[badge] User: <message deleted>!".contentEquals(container),"native container was mutated");

        Object missingPlaceholder=new Object();
        check(DeletedMessages.restoreNativeContainer(container,missingPlaceholder,restoredBody)==container,
            "missing placeholder did not preserve original native container");
        Object zeroLengthPlaceholder=new Object(); container.setSpan(zeroLengthPlaceholder,14,14,33);
        check(DeletedMessages.restoreNativeContainer(container,zeroLengthPlaceholder,restoredBody)==container,
            "zero-length placeholder did not preserve original native container");
        Object outOfBoundsPlaceholder=new Object(); container.setSpan(outOfBoundsPlaceholder,14,container.length()+1,33);
        check(DeletedMessages.restoreNativeContainer(container,outOfBoundsPlaceholder,restoredBody)==container,
            "out-of-bounds placeholder did not preserve original native container");
        check(DeletedMessages.restoreNativeContainer(container,null,restoredBody)==container,
            "null placeholder did not preserve original native container");
        check(DeletedMessages.restoreNativeContainer(container,placeholder,null)==container,
            "null restored body did not preserve original native container");
        check(DeletedMessages.restoreNativeContainer(null,placeholder,restoredBody)==null,
            "null container did not return null safely");

        bttv.ResUtil.enabled=false;
        bttv.ResUtil.localeCalls=0;
        check(DeletedMessages.restoreSpecialContainer(container,placeholder,body)==container,
            "disabled special-row restore did not preserve exact native container");
        check(bttv.ResUtil.localeCalls==0,"disabled special-row restore formatted a suffix");

        bttv.ResUtil.enabled=true;
        Spanned special=DeletedMessages.restoreSpecialContainer(container,placeholder,body);
        check("[badge] User: hello(removed)!".contentEquals(special),
            "special-row body replacement failed");
        check(special.getSpanStart(badgeSpan)==0&&special.getSpanEnd(badgeSpan)==7,
            "special-row restore moved native badge span");
        check(special.getSpanStart(authorSpan)==8&&special.getSpanEnd(authorSpan)==12,
            "special-row restore moved native author click span");
        check(special.getSpanStart(authorColor)==8&&special.getSpanEnd(authorColor)==12,
            "special-row restore moved native author color span");
        check(special.getSpanStart(bodySpan)==14&&special.getSpanEnd(bodySpan)==19,
            "special-row restore lost body spans");
        check("[badge] User: <message deleted>!".contentEquals(container),
            "special-row restore mutated native container");
        check("hello".contentEquals(body),"special-row restore mutated retained original body");
        check(DeletedMessages.restoreSpecialContainer(container,missingPlaceholder,body)==container,
            "special-row missing placeholder did not fail open to exact native container");
        check(DeletedMessages.restoreSpecialContainer(container,null,body)==container,
            "special-row null placeholder did not fail open");
        check(DeletedMessages.restoreSpecialContainer(container,placeholder,null)==container,
            "special-row null body did not fail open");
        check(DeletedMessages.restoreSpecialContainer(null,placeholder,body)==null,
            "special-row null container did not return null safely");
        System.out.println("PASS DeletedMessages disabled/enabled/spans/unicode/idempotent-retained-original/no-space/italic suffix");
    }
}
""",
}


if __name__ == "__main__":
    output = Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        target = output / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding="utf-8")
    print(f"Generated {len(SOURCES)} isolated DeletedMessages JVM fixtures in {output}")
