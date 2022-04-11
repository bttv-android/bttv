package tv.twitch.android.core.mvp.viewdelegate;

import io.reactivex.Flowable;

public abstract class RxViewDelegate<S extends ViewDelegateState, E extends ViewDelegateEvent> extends BaseViewDelegate implements IEventDispatcher<E> {
    @Override
    public Flowable<E> eventObserver() {
        return null;
    }

    @Override
    public void pushEvent(E e) {

    }
}
