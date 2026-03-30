package org.hestiastore.index.benchmark.multithread;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.hestiastore.index.benchmark.plainload.AbstractWriteTest;
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
 * Shared base for multithread read benchmarks.
 *
 * The suite intentionally exposes the same read operation through both
 * SampleTime and Throughput modes so the console and JMH JSON capture latency
 * distributions and aggregate ops/s in the same run.
 */
public abstract class AbstractMultithreadReadBenchmark
        extends AbstractWriteTest {

    public static final String PROPERTY_PRELOAD_ENTRY_COUNT = "benchmarkPreloadEntryCount";
    public static final String PROPERTY_MISS_PROBABILITY = "benchmarkMissProbability";
    private static final long DEFAULT_PRELOAD_ENTRY_COUNT = 10_000_000L;
    private static final double DEFAULT_MISS_PROBABILITY = 0.2d;

    private long preloadEntryCount;
    private double missProbability;

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public final String read() throws Exception {
        return performRead();
    }

    private String performRead() throws Exception {
        final String key = pickReadKey();
        final String value = readValue(key);
        return value != null ? value : key;
    }

    @Setup(Level.Trial)
    public final void setupBenchmark() throws Exception {
        preloadEntryCount = Long.getLong(PROPERTY_PRELOAD_ENTRY_COUNT,
                DEFAULT_PRELOAD_ENTRY_COUNT);
        missProbability = Double
                .parseDouble(System.getProperty(PROPERTY_MISS_PROBABILITY,
                        Double.toString(DEFAULT_MISS_PROBABILITY)));
        validateConfiguration();

        final File directory = prepareDirectory();
        createStorage(directory);
        preloadDataset();
        afterPreload();
        logger.info(
                "Prepared {} with preloadEntryCount={} and missProbability={}",
                getClass().getSimpleName(), preloadEntryCount, missProbability);
    }

    @TearDown(Level.Trial)
    public final void tearDownBenchmark() throws Exception {
        closeStorage();
    }

    protected abstract void createStorage(File dir) throws Exception;

    protected abstract void writeValue(String key, String value)
            throws Exception;

    protected abstract String readValue(String key) throws Exception;

    protected void afterPreload() throws Exception {
        // default no-op
    }

    protected abstract void closeStorage() throws Exception;

    protected final long getPreloadEntryCount() {
        return preloadEntryCount;
    }

    protected final String keyForIndex(final long index) {
        return HASH_DATA_PROVIDER.makeHash(index);
    }

    private void validateConfiguration() {
        if (preloadEntryCount <= 0) {
            throw new IllegalStateException("Property '"
                    + PROPERTY_PRELOAD_ENTRY_COUNT + "' must be > 0");
        }
        if (missProbability < 0d || missProbability > 1d) {
            throw new IllegalStateException("Property '"
                    + PROPERTY_MISS_PROBABILITY + "' must be between 0 and 1");
        }
    }

    private void preloadDataset() throws Exception {
        for (long i = 0; i < preloadEntryCount; i++) {
            writeValue(keyForIndex(i), VALUE);
        }
    }

    private String pickReadKey() {
        return ThreadLocalRandom.current().nextDouble() < missProbability
                ? randomMissingKey()
                : randomExistingKey();
    }

    private String randomExistingKey() {
        final long index = ThreadLocalRandom.current()
                .nextLong(preloadEntryCount);
        return keyForIndex(index);
    }

    private String randomMissingKey() {
        final long index = preloadEntryCount
                + ThreadLocalRandom.current().nextLong(preloadEntryCount);
        return keyForIndex(index);
    }
}
