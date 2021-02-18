/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the actual patch.
 */
package okhttp3;

import java.util.concurrent.TimeUnit;

public class OkHttpClient {

    public static class Builder {

        public final okhttp3.OkHttpClient.Builder connectTimeout(long j, java.util.concurrent.TimeUnit timeUnit) {
            return this;
        }

        public Builder readTimeout(long i, TimeUnit u) {
            return this;
        }

        public Builder addNetworkInterceptor(Interceptor interceptor) {
            return this;
        }

        public OkHttpClient build() {
            return null;
        }

    }

    public Call newCall(Request req) {
        return null;
    }

}
