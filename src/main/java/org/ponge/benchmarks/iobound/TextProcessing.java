package org.ponge.benchmarks.iobound;

import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.ponge.helpers.TextFileLinePublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class TextProcessing {

    File source = new File("data/les-miserables.txt");
    File sink = new File("data/out.txt");

    FileOutputStream out;
    private final byte[] newline = "\n".getBytes(StandardCharsets.UTF_8);

    private Multi<String> multi;
    private Flux<String> flux;
    private Flowable<String> flowable;

    @Setup
    public void prepare() throws FileNotFoundException {
        sink.delete();
        out = new FileOutputStream(sink, true);

        multi = Multi.createFrom().safePublisher(new TextFileLinePublisher(source))
                .select().where(line -> !line.isBlank())
                .onItem().transform(String::length)
                .onItem().transformToUniAndConcatenate(count -> Uni.createFrom().completionStage(appendToSink(count)))
                .onItem().transform(count -> "=> " + count);

        flux = Flux.from(new TextFileLinePublisher(source))
                .filter(line -> !line.isBlank())
                .map(String::length)
                .concatMap(count -> Mono.fromCompletionStage(appendToSink(count)), 1)
                .map(count -> "=> " + count);

        flowable = Flowable.fromPublisher(new TextFileLinePublisher(source))
                .filter(line -> !line.isBlank())
                .map(String::length)
                .concatMap(count -> Flowable.fromCompletionStage(appendToSink(count)), 1)
                .map(count -> "=> " + count);
    }

    @TearDown
    public void finish() throws IOException {
        out.close();
    }

    // Inefficient async work (on purpose)
    private CompletionStage<Long> appendToSink(long count) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (TextProcessing.this) {
                try {
                    out.write(String.valueOf(count).getBytes(StandardCharsets.UTF_8));
                    out.write(newline);
                    out.flush();
                    return count;
                } catch (IOException err) {
                    err.printStackTrace();
                    return -1L;
                }
            }
        });
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void baseline(Blackhole blackhole) throws IOException {
        AtomicLong remaining = new AtomicLong(0L);
        try (BufferedReader reader = Files.newBufferedReader(source.toPath())) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    while (remaining.get() != 0L) {
                        // Busy wait
                    }
                    return;
                }
                if (line.isBlank()) {
                    continue;
                }
                remaining.incrementAndGet();
                appendToSink(line.length()).thenAccept(count -> {
                    blackhole.consume("=> " + count);
                    remaining.decrementAndGet();
                });
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void mutiny(Blackhole blackhole) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        multi.subscribe().with(blackhole::consume, throwable -> {
            throwable.printStackTrace();
            latch.countDown();
        }, latch::countDown);
        latch.await();
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void reactor(Blackhole blackhole) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        flux.subscribe(blackhole::consume, throwable -> {
            throwable.printStackTrace();
            latch.countDown();
        }, latch::countDown);
        latch.await();
    }

    // -------------------------------------------------------------------------------------------------------------- //

    @Benchmark
    public void rxjava(Blackhole blackhole) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        flowable.subscribe(blackhole::consume, throwable -> {
            throwable.printStackTrace();
            latch.countDown();
        }, latch::countDown);
        latch.await();
    }

    // -------------------------------------------------------------------------------------------------------------- //

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(TextProcessing.class.getSimpleName() + ".baseline")
                .warmupIterations(20)
                .measurementIterations(50)
                .forks(1)
                .build();
        new Runner(options).run();
    }
}
