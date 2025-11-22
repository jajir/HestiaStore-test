package org.hestiastore.microbenchmarks.collections;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;

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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Random-read benchmark using {@link RandomAccessFile#seek(long)}.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(0)
@Threads(1)
@State(Scope.Benchmark)
public class RandomReadsRandomFileAccessBenchmark
        extends AbstractRandomReadBenchmark {

    private static final int READ_LENGTH = 4 * 1024;

    private File segmentFile;
    private byte[] buffer;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        ensureDataFileReady();
        this.segmentFile = getSegmentFile();
        this.buffer = new byte[READ_LENGTH];
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        buffer = null;
        segmentFile = null;
    }

    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 10, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public byte readRandomPage() throws Exception {
        final long position = nextRandomPosition(READ_LENGTH);
        try (RandomAccessFile raf = new RandomAccessFile(segmentFile, "r")) {
            raf.seek(position);
            raf.readFully(buffer);
        }
        return buffer[0];
    }
}
