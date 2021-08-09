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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SingleOperationDirectTransformation {

    private Single<String> single;
    private Uni<String> uni;
    private Mono<String> mono;

    @Setup
    public void prepare() {
        single = Single.fromSupplier(() -> ThreadLocalRandom.current().nextLong())
                .map(Math::abs)
                .map(Long::toHexString)
                .flatMap(s -> Single.just("[" + s + "]"));

        uni = Uni.createFrom().item(() -> ThreadLocalRandom.current().nextLong())
                .onItem().transform(Math::abs)
                .onItem().transform(Long::toHexString)
                .onItem().transformToUni(s -> Uni.createFrom().item("[" + s + "]"));

        mono = Mono.fromSupplier(() -> ThreadLocalRandom.current().nextLong())
                .map(Math::abs)
                .map(Long::toHexString)
                .flatMap(s -> Mono.just("[" + s + "]"));
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public String baseline() {
        long n = Math.abs(ThreadLocalRandom.current().nextLong());
        return "[" + Optional.of(Long.toHexString(n)).get() + "]";
    }

    @Benchmark
    public void baseline_completable_future(Blackhole blackhole) {
        CompletableFuture.completedFuture(ThreadLocalRandom.current().nextLong())
                .thenApply(Math::abs)
                .thenApply(Long::toHexString)
                .thenApply(Optional::of)
                .thenApply(s -> "[" + s.get() + "]")
                .thenAccept(blackhole::consume);
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void uni(Blackhole blackhole) {
        uni.subscribe().withSubscriber(new BlackholeSingleOperationSubscriber<>(blackhole));
    }

    @Benchmark
    public void mono(Blackhole blackhole) {
        mono.subscribe(new BlackholeSingleOperationSubscriber<>(blackhole));
    }

    @Benchmark
    public void single(Blackhole blackhole) {
        single.subscribe(new BlackholeSingleOperationSubscriber<>(blackhole));
    }

    // -------------------------------------------------------------------------------------------------------------- //

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(SingleOperationDirectTransformation.class.getSimpleName())
                .forks(1)
                .threads(Runtime.getRuntime().availableProcessors() * 4)
                .syncIterations(true)
                .build();
        new Runner(options).run();
    }
}
