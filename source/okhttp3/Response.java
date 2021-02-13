/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the actual patch.
 */
package okhttp3;

import java.io.IOException;

public class Response implements java.io.Closeable {

    @Override
    public void close() throws IOException {
    }

    public ResponseBody body() {
        return new ResponseBody();
    }

    public boolean isSuccessful() {
        return false;
    }

}
