package org.ponge.helpers;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class BlackholeSingleOperationSubscriber<T> implements UniSubscriber<T>, SingleObserver<T>, Subscriber<T> {

    private final Blackhole blackhole;

    public BlackholeSingleOperationSubscriber(Blackhole blackhole) {
        this.blackhole = blackhole;
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Override
    public void onSubscribe(UniSubscription subscription) {
        blackhole.consume(subscription);
    }

    @Override
    public void onItem(T item) {
        blackhole.consume(item);
    }

    @Override
    public void onFailure(Throwable failure) {
        blackhole.consume(failure);
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Override
    public void onSubscribe(@NonNull Disposable d) {
        blackhole.consume(d);
    }

    @Override
    public void onSuccess(@NonNull T t) {
        blackhole.consume(t);
    }

    @Override
    public void onError(@NonNull Throwable e) {
        blackhole.consume(e);
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Override
    public void onSubscribe(Subscription s) {
        blackhole.consume(s);
    }

    @Override
    public void onNext(T t) {
        blackhole.consume(t);
    }

    @Override
    public void onComplete() {
        // nothing
    }

    // -------------------------------------------------------------------------------------------------------------- //
}
