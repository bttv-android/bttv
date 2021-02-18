package okhttp3;

public interface Interceptor {

    public interface Chain {
        int connectTimeoutMillis();

        okhttp3.Response proceed(okhttp3.Request request) throws java.io.IOException;

        int readTimeoutMillis();

        okhttp3.Request request();

        int writeTimeoutMillis();
    }

    okhttp3.Response intercept(okhttp3.Interceptor.Chain chain) throws java.io.IOException;

}
