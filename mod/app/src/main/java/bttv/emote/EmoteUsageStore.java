package bttv.emote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import java.util.*;
import java.util.concurrent.*;
import bttv.Data;
import bttv.ResUtil;
import bttv.settings.Settings;

/** One serial, bounded, fail-open local writer. No message text crosses its queue. */
public final class EmoteUsageStore {
    interface Backend { String read(); boolean write(String text); }
    static final class Worker {
        final Backend backend;
        volatile EmoteUsageHistory.State state;
        final Runnable changed;
        Worker(Backend backend,Runnable changed) { this.backend=backend;this.changed=changed; }
        void load() {
            try { state=EmoteUsageHistory.decode(backend.read()); }
            catch(Throwable unavailable){ state=EmoteUsageHistory.empty(true); }
            notifyChanged();
        }
        void record(String viewer,Map<EmoteUsageHistory.Binding,Integer> occurrences) {
            try {
                if(state==null)return;
                EmoteUsageHistory.State next=EmoteUsageHistory.add(state,viewer,occurrences);
                if(next==state)return;
                state=next;
                // Failure leaves bounded memory intact; the next real use retries the complete state.
                backend.write(EmoteUsageHistory.encode(next));
            } catch(Throwable unavailable) { /* Local history must never affect Twitch. */ }
            notifyChanged();
        }
        private void notifyChanged(){try{changed.run();}catch(Throwable ignored){}}
    }
    private static final CopyOnWriteArrayList<Runnable> LISTENERS=new CopyOnWriteArrayList<>();
    private static EmoteUsageStore instance;
    private final Worker worker;
    private final ThreadPoolExecutor queue;
    private EmoteUsageStore(Context context) {
        Context app=context.getApplicationContext();
        queue=new ThreadPoolExecutor(0,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(64),r->{
            Thread t=new Thread(r,"bttv-emote-usage");t.setDaemon(true);return t;
        });
        worker=new Worker(new Backend() {
            private SharedPreferences prefs;
            private SharedPreferences prefs(){
                if(prefs==null)prefs=app.getSharedPreferences("BTTV_EmoteUsage",Context.MODE_PRIVATE);
                return prefs;
            }
            public String read(){return prefs().getString("state_v1",null);}
            public boolean write(String value){return prefs().edit().putString("state_v1",value).commit();}
        },()->{
            new Handler(Looper.getMainLooper()).post(()->{
                for(Runnable listener:LISTENERS)try{listener.run();}catch(Throwable ignored){}
            });
        });
        queue.execute(worker::load);
    }
    private static synchronized EmoteUsageStore get() {
        if(instance==null && Data.ctx!=null)instance=new EmoteUsageStore(Data.ctx);
        return instance;
    }
    static EmoteCatalogEvents.Subscription observe(Runnable listener) {
        try { get();LISTENERS.add(listener); }catch(Throwable ignored){}
        return ()->LISTENERS.remove(listener);
    }
    static List<LiveEmotePicker.Entry> visible(String viewer,String channel,List<LiveEmotePicker.Entry> winners) {
        try {
            EmoteUsageStore store=get();
            if(store==null || store.worker.state==null)return Collections.emptyList();
            return EmoteUsageHistory.visible(store.worker.state,viewer,channel,winners);
        }catch(Throwable ignored){return Collections.emptyList();}
    }
    /** Called ONLY by the approved fbj.B DEX7 ABI bridge, after ordinary forwarding selection. */
    public static void submitted(EditText input,String viewer,String channel,String text) {
        try {
            if(Looper.myLooper()!=Looper.getMainLooper() || !PickerSyntheticId.positive(viewer)
                    || !PickerSyntheticId.positive(channel) || text==null || text.isEmpty() || text.length()>4096
                    || text.trim().startsWith("/") || ResUtil.getBooleanFromSettings(Settings.AnonChatEnabled))return;
            Set<String> nativeCodes=PickerRuntime.usageNativeCodes(input,viewer,channel);
            if(nativeCodes==null)return; // Unknown native eligibility: do not guess a provider.
            List<LiveEmotePicker.Entry> matches=LiveEmotes.usageLoaded(text,channel,nativeCodes);
            Map<EmoteUsageHistory.Binding,Integer> delta=new LinkedHashMap<>();
            for(LiveEmotePicker.Entry e:matches) {
                EmoteUsageHistory.Binding b=EmoteUsageHistory.Binding.of(e,channel);
                delta.put(b,delta.getOrDefault(b,0)+1);
            }
            if(delta.isEmpty())return;
            EmoteUsageStore store=get();
            if(store!=null)store.queue.execute(()->store.worker.record(viewer,delta));
        }catch(Throwable ignored){ /* Includes initialization, queue rejection and settings failures. */ }
    }
}
