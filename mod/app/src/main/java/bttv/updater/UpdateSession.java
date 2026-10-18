package bttv.updater;

/** Real coordinator state machine, independent of views and test clocks. */
final class UpdateSession {
    enum State { IDLE, CHECKING, NONE, AVAILABLE, DOWNLOADING, VERIFYING, READY, ERROR, CANCELLED }
    static final class Snapshot {
        final State state;
        final long generation, bytes, total;
        final String message;
        final UpdateRelease release;
        final java.io.File file;
        Snapshot(State state,long generation,long bytes,long total,String message,UpdateRelease release,java.io.File file) {
            this.state=state; this.generation=generation; this.bytes=bytes; this.total=total;
            this.message=message; this.release=release; this.file=file;
        }
    }
    private long generation, owner, nextCheck;
    private boolean running;
    private Snapshot value=new Snapshot(State.IDLE,0,0,0,"Select Check to request official release information.",null,null);
    synchronized Snapshot snapshot() { return value; }
    synchronized boolean running() { return running; }
    synchronized long begin(State state,long now) {
        if(running || (state==State.CHECKING && now<nextCheck))return -1;
        if(state==State.DOWNLOADING && value.state!=State.AVAILABLE)return -1;
        if(state==State.VERIFYING && value.state!=State.READY)return -1;
        if(state==State.CHECKING) nextCheck=now+30000;
        running=true; owner=++generation;
        value=new Snapshot(state,generation,0,value.total,"Working…",state==State.CHECKING?null:value.release,state==State.CHECKING?null:value.file);
        return owner;
    }
    synchronized boolean progress(long id,long bytes,long total) {
        if(id!=generation || !running)return false;
        value=new Snapshot(value.state,generation,bytes,total,"Downloading…",value.release,value.file); return true;
    }
    synchronized boolean finish(long id,State state,String message,UpdateRelease release,java.io.File file) {
        if(id==owner) running=false;
        if(id!=generation)return false;
        value=new Snapshot(state,generation,value.bytes,value.total,message,release,file); return true;
    }
    synchronized void cancel() {
        ++generation;
        value=new Snapshot(State.CANCELLED,generation,0,0,"Cancelled. No installer was opened.",null,null);
    }
    synchronized void backoff(long until) { nextCheck=Math.max(nextCheck,until); }
}
