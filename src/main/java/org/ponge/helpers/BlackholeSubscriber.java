package org.ponge.helpers;

import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public final class BlackholeSubscriber<T> implements Subscriber<T> {

    private final Blackhole blackhole;
    private final long requestCount;
    private volatile Subscription subscription;

    public BlackholeSubscriber(Blackhole blackhole, long requestCount) {
        this.blackhole = blackhole;
        this.requestCount = requestCount;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (this.subscription != null) {
            subscription.cancel();
        } else {
            this.subscription = subscription;
            subscription.request(requestCount);
        }
    }

    @Override
    public void onNext(T item) {
        if (item == null) {
            throw new NullPointerException();
        }
        blackhole.consume(item);
    }

    @Override
    public void onError(Throwable err) {
        if (err == null) {
            throw new NullPointerException();
        }
        blackhole.consume(err);
    }

    @Override
    public void onComplete() {
        blackhole.consume("Done");
    }
}
