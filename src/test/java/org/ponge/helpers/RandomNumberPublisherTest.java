package org.ponge.helpers;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class RandomNumberPublisherTest {

    @Test
    public void check() {
        AssertSubscriber<Long> subscriber = Multi.createFrom().publisher(new RandomNumberPublisher())
                .subscribe().withSubscriber(AssertSubscriber.create());
        subscriber.request(1);
        assertEquals(subscriber.getItems().size(), 1);
        subscriber.request(4);
        assertEquals(subscriber.getItems().size(), 5);
        subscriber.request(5);
        assertEquals(subscriber.getItems().size(), 10);
    }
}