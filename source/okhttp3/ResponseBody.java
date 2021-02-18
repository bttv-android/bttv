/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the actual patch.
 */
package okhttp3;

import java.io.IOException;
import java.io.InputStream;

import okio.BufferedSource;

public abstract class ResponseBody implements java.io.Closeable {
    public String string() {
        return "";
    }

    public InputStream byteStream() {
        return null;
    }

    public abstract BufferedSource source();

    @Override
    public void close() throws IOException {
    }

    public abstract long contentLength();

    public abstract okhttp3.MediaType contentType();

}
