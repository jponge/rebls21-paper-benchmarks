package org.ponge.helpers;

import io.smallrye.mutiny.helpers.Subscriptions;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicLong;

public class TextFileLinePublisher implements Publisher<String> {

    private final File file;
    private final long maxElements;

    public TextFileLinePublisher(File file, long maxElements) {
        this.file = file;
        this.maxElements = maxElements;
    }

    public TextFileLinePublisher(File file) {
        this(file, Long.MAX_VALUE);
    }

    @Override
    public void subscribe(Subscriber<? super String> downstream) {
        try {
            BufferedReader reader = Files.newBufferedReader(file.toPath());
            Generator generator = new Generator(downstream, reader);
            downstream.onSubscribe(generator);
        } catch (IOException err) {
            downstream.onSubscribe(Subscriptions.empty());
            downstream.onError(err);
        }
    }

    private class Generator implements Subscription {

        private final Subscriber<? super String> downstream;
        private final BufferedReader reader;

        private final AtomicLong totalElements = new AtomicLong();
        private final AtomicLong requested = new AtomicLong();
        private volatile boolean done = false;

        private Generator(Subscriber<? super String> downstream, BufferedReader reader) {
            this.downstream = downstream;
            this.reader = reader;
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
                    try {
                        String line = reader.readLine();
                        if (line != null) {
                            downstream.onNext(line);
                        } else {
                            cancel();
                            downstream.onComplete();
                            return;
                        }
                    } catch (IOException err) {
                        cancel();
                        downstream.onError(err);
                        return;
                    }
                    emitted++;
                    total++;
                    if (total == maxElements) {
                        cancel();
                        downstream.onComplete();
                        return;
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
