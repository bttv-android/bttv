package okio;

public interface BufferedSink extends okio.Sink, java.nio.channels.WritableByteChannel {
    long writeAll(okio.Source source) throws java.io.IOException;
}
