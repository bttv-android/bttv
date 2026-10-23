package bttv.updater;

/** Production automatic-check policy. No Android view, networking, download or installer. */
final class AutoUpdateState {
    static final String ENABLED="bttv_auto_update_checks_enabled";
    static final String SUCCESS="bttv_updater_auto_success_at";
    static final String ATTEMPT="bttv_updater_auto_attempt_at";
    static final String RETRY="bttv_updater_auto_retry_at";
    static final String NOTIFIED="bttv_updater_auto_notified_code";
    static final long DAY=24*60*60*1000L, BACKOFF=60*60*1000L;
    interface Store {
        boolean enabled();
        long get(String key);
        void checked(long success,long attempt,long retry);
        void notified(long code);
    }
    private final Store store;
    private long generation,flight=-1;
    private long success,attempt,retryAt,notified;
    private UpdateRelease pending;

    AutoUpdateState(Store store) {
        this.store=store;
        success=store.get(SUCCESS); attempt=store.get(ATTEMPT);
        retryAt=store.get(RETRY); notified=store.get(NOTIFIED);
    }
    synchronized long begin(long now) {
        if(!store.enabled() || flight>=0)return -1;
        // A clock rollback must not permanently disable checks; completion anchors the new clock.
        if(now>=attempt && now<retryAt)return -1;
        if(success>0 && now>=success && now-success<DAY)return -1;
        return flight=++generation;
    }
    synchronized void abort(long ticket) { if(flight==ticket)flight=-1; }
    synchronized void disabled() { ++generation; flight=-1; pending=null; }
    synchronized boolean complete(long ticket,long now,UpdateRelease.Outcome outcome,UpdateRelease release,long retry) {
        if(flight!=ticket)return false;
        flight=-1;
        if(!store.enabled()) { pending=null; return false; }
        switch(outcome) {
            case AVAILABLE:
            case NOT_NEWER:
            case UNSUPPORTED:
            case MISSING_ASSET:
                success=now; attempt=now; retryAt=0;
                pending=outcome==UpdateRelease.Outcome.AVAILABLE?release:null;
                store.checked(success,attempt,retryAt);
                return true;
            case CANCELLED:
            case DISABLED:
                return false;
            default:
                attempt=now; retryAt=Math.max(now+BACKOFF,retry);
                store.checked(success,attempt,retryAt);
                return false;
        }
    }
    synchronized UpdateRelease notice() {
        return store.enabled() && pending!=null && pending.code>notified?pending:null;
    }
    synchronized boolean notified(UpdateRelease release) {
        if(release==null || notice()!=release)return false;
        notified=release.code;
        pending=null;
        store.notified(notified);
        return true;
    }
}
