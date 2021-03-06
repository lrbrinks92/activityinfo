package org.activityinfo.observable;


public class Connection<T> {
    private final Observable<T> observable;
    private final Subscription subscription;


    Connection(Observable<T> observable) {
        this.observable = observable;
        this.subscription = observable.subscribe(this::onChanged);
    }

    private void onChanged(Observable<T> observable) {

    }

    public void assertLoading() {
        if (!observable.isLoading()) {
            throw new AssertionError("Expected observable to be loading");
        }
    }

    public T assertLoaded() {
        if (!observable.isLoaded()) {
            throw new AssertionError("Expected observable to have loaded");
        }
        return observable.get();
    }

    public void disconnect() {
        subscription.unsubscribe();
    }
}
