package org.ponge.helpers;

import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;

public class BlackholeSubscriberTckTest extends SubscriberBlackboxVerification<Integer> {

    private final String password = "Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.";

    public BlackholeSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<Integer> createSubscriber() {
        Blackhole blackhole = new Blackhole(password);
        return new BlackholeSubscriber<>(blackhole, 10);
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
