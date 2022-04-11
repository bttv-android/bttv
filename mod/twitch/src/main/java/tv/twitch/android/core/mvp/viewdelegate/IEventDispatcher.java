package tv.twitch.android.core.mvp.viewdelegate;

public interface IEventDispatcher<E> {
    io.reactivex.Flowable<E> eventObserver();
    void pushEvent(E e);
}
