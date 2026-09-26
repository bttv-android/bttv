package bttvcompat;

import java.util.ArrayList;
import java.util.Set;

/**
 * Compile-only declarations, not JNI. buildsource does not inject this stub package.
 * The matching new-file smali patch supplies normal classes7 DEX methods against the
 * verified Twitch 31.2.0 ABI, without exposing obfuscated types to Java compilation.
 */
public final class NativeAutocompleteBridge {
    private NativeAutocompleteBridge() {}

    public static native boolean isEmoteAdapter(Object adapter);
    public static native Object create(String id, String code, boolean animated);
    public static native String code(Object item);
    public static native String id(Object item);
    public static native Object values(Object results);
    public static native int count(Object results);
    public static native void publish(Object results, ArrayList<Object> values);
    public static native void removeItems(Object adapter, Set<Object> items);
}
