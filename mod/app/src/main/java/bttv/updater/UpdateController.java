package bttv.updater;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** Shared manual/automatic coordinator; one bounded worker and no owned Activity. */
final class UpdateController {
    interface Observer { void changed(UpdateSession.Snapshot state); }
    interface AutomaticCheck {
        boolean enabled();
        void finished(long id,UpdateRelease.Outcome outcome,UpdateRelease release,long retryAt);
    }
    private static UpdateController instance;
    static synchronized UpdateController get(Context context) {
        if(instance==null)instance=new UpdateController(context.getApplicationContext());
        return instance;
    }
    final Context context;
    final UpdateSession session=new UpdateSession();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<WeakReference<Observer>> observers=new ArrayList<>();
    private final ThreadPoolExecutor worker;
    private final UpdateTransfer.Connections connections;
    private volatile UpdateTransfer.Cancel cancel;
    private boolean cleaned;
    private long automaticGeneration=-1;
    private boolean automaticSelection;
    private UpdateController(Context context) {
        this(context,UpdateTransfer.HTTPS);
    }
    UpdateController(Context context,UpdateTransfer.Connections connections) {
        this.context=context;
        this.connections=connections;
        worker=new ThreadPoolExecutor(1,1,15,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1),r->new Thread(r,"bttv-manual-update"));
        worker.allowCoreThreadTimeOut(true);
    }
    synchronized void observe(Observer observer) {
        remove(observer); observers.add(new WeakReference<>(observer)); observer.changed(session.snapshot());
    }
    synchronized void remove(Observer observer) { observers.removeIf(ref->ref.get()==null || ref.get()==observer); }
    private void emit() {
        main.post(()-> {
            List<Observer> copy=new ArrayList<>();
            synchronized(UpdateController.this) {
                observers.removeIf(ref->ref.get()==null);
                for(WeakReference<Observer> ref:observers) { Observer o=ref.get(); if(o!=null)copy.add(o); }
            }
            UpdateSession.Snapshot state=session.snapshot();
            for(Observer o:copy)o.changed(state);
        });
    }
    void check() { start(UpdateSession.State.CHECKING); }
    long checkAutomatically(AutomaticCheck completion) {
        return start(UpdateSession.State.CHECKING,completion);
    }
    synchronized boolean canNotify(UpdateRelease release) {
        return !hasManualUi() && !session.running() && session.snapshot().state==UpdateSession.State.AVAILABLE
                && session.snapshot().release==release;
    }
    private boolean hasManualUi() {
        observers.removeIf(ref->ref.get()==null);
        return !observers.isEmpty();
    }
    synchronized void cancelAutomatic() {
        if(automaticGeneration>=0 && session.snapshot().generation==automaticGeneration && session.running())cancel();
    }
    void download() { start(UpdateSession.State.DOWNLOADING); }
    long prepareInstall() { return start(UpdateSession.State.VERIFYING); }
    void cancel() {
        UpdateTransfer.Cancel operation=cancel;
        session.cancel(); if(operation!=null)operation.cancel(); emit();
    }
    private long start(UpdateSession.State action) { return start(action,null); }
    private synchronized long start(UpdateSession.State action,AutomaticCheck automatic) {
        if(automatic!=null && (!automatic.enabled() || hasManualUi()
                || session.snapshot().state==UpdateSession.State.READY
                || (session.snapshot().state==UpdateSession.State.AVAILABLE && !automaticSelection)))return -1;
        long id=session.begin(action,System.currentTimeMillis());
        if(id<0) { emit(); return -1; }
        UpdateTransfer.Cancel token=new UpdateTransfer.Cancel(); cancel=token;
        automaticGeneration=automatic==null?-1:id;
        if(action==UpdateSession.State.CHECKING)automaticSelection=automatic!=null;
        UpdateSession.Snapshot selected=session.snapshot(); emit();
        worker.execute(()-> {
            File downloaded=null;
            UpdateRelease.Outcome outcome=UpdateRelease.Outcome.INTERNAL_ERROR;
            UpdateRelease candidate=null;
            long retryAt=0;
            try {
                File directory=automatic==null?new File(context.getCacheDir(),"bttv-updater"):null;
                if(automatic==null && (!cleaned || action==UpdateSession.State.CHECKING)) {
                    File[] files=directory.listFiles();
                    if(files!=null)for(File f:files)if(f.isFile() && f.getName().matches("[0-9]+-[0-9A-F]{64}\\.(part|apk)"))
                        UpdateRelease.require(f.delete(),"Cannot clean stale updater file");
                    cleaned=true;
                }
                token.check();
                if(action==UpdateSession.State.CHECKING) {
                    UpdateTransfer.Connections source=automatic==null?connections:url->{
                        token.check();
                        if(!automatic.enabled())throw new IOException("Automatic checking disabled");
                        return connections.open(url);
                    };
                    UpdateRelease.Inspection result=UpdateRelease.inspect(UpdateTransfer.metadata(token,source),UpdateVerifier.installed(context));
                    outcome=result.outcome; candidate=result.candidate;
                    token.check(); session.finish(id,result.candidate==null?UpdateSession.State.NONE:UpdateSession.State.AVAILABLE,result.message,result.candidate,null);
                } else {
                    UpdateRelease release=selected.release;
                    if(action==UpdateSession.State.DOWNLOADING) {
                        final long[] last={0};
                        downloaded=UpdateTransfer.download(release,directory,token,(bytes,total)-> {
                            long now=android.os.SystemClock.elapsedRealtime();
                            if(now-last[0]>=250 || bytes==total) { last[0]=now; if(session.progress(id,bytes,total))emit(); }
                        },connections);
                    } else downloaded=selected.file;
                    UpdateVerifier.verify(context,release,downloaded,token);
                    token.check(); session.finish(id,UpdateSession.State.READY,"APK verified. Installation still requires your approval.",release,downloaded);
                }
            } catch(Exception failure) {
                if(downloaded!=null && downloaded.isFile())downloaded.delete();
                if(failure instanceof UpdateTransfer.HttpFailure)session.backoff(((UpdateTransfer.HttpFailure)failure).retryAt);
                if(automatic!=null && !automatic.enabled())outcome=UpdateRelease.Outcome.DISABLED;
                else if(session.snapshot().state==UpdateSession.State.CANCELLED)outcome=UpdateRelease.Outcome.CANCELLED;
                else if(failure instanceof UpdateTransfer.HttpFailure) {
                    UpdateTransfer.HttpFailure http=(UpdateTransfer.HttpFailure)failure;
                    retryAt=http.retryAt;
                    outcome=http.rateLimited?UpdateRelease.Outcome.RATE_LIMIT:
                            http.status>=500?UpdateRelease.Outcome.SERVER:UpdateRelease.Outcome.HTTP_ERROR;
                } else if(failure instanceof UpdateRelease.Problem)outcome=((UpdateRelease.Problem)failure).outcome;
                else if(failure instanceof IOException)outcome=UpdateRelease.Outcome.NETWORK;
                String message;
                if(failure instanceof java.net.UnknownHostException)message="Cannot resolve the official server. Check your connection.";
                else if(failure instanceof java.net.SocketTimeoutException)message="The official server timed out. Please retry later.";
                else if(failure instanceof javax.net.ssl.SSLException)message="A secure connection could not be established.";
                else if(failure instanceof UpdateRelease.Problem || failure instanceof UpdateTransfer.HttpFailure)message=failure.getMessage();
                else if(failure instanceof IOException)message="Network or file access failed. Check your connection and available storage.";
                else message="Update verification failed. No installer was opened.";
                // Only locally constructed policy/HTTP messages are displayed; never server bodies or URLs.
                if(message==null || message.length()>240)message="Update failed. Check your connection and retry later.";
                session.finish(id,UpdateSession.State.ERROR,message,null,null);
            } finally {
                synchronized(UpdateController.this) { if(automaticGeneration==id)automaticGeneration=-1; }
                emit();
                if(automatic!=null) {
                    final UpdateRelease.Outcome completed=outcome;
                    final UpdateRelease available=candidate;
                    final long retry=retryAt;
                    main.post(()->automatic.finished(id,completed,available,retry));
                }
            }
        });
        return id;
    }
}
