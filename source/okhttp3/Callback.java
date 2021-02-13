/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the actual patch.
 */
package okhttp3;

public interface Callback {
    void onFailure(okhttp3.Call call, java.io.IOException iOException);

    void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException;
}
