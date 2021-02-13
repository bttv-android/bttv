/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the actual patch.
 */
package okhttp3;

public interface Call {
    public Response execute();

    void enqueue(okhttp3.Callback callback);
}
