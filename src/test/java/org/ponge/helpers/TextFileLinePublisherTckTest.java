package org.ponge.helpers;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

import java.io.File;

public class TextFileLinePublisherTckTest extends PublisherVerification<String> {

    public TextFileLinePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<String> createPublisher(long elements) {
        File file = new File("data/les-miserables.txt");
        return new TextFileLinePublisher(file, elements);
    }

    @Override
    public Publisher<String> createFailedPublisher() {
        return null;
    }
}
