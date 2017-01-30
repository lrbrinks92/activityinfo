package org.activityinfo.observable;

import com.google.common.base.Function;

import java.util.ArrayList;
import java.util.List;


/**
 * A list of items whose composition can be externally observed.
 * <p>
 * <p>Once subscribed, a {@link ListObserver} will be fired when items are added, removed from the list,
 * or when the composition of the list changes completely.
 * <p>
 * <p>Note that changes to the items themselves are not broadcast to {@code ListObserver}s.</p>
 *
 * @param <T>
 */
public abstract class ObservableList<T> {
    private final List<ListObserver<T>> observers = new ArrayList<>();

    public abstract boolean isLoading();

    public final Subscription subscribe(final ListObserver<T> observer) {
        if (observers.isEmpty()) {
            onConnect();
        }
        observer.onChange();

        observers.add(observer);

        return new Subscription() {
            @Override
            public void unsubscribe() {
                observers.remove(observer);
                if (observers.isEmpty()) {
                    onDisconnect();
                }
            }
        };
    }


    protected void onConnect() {

    }

    protected void onDisconnect() {

    }


    /**
     * Signal that the list has changed.
     */
    protected final void fireChanged() {
        for (ListObserver<T> observer : observers) {
            observer.onChange();
        }
    }


    /**
     * Signal that the given {@code element} has been added to the list
     */
    protected final void fireAdded(T element) {
        for (ListObserver<T> observer : observers) {
            observer.onElementAdded(element);
        }
    }

    /**
     * Signal that the given {@code element} has been removed from the list
     */
    protected final void fireRemoved(T element) {
        for (ListObserver<T> observer : observers) {
            observer.onElementRemoved(element);
        }
    }

    public final <R> ObservableList<R> map(final Function<T, R> function) {
        return new ObservableListMap<>(this, function);
    }

    /**
     * @return the loaded list of items.
     * @throws AssertionError if the list is still loading.
     */
    public abstract List<T> getList();

    public final Observable<List<T>> asObservable() {
        return new Observable<List<T>>() {

            private Subscription subscription;

            @Override
            public boolean isLoading() {
                return ObservableList.this.isLoading();
            }

            @Override
            public List<T> get() {
                return ObservableList.this.getList();
            }

            @Override
            protected void onConnect() {
                final Observable<List<T>> thisObservable = this;
                subscription = ObservableList.this.subscribe(new ListObserver<T>() {
                    @Override
                    public void onChange() {
                        thisObservable.fireChange();
                    }

                    @Override
                    public void onElementAdded(T element) {
                        thisObservable.fireChange();
                    }

                    @Override
                    public void onElementRemoved(T element) {
                        thisObservable.fireChange();
                    }
                });
            }

            @Override
            protected void onDisconnect() {
                subscription.unsubscribe();
            }
        };
    }
}
