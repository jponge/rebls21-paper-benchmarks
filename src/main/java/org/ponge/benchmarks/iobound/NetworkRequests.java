package org.ponge.benchmarks.iobound;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class NetworkRequests {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final HttpRequest httpRequest = HttpRequest
            .newBuilder(
                    URI.create(System.getProperty("network-requests-benchmark.url", "http://127.0.0.1:4000/les-miserables.txt")))
            .GET()
            .build();

    private Multi<Integer> multi;
    private Flux<Integer> flux;
    private Flowable<Integer> flowable;

    @SuppressWarnings("unchecked")
    private static List<HttpResponse<String>> apply(List<?> list) {
        return (List<HttpResponse<String>>) list;
    }

    private CompletableFuture<HttpResponse<String>> performHttpRequest() {
        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Setup
    public void prepare() {
        multi = Uni.join().all(
                        Uni.createFrom().completionStage(this::performHttpRequest),
                        Uni.createFrom().completionStage(this::performHttpRequest),
                        Uni.createFrom().completionStage(this::performHttpRequest),
                        Uni.createFrom().completionStage(this::performHttpRequest),
                        Uni.createFrom().completionStage(this::performHttpRequest),
                        Uni.createFrom().completionStage(this::performHttpRequest)).andFailFast()
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list))
                .onItem().transform(HttpResponse::body)
                .onItem().transform(String::length);

        flux = Mono.zip(
                        Mono.fromCompletionStage(this::performHttpRequest),
                        Mono.fromCompletionStage(this::performHttpRequest),
                        Mono.fromCompletionStage(this::performHttpRequest),
                        Mono.fromCompletionStage(this::performHttpRequest),
                        Mono.fromCompletionStage(this::performHttpRequest),
                        Mono.fromCompletionStage(this::performHttpRequest)
                )
                .flatMapMany(tuple -> Flux.fromIterable(tuple.toList()))
                .cast(HttpResponse.class)
                .map(HttpResponse::body)
                .cast(String.class)
                .map(String::length);

        // Flowable can be created from a CompletionStage, but not a supplier... which defeats the purpose of bridging CS!
        flowable = Flowable.zip(
                        Flowable.defer(() -> Flowable.fromCompletionStage(performHttpRequest())),
                        Flowable.defer(() -> Flowable.fromCompletionStage(performHttpRequest())),
                        Flowable.defer(() -> Flowable.fromCompletionStage(performHttpRequest())),
                        Flowable.defer(() -> Flowable.fromCompletionStage(performHttpRequest())),
                        Flowable.defer(() -> Flowable.fromCompletionStage(performHttpRequest())),
                        Flowable.defer(() -> Flowable.fromCompletionStage(performHttpRequest())),
                        (r1, r2, r3, r4, r5, r6) -> apply(Arrays.asList(r1, r2, r3, r4, r5, r6))
                )
                .flatMapIterable(Functions.identity())
                .map(HttpResponse::body)
                .map(String::length);
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void baseline(Blackhole blackhole) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(6);

        ArrayList<HttpResponse<String>> responses = new ArrayList<>(6);
        for (int i = 0; i < 6; i++) {
            performHttpRequest().thenAccept(e -> {
                responses.add(e);
                latch.countDown();
            });
        }
        latch.await();

        responses.forEach(response -> blackhole.consume(response.body().length()));
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void rxjava(Blackhole blackhole) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        flowable.subscribe(blackhole::consume,
                throwable -> {
                    throwable.printStackTrace();
                    latch.countDown();
                }, latch::countDown);
        latch.await();
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void mutiny(Blackhole blackhole) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        multi.subscribe().with(
                blackhole::consume,
                throwable -> {
                    throwable.printStackTrace();
                    latch.countDown();
                }, latch::countDown);
        latch.await();
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void reactor(Blackhole blackhole) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        flux.subscribe(blackhole::consume,
                throwable -> {
                    throwable.printStackTrace();
                    latch.countDown();
                }, latch::countDown);

        latch.await();
    }

    // -------------------------------------------------------------------------------------------------------------- //

    public static void main(String[] args) throws Throwable {
        Options options = new OptionsBuilder()
                .include(NetworkRequests.class.getSimpleName())
                .warmupIterations(20)
                .measurementIterations(50)
                .forks(1)
                .build();
        new Runner(options).run();
    }
}
