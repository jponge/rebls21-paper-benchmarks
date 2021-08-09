package org.ponge.helpers;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

public class RandomNumberPublisherTckTest extends PublisherVerification<Long> {

    public RandomNumberPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return new RandomNumberPublisher(elements);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }
}
