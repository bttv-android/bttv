package dagger.android;

public interface AndroidInjector<T> {
    public interface Factory<T> {
        AndroidInjector<T> create(T t);
    }

    void inject(T t);

}
