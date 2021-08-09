package org.ponge.benchmarks.cpubound;

import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.mutiny.Multi;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.ponge.helpers.BlackholeSubscriber;
import org.ponge.helpers.RandomNumberPublisher;
import reactor.core.publisher.Flux;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class StreamDirectTransformation {

    @Param({"100000"})
    private long requestCount;

    private Multi<String> multi;
    private Flux<String> flux;
    private Flowable<String> flowable;

    @Setup
    public void prepare() {
        multi = Multi.createFrom().safePublisher(new RandomNumberPublisher())
                .onItem().transform(Math::abs)
                .select().where(n -> n % 2 == 0)
                .onItem().transformToMultiAndConcatenate(n -> {
                    String s = Long.toHexString(n);
                    return Multi.createFrom().items(s, s + "!", s + "!!", s + "!!!");
                });

        flux = Flux.from(new RandomNumberPublisher())
                .map(Math::abs)
                .filter(n -> n % 2 == 0)
                .concatMap(n -> {
                    String s = Long.toHexString(n);
                    return Flux.just(s, s + "!", s + "!!", s + "!!!");
                });

        flowable = Flowable.fromPublisher(new RandomNumberPublisher())
                .map(Math::abs)
                .filter(n -> n % 2 == 0)
                .concatMap(n -> {
                    String s = Long.toHexString(n);
                    return Flowable.just(s, s + "!", s + "!!", s + "!!!");
                });
    }

    @Benchmark
    public void baseline_loop(Blackhole blackhole) {
        Random random = new Random(666L);
        for (long i = 0; i < requestCount; i++) {
            long n = Math.abs(random.nextLong());
            if (n % 2 == 0) {
                String str = Long.toHexString(n);
                blackhole.consume(str);
                blackhole.consume(str + "!");
                blackhole.consume(str + "!!");
                blackhole.consume(str + "!!!");
            }
        }
    }

    @Benchmark
    public void baseline_streams(Blackhole blackhole) {
        Random random = new Random(666L);
        LongStream.generate(random::nextLong)
                .limit(requestCount)
                .boxed()
                .map(Math::abs)
                .filter(n -> n % 2 == 0)
                .flatMap(n -> {
                    String s = Long.toHexString(n);
                    return Stream.of(s, s + "!", s + "!!", s + "!!!");
                })
                .forEach(blackhole::consume);
    }

    @Benchmark
    public void multi(Blackhole blackhole) {
        multi.subscribe().withSubscriber(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    @Benchmark
    public void flux(Blackhole blackhole) {
        flux.subscribe(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    @Benchmark
    public void flowable(Blackhole blackhole) {
        flowable.subscribe(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(StreamDirectTransformation.class.getSimpleName())
                .forks(1)
                .threads(Runtime.getRuntime().availableProcessors() * 4)
                .syncIterations(true)
                .build();
        new Runner(options).run();
    }
}
