package org.hestiastore.index.benchmark.storage.singlethread.write;

import java.util.concurrent.TimeUnit;

import org.hestiastore.index.benchmark.storage.common.AbstractSingleThreadStorageBenchmark;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Base class for single-thread write-focused benchmarks.
 */
public abstract class AbstractWriteSingleThreadBenchmark extends AbstractSingleThreadStorageBenchmark {

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String write() throws Exception {
        return runSingleThreadOperation();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String writeThroughput() throws Exception {
        return runSingleThreadOperation();
    }
}
