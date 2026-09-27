package bttvcompat;

import java.util.List;
import android.widget.EditText;

/** Compile-only ABI. Normal DEX implementation is supplied by the classes7 smali patch. */
public final class NativePickerBridge {
    private NativePickerBridge() {}
    public static native io.reactivex.f wrap(Object context, io.reactivex.f upstream);
    public static native String pickerUrl(Object cell);
    public static native boolean pickerClick(Object presenter, Object clicked);
    public static native boolean pickerInsert(Object composer, String text);
    // classes7-only adapters; never called by near-full Twitch DEX files.
    public static native Object presenter(Object context);
    public static native String channel(Object context);
    public static native String id(Object cellOrClick);
    public static native String code(Object cell);
    public static native List<?> sections(Object pair);
    public static native List<?> cells(Object section);
    public static native boolean frequent(Object section);
    public static native Object copySection(Object section, List<?> cells);
    public static native Object pair(Object original, List<?> sections);
    public static native Object cell(String id, String code, boolean animated);
    public static native Object section(int title, boolean channel, List<?> cells);
    public static native void dispatch(Object presenter, String text, String id);
    public static native EditText input(Object composer);
    public static native void subscribe(Object upstream, Object emitter, Object stream);
    public static native void cancel(Object subscription);
    public static native void request(Object subscription);
    public static native void emit(Object emitter, Object pair);
    public static native void error(Object emitter, Throwable error);
    public static native void complete(Object emitter);
}
