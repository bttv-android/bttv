package bttv;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Talks to the react native NetworkingModule using reflection,
 * only names that are not obfuscated are used.
 */
public class RNNetwork {

    /** headers of a request, as [name, value] pairs */
    public static List<String[]> readHeaders(Object readableArray) throws Exception {
        List<String[]> headers = new ArrayList<>();
        if (readableArray == null) {
            return headers;
        }
        Class<?> cls = Class.forName("com.facebook.react.bridge.ReadableArray");
        Method size = cls.getMethod("size");
        Method getArray = cls.getMethod("getArray", int.class);
        Method getString = cls.getMethod("getString", int.class);
        int count = (Integer) size.invoke(readableArray);
        for (int i = 0; i < count; i++) {
            Object pair = getArray.invoke(readableArray, i);
            if (pair == null || (Integer) size.invoke(pair) != 2) {
                continue;
            }
            headers.add(new String[]{(String) getString.invoke(pair, 0), (String) getString.invoke(pair, 1)});
        }
        return headers;
    }

    /** body of a request, null if it is not a plain string */
    public static String readBody(Object readableMap) throws Exception {
        if (readableMap == null) {
            return null;
        }
        Class<?> cls = Class.forName("com.facebook.react.bridge.ReadableMap");
        if (!(Boolean) cls.getMethod("hasKey", String.class).invoke(readableMap, "string")) {
            return null;
        }
        return (String) cls.getMethod("getString", String.class).invoke(readableMap, "string");
    }

    /** sends the same events the NetworkingModule sends for a real response */
    public static void emitResponse(Object module, int requestId, int status, Map<String, String> headers, String url, String body) throws Exception {
        Object context = module.getClass().getMethod("getReactApplicationContext").invoke(module);
        Method emit = context.getClass().getMethod("emitDeviceEvent", String.class, Object.class);
        Class<?> arguments = Class.forName("com.facebook.react.bridge.Arguments");
        Method createArray = arguments.getMethod("createArray");
        Method createMap = arguments.getMethod("createMap");
        Class<?> readableMap = Class.forName("com.facebook.react.bridge.ReadableMap");

        Object headerMap = createMap.invoke(null);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            call(headerMap, "putString", new Class<?>[]{String.class, String.class}, header.getKey(), header.getValue());
        }

        Object response = createArray.invoke(null);
        call(response, "pushInt", new Class<?>[]{int.class}, requestId);
        call(response, "pushInt", new Class<?>[]{int.class}, status);
        call(response, "pushMap", new Class<?>[]{readableMap}, headerMap);
        call(response, "pushString", new Class<?>[]{String.class}, url);
        emit.invoke(context, "didReceiveNetworkResponse", response);

        // fetch() requests the body as a blob
        Class<?> blobModuleClass = Class.forName("com.facebook.react.modules.blob.BlobModule");
        Object blobModule = context.getClass().getMethod("getNativeModule", Class.class).invoke(context, blobModuleClass);
        byte[] bytes = body.getBytes("UTF-8");
        String blobId = (String) blobModuleClass.getMethod("store", byte[].class).invoke(blobModule, (Object) bytes);

        Object blob = createMap.invoke(null);
        call(blob, "putString", new Class<?>[]{String.class, String.class}, "blobId", blobId);
        call(blob, "putInt", new Class<?>[]{String.class, int.class}, "offset", 0);
        call(blob, "putInt", new Class<?>[]{String.class, int.class}, "size", bytes.length);

        Object data = createArray.invoke(null);
        call(data, "pushInt", new Class<?>[]{int.class}, requestId);
        call(data, "pushMap", new Class<?>[]{readableMap}, blob);
        emit.invoke(context, "didReceiveNetworkData", data);

        Object complete = createArray.invoke(null);
        call(complete, "pushInt", new Class<?>[]{int.class}, requestId);
        call(complete, "pushNull", new Class<?>[]{});
        emit.invoke(context, "didCompleteNetworkResponse", complete);
    }

    private static void call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getMethod(name, types);
        method.setAccessible(true);
        method.invoke(target, args);
    }
}
