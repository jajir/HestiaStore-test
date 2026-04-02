package org.hestiastore.index.benchmark.storage.singlethread.sequential;

import java.util.concurrent.TimeUnit;

import org.hestiastore.index.benchmark.storage.common.AbstractSingleThreadReadBenchmarkSupport;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Base class for sequential read benchmarks. Sequential variants reuse the
 * preloaded dataset but iterate over keys in a deterministic order instead of
 * random access.
 */
abstract class AbstractSequentialReadBenchmark extends AbstractSingleThreadReadBenchmarkSupport {

    private long sequentialCursor = 0L;

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String sequentialRead() throws Exception {
        return runSingleThreadOperation();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String sequentialReadThroughput() throws Exception {
        return runSingleThreadOperation();
    }

    protected String nextSequentialKey() {
        final long idx = sequentialCursor;
        sequentialCursor = (sequentialCursor + 1) % PRELOAD_ENTRY_COUNT;
        return keyForIndex(idx);
    }

    protected void resetSequentialCursor() {
        sequentialCursor = 0L;
    }
}
