package org.ponge.helpers;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TextFileLinePublisherTest {

    @Test
    public void check() {
        File file = new File("data/les-miserables.txt");

        AssertSubscriber<String> subscriber = Multi.createFrom().publisher(new TextFileLinePublisher(file))
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(10);
        assertEquals(subscriber.getItems().size(), 10);
        assertTrue(subscriber.getItems().get(5).contains("Project Gutenberg License"));
    }

}