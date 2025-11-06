package org.hestiastore.microbenchmarks.collections;

import java.util.concurrent.TimeUnit;

import org.hestiastore.index.Entry;
import org.hestiastore.index.chunkentryfile.SingleChunkEntryWriter;
import org.hestiastore.index.chunkentryfile.SingleChunkEntryWriterImpl;
import org.hestiastore.index.datatype.TypeDescriptor;
import org.hestiastore.index.datatype.TypeDescriptorString;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark for measuring performance of SingleChunkEntryWriterImpl#put.
 *
 * Notes:
 * - The writer requires strictly ascending keys due to DiffKeyWriter.
 * - We generate ascending, zero-padded numeric String keys via DataProvider.wrap(counter).
 * - To cap memory usage, the writer is periodically closed and reopened.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(0)
@State(Scope.Benchmark)
public class SingleChunkEntryWriterBenchmark extends AbstractBenchmark {

    private static final TypeDescriptor<String> TYPE_STRING = new TypeDescriptorString();

    private SingleChunkEntryWriter<String, String> writer;
    private long seq;
    private long opsSinceReopen;

    // Reopen interval in entries to bound in-memory growth
    private static final long REOPEN_INTERVAL = 500_000;

    @Setup(Level.Trial)
    public void setup() {
        this.writer = newWriter();
        this.seq = 0L;
        this.opsSinceReopen = 0L;
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        tryCloseWriter();
        writer = null;
    }

    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 10, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String put() {
        // Periodically rotate the writer to avoid unbounded memory growth
        if (opsSinceReopen >= REOPEN_INTERVAL) {
            tryCloseWriter();
            writer = newWriter();
            opsSinceReopen = 0L;
            seq = 0L;
        }

        final String key = dataProvider.wrap(++seq);
        writer.put(Entry.of(key, VALUE));
        opsSinceReopen++;
        return key;
    }

    private void tryCloseWriter() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception ignore) {
            // Best-effort close in benchmark teardown/rotation
        }
    }

    private SingleChunkEntryWriter<String, String> newWriter() {
        return new SingleChunkEntryWriterImpl<>(TYPE_STRING, TYPE_STRING);
    }
}
