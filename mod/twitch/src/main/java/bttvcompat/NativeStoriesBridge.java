package bttvcompat;

/** Compile-only ABI; the matching classes7 smali patch supplies normal DEX code. */
public final class NativeStoriesBridge {
    private NativeStoriesBridge() {}
    public static native void rebind(Object owner, Object originalState);
}
