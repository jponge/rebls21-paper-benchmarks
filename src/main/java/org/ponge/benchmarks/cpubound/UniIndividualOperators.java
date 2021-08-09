package org.ponge.benchmarks.cpubound;

import io.reactivex.rxjava3.core.Single;
import io.smallrye.mutiny.Uni;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.ponge.helpers.BlackholeSingleOperationSubscriber;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class UniIndividualOperators {

    private Single<Long> single;
    private Uni<Long> uni;
    private Mono<Long> mono;

    @Setup
    public void prepare() {
        single = Single.fromSupplier(() -> ThreadLocalRandom.current().nextLong());
        uni = Uni.createFrom().item(() -> ThreadLocalRandom.current().nextLong());
        mono = Mono.fromSupplier(() -> ThreadLocalRandom.current().nextLong());
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void baseline_cf_map(Blackhole blackhole) {
        CompletableFuture.completedFuture(ThreadLocalRandom.current().nextLong())
                .thenApply(n -> n + 1L)
                .thenAccept(blackhole::consume);
    }

    @Benchmark
    public void uni_map(Blackhole blackhole) {
        uni.onItem().transform(n -> n + 1L)
                .subscribe().withSubscriber(new BlackholeSingleOperationSubscriber<>(blackhole));
    }

    @Benchmark
    public void single_map(Blackhole blackhole) {
        single.map(n -> n + 1L)
                .subscribe(new BlackholeSingleOperationSubscriber<>(blackhole));
    }

    @Benchmark
    public void mono_map(Blackhole blackhole) {
        mono.map(n -> n + 1L)
                .subscribe(new BlackholeSingleOperationSubscriber<>(blackhole));
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void baseline_cf_chain(Blackhole blackhole) {
        CompletableFuture.completedFuture(ThreadLocalRandom.current().nextLong())
                .thenCompose(n -> CompletableFuture.completedFuture(n + 1L))
                .thenAccept(blackhole::consume);
    }

    @Benchmark
    public void uni_chain(Blackhole blackhole) {
        uni.onItem().transformToUni(n -> Uni.createFrom().item(n + 1L))
                .subscribe().withSubscriber(new BlackholeSingleOperationSubscriber<>(blackhole));
    }

    @Benchmark
    public void single_chain(Blackhole blackhole) {
        single.flatMap(n -> Single.just(n + 1L))
                .subscribe(new BlackholeSingleOperationSubscriber<>(blackhole));
    }

    @Benchmark
    public void mono_chain(Blackhole blackhole) {
        mono.flatMap(n -> Mono.just(n + 1L))
                .subscribe(new BlackholeSingleOperationSubscriber<>(blackhole));
    }

    // -------------------------------------------------------------------------------------------------------------- //

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(UniIndividualOperators.class.getSimpleName())
                .forks(1)
                .threads(Runtime.getRuntime().availableProcessors() * 4)
                .syncIterations(true)
                .build();
        new Runner(options).run();
    }
}
