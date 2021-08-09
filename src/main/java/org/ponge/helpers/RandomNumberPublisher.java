package org.ponge.helpers;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public final class RandomNumberPublisher implements Publisher<Long> {

    private final long maxElements;

    public RandomNumberPublisher() {
        this(Long.MAX_VALUE);
    }

    public RandomNumberPublisher(long maxElements) {
        this.maxElements = maxElements;
    }

    @Override
    public void subscribe(Subscriber<? super Long> downstream) {
        downstream.onSubscribe(new Generator(downstream));
    }

    private final class Generator implements Subscription {

        private final Subscriber<? super Long> downstream;

        private final AtomicLong totalElements = new AtomicLong();
        private final AtomicLong requested = new AtomicLong();
        private volatile boolean done = false;
        private final Random random = new Random(666L);

        private Generator(Subscriber<? super Long> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void request(long n) {
            if (n <= 0L) {
                downstream.onError(new IllegalArgumentException("Requested a negative number of elements: " + n));
                cancel();
                return;
            }
            while (true) {
                long initial = requested.get();
                long newValue = initial + n;
                if (newValue < 0L) {
                    newValue = Long.MAX_VALUE;
                }
                if (requested.compareAndSet(initial, newValue)) {
                    if (initial == 0L) {
                        drain();
                    }
                    return;
                }
            }
        }

        private void drain() {
            long emitted = 0L;
            long total = totalElements.get();
            long remaining = requested.get();
            while (true) {
                if (done) {
                    return;
                }
                while (emitted != remaining) {
                    downstream.onNext(random.nextLong());
                    emitted++;
                    total++;
                    if (total == maxElements) {
                        downstream.onComplete();
                        cancel();
                    }
                    if (done) {
                        return;
                    }
                }
                remaining = requested.addAndGet(-emitted);
                total = totalElements.addAndGet(emitted);
                if (remaining == 0L) {
                    return;
                }
                emitted = 0L;
            }
        }

        @Override
        public void cancel() {
            done = true;
        }
    }
}
