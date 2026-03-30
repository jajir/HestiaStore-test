package org.hestiastore.index.benchmark.multithread;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.hestiastore.index.benchmark.plainload.AbstractBenchmarkSupport;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Shared base for multithread write benchmarks.
 *
 * The benchmark mirrors the multithread read suite by exposing the same write
 * operation in both SampleTime and Throughput modes so JMH JSON captures
 * percentile latency data together with aggregate ops/s.
 */
public abstract class AbstractMultithreadWriteBenchmark
        extends AbstractBenchmarkSupport {

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public final String write() throws Exception {
        return performWrite();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public final String writeThroughput() throws Exception {
        return performWrite();
    }

    @Setup(Level.Trial)
    public final void setupBenchmark() throws Exception {
        final File directory = prepareDirectory();
        createStorage(directory);
        logger.info("Prepared {} for multithread writes",
                getClass().getSimpleName());
    }

    @TearDown(Level.Trial)
    public final void tearDownBenchmark() throws Exception {
        closeStorage();
    }

    protected abstract void createStorage(File dir) throws Exception;

    protected abstract void writeValue(String key, String value)
            throws Exception;

    protected abstract void closeStorage() throws Exception;

    private String performWrite() throws Exception {
        final String key = HASH_DATA_PROVIDER
                .makeHash(ThreadLocalRandom.current().nextLong());
        writeValue(key, VALUE);
        return key;
    }
}
