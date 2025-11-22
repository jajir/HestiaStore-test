package org.hestiastore.microbenchmarks.collections;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
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
 * Baseline random-read benchmark using {@link FileInputStream}. Each operation
 * seeks to a random offset and reads a fixed-size buffer.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(0)
@Threads(1)
@State(Scope.Benchmark)
public class RandomReadsChannelsBenchmark extends AbstractRandomReadBenchmark {

    private static final int READ_LENGTH = 4 * 1024;

    private File segmentFile;
    private ByteBuffer buffer;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        ensureDataFileReady();
        this.segmentFile = getSegmentFile();
        this.buffer = ByteBuffer.allocateDirect(READ_LENGTH);
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
        try (FileChannel ch = FileChannel.open(segmentFile.toPath(),
                StandardOpenOption.READ)) {
            final long position = nextRandomPosition(READ_LENGTH);
            buffer.clear();
            ch.position(position);
            ch.read(buffer);
            return buffer.get(0);
        }
    }

}
