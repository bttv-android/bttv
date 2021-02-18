package okio;

public abstract class ForwardingSource implements Source {
    public ForwardingSource(okio.Source source) {

    }

    @Override
    public long read(okio.Buffer buffer, long j) throws java.io.IOException {
        return 0L;
    }
}
