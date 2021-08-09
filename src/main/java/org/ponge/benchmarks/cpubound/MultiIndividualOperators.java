package org.ponge.benchmarks.cpubound;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.ponge.helpers.BlackholeSubscriber;
import org.ponge.helpers.RandomNumberPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class MultiIndividualOperators {

    @Param({"100000"})
    private long requestCount;

    private Multi<Long> multi;
    private Flux<Long> flux;
    private Flowable<Long> flowable;

    @Setup
    public void prepare() {
        multi = Multi.createFrom().safePublisher(new RandomNumberPublisher());
        flux = Flux.from(new RandomNumberPublisher());
        flowable = Flowable.fromPublisher(new RandomNumberPublisher());
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void multi_map(Blackhole blackhole) {
        multi.onItem().transform(Math::abs)
                .subscribe().withSubscriber(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    @Benchmark
    public void flux_map(Blackhole blackhole) {
        flux.map(Math::abs)
                .subscribe(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    @Benchmark
    public void flowable_map(Blackhole blackhole) {
        flowable.map(Math::abs)
                .subscribe(new BlackholeSubscriber<>(blackhole, requestCount));
    }


    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void multi_filter(Blackhole blackhole) {
        multi.select().where(n -> n % 2 == 0)
                .subscribe().withSubscriber(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    @Benchmark
    public void flux_filter(Blackhole blackhole) {
        flux.filter(n -> n % 2 == 0)
                .subscribe(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    @Benchmark
    public void flowable_filter(Blackhole blackhole) {
        flowable.filter(n -> n % 2 == 0)
                .subscribe(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void multi_mapToMany(Blackhole blackhole) {
        multi.onItem().transformToMulti(n -> Multi.createFrom().items(n, n + 1L, n + 2L))
                .merge(32)
                .subscribe().withSubscriber(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    @Benchmark
    public void flowable_mapToMany(Blackhole blackhole) {
        flowable.flatMap(n -> Flowable.just(n, n + 1L, n + 2L), false, 32, 1) // Can't disable pre-fetching
                .subscribe(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    @Benchmark
    public void flux_mapToMany(Blackhole blackhole) {
        flux.flatMap(n -> Flux.just(n, n + 1L, n + 2L), 32, 1) // Can't disable pre-fetching
                .subscribe(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void multi_mapToOne(Blackhole blackhole) {
        multi.onItem().transformToUniAndMerge(n -> Uni.createFrom().item(n + 1L))
                .subscribe().withSubscriber(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    //@Benchmark
    // OutOfMemoryException bug with Flowable
    public void flowable_mapToOne(Blackhole blackhole) {
        flowable.flatMapSingle(n -> Single.just(n + 1L))
                .subscribe(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    @Benchmark
    public void flux_mapToOne(Blackhole blackhole) {
        flux.flatMap(n -> Mono.just(n + 1L))
                .subscribe(new BlackholeSubscriber<>(blackhole, requestCount));
    }

    // -------------------------------------------------------------------------------------------------------------- //

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(MultiIndividualOperators.class.getSimpleName() + ".*mapToMany.*")
                .warmupIterations(2)
                .measurementIterations(2)
                .forks(1)
                .threads(Runtime.getRuntime().availableProcessors() * 4)
                .syncIterations(true)
                .build();
        new Runner(options).run();
    }
}
